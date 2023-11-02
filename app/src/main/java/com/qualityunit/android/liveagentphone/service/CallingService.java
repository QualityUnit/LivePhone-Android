package com.qualityunit.android.liveagentphone.service;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.ui.call.CallingActivity;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.voice.VoiceAccount;
import com.qualityunit.android.voice.VoiceCall;
import com.qualityunit.android.voice.VoiceCore;

import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static com.qualityunit.android.liveagentphone.Const.Call.PHONE_ACCOUNT_HANDLE_ID;
import static com.qualityunit.android.liveagentphone.Const.ChannelId.INCOMING_CHANNEL_ID;
import static com.qualityunit.android.liveagentphone.Const.ChannelId.SERVICE_CHANNEL_ID;
import static com.qualityunit.android.liveagentphone.Const.NotificationId.INCOMING_NOTIFICATION_ID;
import static com.qualityunit.android.liveagentphone.Const.NotificationId.ONGOING_NOTIFICATION_ID;

/**
 * Created by rasto on 17.10.16.
 */
public class CallingService extends ConnectionService implements VoiceConnectionCallbacks {

    private static final String TAG = CallingService.class.getSimpleName();
    private static final long WAITING_TO_CALL_MILLIS = 5000;
    public static final String INTENT_FILTER_CALL_STATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_CALL_STATE";
    public static final String INTENT_FILTER_CALL_UPDATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_CALL_UPDATE";
    public static final String INTENT_FILTER_EXTENSION_STATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_EXTENSION_STATE";
    public static final String INTENT_FILTER_EXTENSION_UPDATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_EXTENSION_UPDATE";

    private static final int pendingIntentFlags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

    public static final class COMMANDS {
        public static final String MAKE_CALL = "MAKE_CALL";
        public static final String INCOMING_CALL = "INCOMING_CALL";
        public static final String ANSWER_CALL = "ANSWER_CALL";
        public static final String HANGUP_CALL = "HANGUP_CALL";
        public static final String TOGGLE_HOLD = "TOGGLE_HOLD";
        public static final String ADJUST_INCALL_VOLUME = "ADJUST_INCALL_VOLUME";
        public static final String SEND_DTMF = "SEND_DTMF";
        public static final String SILENCE_RINGING = "SILENCE_RINGING";
        public static final String TOGGLE_MUTE = "TOGGLE_MUTE";
        public static final String TOGGLE_SPEAKER = "TOGGLE_SPEAKER";
        public static final String GET_CALL_UPDATES = "GET_CALL_UPDATES";
        public static final String GET_CALL_STATE = "GET_CALL_STATE";
        public static final String TRANSFER = "TRANSFER";
        public static final class TRANSFER_STEP {
            public static final String HOLD_ACTIVE = "STEP_HOLD_ACTIVE";
            public static final String HOLD_EXTENSION = "HOLD_EXTENSION";
            public static final String CALL_EXTENSION = "STEP_CALL_EXTENSION";
            public static final String HANGUP_EXTENSION = "STEP_HANGUP_EXTENSION";
            public static final String COMPLETE_TRANSFER = "STEP_COMPLETE_TRANSFER";
        }
    }

    public static final class CALL_STATE {
        public static final String INITIALIZING_SIP_LIBRARY = "INITIALIZING_SIP_LIBRARY";
        public static final String REGISTERING_SIP_USER = "REGISTERING_SIP_USER";
        public static final String DIALING = "DIALING";
        public static final String WAITING_TO_CALL = "WAITING_TO_CALL";
        public static final String RINGING = "RINGING";
        public static final String ACTIVE = "ACTIVE";
        public static final String HOLD = "HOLD";
        public static final String DISCONNECTED = "DISCONNECTED";
        public static final String ERROR = "ERROR";
    }
    public static final class EXTENSION_STATE {
        public static final String DIALING = "DIALING";
        public static final String ACTIVE = "ACTIVE";
        public static final String HOLD = "HOLD";
        public static final String DISCONNECTED = "DISCONNECTED";
        public static final String ERROR = "ERROR";
    }
    public static final class CALL_UPDATE {
        public static final String UPDATE_MUTE = "TOGGLE_MUTE";
        public static final String UPDATE_SPEAKER = "TOGGLE_SPEAKER";
        public static final String UPDATE_HOLD = "UPDATE_HOLD";
        public static final String UPDATE_DURATION = "UPDATE_DURATION";
    }
    private LocalBroadcastManager localBroadcastManager;
    private PhoneAccountHandle phoneAccountHandle;
    private HandlerThread mainThread;
    private Handler mainHandler;
    private HandlerThread workerThread;
    private Handler workerHandler;
    private volatile boolean nextCallAhead;
    private static PowerManager.WakeLock wakeLock;
    private VoiceConnection activeVoiceConnection;
    private Ringer ringer;

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        wakeLock.acquire();
        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        phoneAccountHandle = new PhoneAccountHandle(new ComponentName(getApplicationContext(), CallingService.class), PHONE_ACCOUNT_HANDLE_ID);
        ringer = new Ringer(getApplicationContext());
        startMainThread();
        startWorkerThread();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (intent.getStringExtra("command")) {
                        case COMMANDS.MAKE_CALL:
                            if (activeVoiceConnection != null) throw new CallingException(getString(R.string.only_one_call));
                            makeCall(intent);
                            break;
                        case COMMANDS.INCOMING_CALL:
                            if (activeVoiceConnection != null) {
                                nextCallAhead = true;
                                return;
                            }
                            incomingCall();
                            break;
                        case COMMANDS.ANSWER_CALL:
                            dismissIncomingCallNotification();
                            getActiveConnection().answerIncomingCall();
                            break;
                        case COMMANDS.SEND_DTMF:
                            getActiveConnection().sendDtfm(intent.getStringExtra("character"));
                            break;
                        case COMMANDS.HANGUP_CALL:
                            dismissIncomingCallNotification();
                            getActiveConnection().hangupAndDestroy(DisconnectCause.LOCAL);
                            break;
                        case COMMANDS.ADJUST_INCALL_VOLUME:
                            getActiveConnection().adjustIncallVolume(intent.getBooleanExtra("increase", true));
                            break;
                        case COMMANDS.SILENCE_RINGING:
                            stopRingtone();
                            break;
                        case COMMANDS.TOGGLE_MUTE:
                            getActiveConnection().toggleMute();
                            break;
                        case COMMANDS.TOGGLE_SPEAKER:
                            getActiveConnection().toggleSpeaker();
                            break;
                        case COMMANDS.TOGGLE_HOLD:
                            getActiveConnection().toggleHold();
                            break;
                        case COMMANDS.GET_CALL_UPDATES:
                            getActiveConnection().broadcastCallUpdates();
                            break;
                        case COMMANDS.GET_CALL_STATE:
                            getActiveConnection().broadcastCallState();
                            break;
                        case COMMANDS.TRANSFER:
                            switch (intent.getStringExtra("step")) {
                                case COMMANDS.TRANSFER_STEP.HOLD_ACTIVE:
                                    getActiveConnection().setHold(true);
                                    break;
                                case COMMANDS.TRANSFER_STEP.CALL_EXTENSION:
                                    getActiveConnection().callExtension(intent.getStringExtra("extensionNumber"), intent.getStringExtra("extensionName"));
                                    break;
                                case COMMANDS.TRANSFER_STEP.HANGUP_EXTENSION:
                                    getActiveConnection().hangupExtension();
                                    break;
                                case COMMANDS.TRANSFER_STEP.COMPLETE_TRANSFER:
                                    getActiveConnection().completeTransfer();
                                    break;
                                case COMMANDS.TRANSFER_STEP.HOLD_EXTENSION:
                                    getActiveConnection().toggleExtensionHold();
                                    break;
                                default:
                                    throw new CallingException("Unknown transfer step");
                            }
                            break;
                    }
                } catch (Exception e) {
                    onErrorState(e.getMessage(), e);
                }
            }
        });
        return START_NOT_STICKY;
    }

    private void incomingCall() {
        Bundle incomingExtras = new Bundle();
        incomingExtras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        registerPhoneAccount(telecomManager);
        telecomManager.addNewIncomingCall(phoneAccountHandle, incomingExtras);
    }

    private void makeCall(Intent intent) throws CallingException {
        Uri outgoingUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, intent.getStringExtra("remoteNumber"), null);
        Bundle outgoingExtras = new Bundle();
        outgoingExtras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
        outgoingExtras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new Bundle(intent.getExtras()));
        try {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            registerPhoneAccount(telecomManager);
            telecomManager.placeCall(outgoingUri, outgoingExtras);
        } catch (SecurityException e) {
            throw new CallingException(e.getMessage());
        }
    }

    private void dismissIncomingCallNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(INCOMING_NOTIFICATION_ID);
        }
    }

    private VoiceConnection getActiveConnection() throws CallingException {
        if(activeVoiceConnection == null) {
            throw new CallingException("No active voice connection");
        }
        return activeVoiceConnection;
    }

    private void registerPhoneAccount(TelecomManager telecomManager) {
        PhoneAccount.Builder builder = PhoneAccount.builder(phoneAccountHandle, PHONE_ACCOUNT_HANDLE_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED);
        }
        PhoneAccount phoneAccount = builder.build();
        telecomManager.registerPhoneAccount(phoneAccount);
    }

    @Override
    public void onDestroy() {
        stopRingtone();
        stopWorkerThread();
        stopMainThread();
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        log("Service destroyed");
        super.onDestroy();
//        android.os.Process.killProcess(android.os.Process.myPid()); // this also kills calling activity (which is running on the same PID)
    }

    /** Telephony callbacks **/

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        activeVoiceConnection = new VoiceConnection(this);
        activeVoiceConnection.makeVoiceCall(request);
        return activeVoiceConnection;
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Toast.makeText(this, "Failed to create LivePhone outgoing call", Toast.LENGTH_LONG).show();
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request);
    }

    @SuppressLint("MissingPermission")
    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        activeVoiceConnection = new VoiceConnection(this);
        activeVoiceConnection.incomingVoiceCall();
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        activeVoiceConnection.setInCallOfAnotherApp(telecomManager.isInCall());
        return activeVoiceConnection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Toast.makeText(this, "Failed to create LivePhone incoming call", Toast.LENGTH_LONG).show();
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request);
    }

    final class VoiceConnection extends Connection implements VoiceCall.Callbacks, VoiceAccount.Callbacks {

        private final AudioManager audioManager;
        private final String sipHost;
        private final String sipUser;
        private final String sipPassword;
        private PowerManager.WakeLock proximityWakeLock;
        private VoiceCall voiceCall;
        private VoiceCall extensionCall;
        private VoiceCore voiceCore;
        private VoiceAccount voiceAccount;
        private boolean isOutgoing;
        private boolean isMute;
        private boolean isSpeaker;
        private boolean isHold;
        private boolean isHangingUp;
        private boolean isInCallOfAnotherApp;
        private long duration = 0;
        private String lastCallState;
        private String remoteNumber;
        private String remoteName;
        private String prefix;
        private Timer finishTimer;
        private Timer durationTimer;
        private VoiceConnectionCallbacks callbacks;
        private String lastExtensionState = EXTENSION_STATE.DISCONNECTED;
        private String extensionNumber;
        private String extensionName;

        public VoiceConnection (VoiceConnectionCallbacks callbacks) {
            setInitializing();
            this.callbacks = callbacks;
            int capabilities = getConnectionCapabilities();
            capabilities |= CAPABILITY_MUTE;
            capabilities |= CAPABILITY_SUPPORT_HOLD;
            capabilities |= CAPABILITY_HOLD;
            setConnectionCapabilities(capabilities);
            setAudioModeIsVoip(true);
            // retrieve sip credentials
            AccountManager accountManager = AccountManager.get(getApplicationContext());
            sipHost = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_HOST);
            sipUser = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_USER);
            sipPassword = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_PASS);
            // unmute and turn off speaker from previous sessions
            audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(false);
        }

        public void makeVoiceCall(final ConnectionRequest request) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("--------------------------------------------------\n");
                    log("Make call requested...");
                    isOutgoing = true;
                    Bundle extras = request.getExtras();
                    prefix = extras.getString("prefix");
                    remoteNumber = extras.getString("remoteNumber");
                    remoteName = extras.getString("remoteName");
                    startActivity(createCallingActivityIntent(false, remoteNumber, remoteName));
                    acquireProximityWakelock();
                    initAndRegister();
                }
            });
        }

        public void incomingVoiceCall() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Incoming call requested...");
                    isOutgoing = false;
                    initAndRegister();
                }
            });
        }

        public void answerIncomingCall() {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Call answering...");
                        stopRingtone();
                        CallOpParam prm = VoiceCall.createDefaultParams();
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
                        voiceCall.answer(prm);
                        acquireProximityWakelock();
                        log("Call successfully answered");
                    } catch (Exception e) {
                        setErrorState("Error while answering call: " + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });
        }

        public void toggleHold() {
            setHold(!isHold);
        }

        public void setHold(final boolean hold) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (hold && !isHold) {
                        onHold();
                    } else if (!hold && isHold) {
                        onUnhold();
                    }
                }
            });
        }

        public void toggleSpeaker() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Toggling loud speaker " + (!isSpeaker ? "ON" : "OFF") + "...");
                    audioManager.setSpeakerphoneOn(!isSpeaker);
                    isSpeaker = !isSpeaker;
                    Bundle keyValue = new Bundle(1);
                    keyValue.putBoolean(CALL_UPDATE.UPDATE_SPEAKER, isSpeaker);
                    callbacks.onCallUpdate(keyValue);
                }
            });
        }

        public void toggleMute() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Toggling mute " + (!isMute ? "ON" : "OFF") + "...");
                    audioManager.setMicrophoneMute(!isMute);
                    isMute = !isMute;
                    Bundle keyValue = new Bundle(1);
                    keyValue.putBoolean(CALL_UPDATE.UPDATE_MUTE, isMute);
                    callbacks.onCallUpdate(keyValue);
                }
            });
        }

        public void adjustIncallVolume(final boolean increase) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Adjusting in-call volume " + (increase ? "UP" : "DOWN") + "...");
                    audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                }
            });
        }

        public void sendDtfm(final String character) {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Sending DTMF character '" + character + "'...");
                    try {
                        if (TextUtils.isEmpty(character)) throw new CallingException("Empty DTMF character");
                        if (!voiceCall.isActive()) throw new CallingException("Cannot send DTMF signal while call is not active");
                        voiceCall.dialDtmf(character);
                    } catch (Exception e) {
                        setErrorState("Error while sending DTMF signal: " + e.getMessage(), e);
                    }
                }
            });
        }

        private void continueMakeCall() {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Making a call...");
                        if (TextUtils.isEmpty(remoteNumber)) throw new CallingException("Argument 'remoteNumber' cannot be empty");
                        voiceCall = new VoiceCall(voiceAccount, -1, voiceCore, VoiceConnection.this);
                        String cleanedRemoteNumber = remoteNumber.replaceAll(" ", "").replaceAll("\\+", "00").replaceAll("\\/", ""); // cleaning number
                        String sipCalleeUri = "sip:" + prefix + cleanedRemoteNumber + "@" + sipHost;
                        voiceCall.makeCall(sipCalleeUri, VoiceCall.createDefaultParams());
                        setCallState(CALL_STATE.DIALING);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setInitialized();
                            }
                        });
                        log("Call started successfully");
                    } catch (final Exception e) {
                        setErrorState("Error while making SIP call:" + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });
        }

        private void initAndRegister() {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Initializing of SIP library...");
                        StringBuilder sb = new StringBuilder();
                        if (TextUtils.isEmpty(sipHost)) sb.append(sb.length() > 0 ? ", SIP host" : "SIP host");
                        if (TextUtils.isEmpty(sipUser)) sb.append(sb.length() > 0 ? ", SIP user" : "SIP user");
                        if (TextUtils.isEmpty(sipPassword)) sb.append(sb.length() > 0 ? ", SIP password" : "SIP password");
                        if (sb.length() > 0) {
                            throw new Exception("Empty parameters: " + sb.toString());
                        }
                        if (voiceCore == null) {
                            setCallState(CALL_STATE.INITIALIZING_SIP_LIBRARY);
                            voiceCore = new VoiceCore();
                            voiceCore.init(workerThread.getName());
                            log("SIP library initialized successfully");
                        }
                        if (voiceAccount == null || !voiceAccount.isRegistered()) {
                            setCallState(CALL_STATE.REGISTERING_SIP_USER);
                            voiceAccount = new VoiceAccount(VoiceConnection.this);
                            voiceAccount.register(sipHost, sipUser, sipPassword);
                            // now wait for registration callback and continue in flow
                            log("Account registration sent");
                        }
                    } catch (final Exception e) {
                        setErrorState("Error while initializing SIP lib: " + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });

        }

        public void hangupAndDestroy(final int cause) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    log("Hanging up and destroying...");
                    if (durationTimer != null) {
                        durationTimer.cancel();
                        durationTimer = null;
                    }
                    if (finishTimer != null) {
                        finishTimer.cancel();
                        finishTimer = null;
                    }
                    if (proximityWakeLock != null) {
                        proximityWakeLock.release();
                        proximityWakeLock = null;
                    }
                    if (!isHangingUp) {
                        isHangingUp = true;
                        workerHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (voiceCall != null) {
                                        if (voiceCall.isActive()) {
                                            CallOpParam prm = VoiceCall.createDefaultParams();
                                            prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
                                            voiceCall.hangup(prm);
                                        }
                                        voiceCall.delete();
                                        voiceCall = null;
                                    }
                                    if (voiceAccount != null) {
                                        voiceAccount.delete();
                                        voiceAccount = null;
                                    }
                                    if (voiceCore != null) {
                                        voiceCore.deinit();
                                        log("Library deinitialed");
                                        voiceCore = null;
                                    }
                                    log("Call successfully hanged up");
                                } catch (final Exception e) {
                                    setErrorState("Error while hanging up call: " + e.getMessage(), e);
                                } finally {
                                    mainHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            setDisconnected(new DisconnectCause(cause));
                                            destroy();
                                            callbacks.onConnectionDestroyed();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });
        }

        /** transfer methods **/

        public void callExtension(final String number, final String name) {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        log("Making an extension call...");
                        extensionNumber = number;
                        extensionName = name;
                        if (TextUtils.isEmpty(extensionNumber)) throw new CallingException("Argument 'extensionNumber' cannot be empty");
                        extensionCall = new VoiceCall(voiceAccount, -2, voiceCore, new VoiceCall.Callbacks() {
                            @Override
                            public void onAnswerCall() {
                                log("Answer extension call...");
                                setExtensionState(EXTENSION_STATE.ACTIVE);
                            }

                            @Override
                            public void onDisconnect() {
                                log("Disconnect extension call...");
                                setExtensionState(EXTENSION_STATE.DISCONNECTED);
                            }

                            @Override
                            public void onCallError(final Exception e) {
                                log("Error extension call: " + e.getMessage());
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        lastExtensionState = EXTENSION_STATE.ERROR;
                                        callbacks.onErrorState("Error while calling extension", e);
                                    }
                                });
                            }
                        });
                        String sipCalleeUri = "sip:" + extensionNumber.replaceAll(" ", "") + "@" + sipHost;
                        extensionCall.makeCall(sipCalleeUri, VoiceCall.createDefaultParams());
                        setExtensionState(EXTENSION_STATE.DIALING);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setInitialized();
                            }
                        });
                        log("Extension call started successfully. Call ID: " + voiceCall.getInfo().getCallIdString());
                    } catch (final Exception e) {
                        log("Error while making extension call: " + e.getMessage());
//                        setErrorState("Error while making extension call:" + e.getMessage(), e);
//                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });
        }

        public void completeTransfer() {
            // TODO call api to complete transfer: /calls/{callId}/_merge
//            if (voiceCall != null) {
//                CallOpParam prm = VoiceCall.createDefaultParams();
//                prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
//                try {
//                    voiceCall.xferReplaces(extensionCall, prm);
//                }
//                catch (Exception e) {
//                    System.out.println(e);
//                }
//            }
        }

        public void hangupExtension() {
            CallOpParam prm = VoiceCall.createDefaultParams();
            prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
            try {
                extensionCall.hangup(prm);
            } catch (Exception e) {
                setErrorState("Error while hanging up extension call: " + e.getMessage(), e);
            }
        }

        public void toggleExtensionHold() {

        }

        /** helper methods **/

        private void setErrorState(final String errorMessage, final Exception e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastCallState = CALL_STATE.ERROR;
                    callbacks.onErrorState(errorMessage, e);
                }
            });
        }

        private void setCallState(final String callState) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastCallState = callState;
                    callbacks.onCallState(callState, remoteNumber, remoteName);
                    switch (callState) {
                        case CALL_STATE.RINGING:
                            setRingingCallState(remoteNumber, remoteName);
                            break;
                        case CALL_STATE.HOLD:
                            setOtherCallState(remoteNumber, remoteName, R.string.call_hold, R.drawable.ic_call_paused_24dp);
                            break;
                        case CALL_STATE.DISCONNECTED:
                            setOtherCallState(remoteNumber, remoteName, R.string.call_state_call_ended, R.drawable.ic_call_end_24dp);
                            break;
                        default:
                            setOtherCallState(remoteNumber, remoteName, R.string.ongoing_call, R.drawable.ic_call_24dp);
                    }

                }
            });
        }

        private void setExtensionState(final String extensionState) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastExtensionState = extensionState;
                    callbacks.onExtensionState(extensionState, extensionNumber, extensionName);
                }
            });
        }

        private void acquireProximityWakelock() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                    if (proximityWakeLock == null && pm != null) {
                        proximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getClass().getSimpleName());
                        proximityWakeLock.acquire();
                    }
                }
            });
        }

        public void broadcastCallUpdates() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Bundle bundle = new Bundle(4);
                    bundle.putBoolean(CALL_UPDATE.UPDATE_MUTE, isMute);
                    bundle.putBoolean(CALL_UPDATE.UPDATE_HOLD, isHold);
                    bundle.putBoolean(CALL_UPDATE.UPDATE_SPEAKER, isSpeaker);
                    bundle.putLong(CALL_UPDATE.UPDATE_DURATION, duration);
                    callbacks.onCallUpdate(bundle);
                }
            });
        }

        public void broadcastCallState() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callbacks.onCallState(lastCallState, remoteNumber, remoteName);
                    callbacks.onExtensionState(lastExtensionState, extensionNumber, extensionName);
                }
            });
        }

        public boolean isInCallOfAnotherApp() {
            return isInCallOfAnotherApp;
        }

        public void setInCallOfAnotherApp(boolean inCallofAnotherApp) {
            isInCallOfAnotherApp = inCallofAnotherApp;
        }

        /** PJSIP callbacks **/

        @Override
        public void onSipRegistrationSuccess() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(isOutgoing) {
                        continueMakeCall();
                        return;
                    }
                    setCallState(CALL_STATE.WAITING_TO_CALL);
                    finishTimer = new Timer();
                    finishTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(CallingService.this, "Asterisk did not route the call to this device", Toast.LENGTH_LONG).show();
                                    hangupAndDestroy(DisconnectCause.ERROR);
                                }
                            });
                        }
                    }, WAITING_TO_CALL_MILLIS);
                }
            });
        }

        @Override
        public void onSipRegistrationFailure(final String errorMessage) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    setErrorState(errorMessage, null);
                    hangupAndDestroy(DisconnectCause.ERROR);
                }
            });
        }

        @Override
        public void onIncomingVoiceCall(final int callId) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (finishTimer != null) {
                        finishTimer.cancel();
                        finishTimer = null;
                    }
                }
            });
            try {
                voiceCall = new VoiceCall(voiceAccount, callId, voiceCore, VoiceConnection.this);
                CallOpParam prm = new CallOpParam();
                prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
                voiceCall.answer(prm);
                // update remoteNumber and remoteName
                String remoteUri = voiceCall.getInfo().getRemoteUri();
                if (!TextUtils.isEmpty(remoteUri)) {
                    Pattern pattern = Pattern.compile("\"?([^\"]*)\"? *<sip:(.+)@.+>");
                    Matcher matcher = pattern.matcher(remoteUri);
                    if (matcher.find()) {
                        remoteName = matcher.group(1);
                        remoteNumber = matcher.group(2);
                    }
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setInitialized();
                        setCallState(CALL_STATE.RINGING);
                    }
                });
                log("XXX getCallIdString: " + voiceCall.getInfo().getCallIdString());
                log("XXX getRemoteContact: " + voiceCall.getInfo().getRemoteContact());
                log("XXX getRemoteUri: " + voiceCall.getInfo().getRemoteUri());
                log("XXX getId: " + voiceCall.getInfo().getId());
                log("XXX getLocalContact: " + voiceCall.getInfo().getLocalContact());
                log("XXX getLocalUri: " + voiceCall.getInfo().getLocalUri());
                log("XXX getAccId: " + voiceCall.getInfo().getAccId());
            } catch (Exception e) {
                setErrorState("Error while notifying incoming call: " + e.getMessage(), e);
                hangupAndDestroy(DisconnectCause.ERROR);
            }
        }

        @Override
        public void onAnswerCall() {
            super.onAnswer();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (durationTimer == null) {
                        durationTimer = new Timer();
                        durationTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                Bundle bundle = new Bundle(1);
                                bundle.putLong(CALL_UPDATE.UPDATE_DURATION, ++duration);
                                callbacks.onCallUpdate(bundle);
                            }
                        }, 0, 1000);
                    }
                    setActive();
                    setCallState(CALL_STATE.ACTIVE);
                }
            });
        }

        /* Telecom subsystem callbacks bellow */

        @Override
        public void onDisconnect() { // both telecom or pjsip callback
            setCallState(CALL_STATE.DISCONNECTED);
            hangupAndDestroy(DisconnectCause.LOCAL);
        }

        @Override
        public void onAnswer() {
            answerIncomingCall();
        }

        @Override
        public void onCallError(Exception e) {
            setErrorState(e.getMessage(), e);
            hangupAndDestroy(DisconnectCause.LOCAL);
        }

        @Override
        public void onHold() {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        voiceCall.setHold(VoiceCall.createDefaultParams());
                        setOnHold();
                        isHold = true;
                        Bundle keyValue = new Bundle(1);
                        keyValue.putBoolean(CALL_UPDATE.UPDATE_HOLD, isHold);
                        callbacks.onCallUpdate(keyValue);
                        setCallState(CALL_STATE.HOLD);
                    } catch (Exception e) {
                        setErrorState("Error while holding a call: " + e.getMessage(), e);
                    }
                }
            });
        }

        @Override
        public void onUnhold() {
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CallOpParam param = VoiceCall.createDefaultParams();
                        param.getOpt().setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                        voiceCall.reinvite(param);
                        setActive();
                        isHold = false;
                        Bundle bundle = new Bundle(1);
                        bundle.putBoolean(CALL_UPDATE.UPDATE_HOLD, isHold);
                        callbacks.onCallUpdate(bundle);
                        setCallState(CALL_STATE.ACTIVE);
                    } catch (Exception e) {
                        setErrorState("Error while unholding a call: " + e.getMessage(), e);
                    }
                }
            });
        }

        @Override
        public void onAbort() {
            hangupAndDestroy(DisconnectCause.UNKNOWN);
        }

        @Override
        public void onReject() {
            hangupAndDestroy(DisconnectCause.REJECTED);
        }

    }

    private Intent createCallingActivityIntent(boolean answer, String remoteNumber, String remoteName) {
        return new Intent(CallingService.this, CallingActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName)
                .putExtra("answer", answer);
    }

    private PendingIntent createCallingPendingIntent(boolean answer, String remoteNumber, String remoteName) {
        Intent intent = createCallingActivityIntent(answer, remoteNumber, remoteName);
        return PendingIntent.getActivity(CallingService.this, answer ? 1 : 0, intent, pendingIntentFlags);
    }

    private PendingIntent createHangupPendingIntent() {
        Intent intent = new Intent(CallingService.this, CallingService.class).putExtra("command", COMMANDS.HANGUP_CALL);
        return PendingIntent.getService(CallingService.this, 0, intent, pendingIntentFlags);
    }

    private void createNotificationChannel(String channelId, int channelNameRes, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, getString(channelNameRes), importance);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startWorkerThread() {
        if (workerHandler == null) {
            if (workerThread == null) {
                workerThread = new HandlerThread("voice_thread_worker", Thread.MAX_PRIORITY);
                workerThread.setPriority(Thread.MAX_PRIORITY);
                workerThread.start();
            }
            workerHandler = new Handler(workerThread.getLooper());
            log("New worker thread initiated");
        }
    }

    private void stopWorkerThread() {
        if (workerThread != null) {
            workerThread.quitSafely();
            workerThread = null;
            workerHandler = null;
            log("Worker thread destroyed");
        }
    }

    private void startMainThread() {
        if (mainHandler == null) {
            if (mainThread == null) {
                mainThread = new HandlerThread("voice_thread_main", Process.THREAD_PRIORITY_FOREGROUND);
                mainThread.setPriority(Thread.MAX_PRIORITY);
                mainThread.start();
            }
            mainHandler = new Handler(mainThread.getLooper());
            log("New main thread initiated");
        }
    }

    private void stopMainThread() {
        if (mainThread != null) {
            mainThread.quitSafely();
            mainThread = null;
            mainHandler = null;
            log("Main thread destroyed");
        }
    }

    private void setOtherCallState(String remoteNumber, String remoteName, int titleRes, int iconRes) {
        String titleText = getString(titleRes);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), SERVICE_CHANNEL_ID);
        createNotificationChannel(SERVICE_CHANNEL_ID, R.string.ongoing_call, IMPORTANCE_LOW);
        NotificationCompat.Action hangupAction = new NotificationCompat.Action.Builder(R.drawable.ic_call_end_24dp, getString(R.string.hangup), createHangupPendingIntent()).build();
        Notification notification = builder
                .setSmallIcon(iconRes)
                .setTicker(titleText)
                .setContentTitle(titleText)
                .setContentText(createRemoteTitle(remoteNumber, remoteName))
                .setContentIntent(createCallingPendingIntent(false, remoteNumber, remoteName))
                .addAction(hangupAction)
                .build();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private String createRemoteTitle(String remoteNumber, String remoteName) {
        return !TextUtils.isEmpty(remoteName) ? remoteName : !TextUtils.isEmpty(remoteNumber) ? remoteNumber : getString(R.string.unknown);
    }

    private void setRingingCallState(String remoteNumber, String remoteName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // pre-oreo versions
            startActivity(createCallingActivityIntent(false, remoteNumber, remoteName));
        } else {
            String remoteTitle = createRemoteTitle(remoteNumber, remoteName);
            RemoteViews headUpLayout = new RemoteViews(getPackageName(), R.layout.notification_incoming_call);
            headUpLayout.setTextViewText(R.id.text, remoteTitle);
            headUpLayout.setOnClickPendingIntent(R.id.accept, createCallingPendingIntent(true, remoteNumber, remoteName));
            headUpLayout.setOnClickPendingIntent(R.id.decline, createHangupPendingIntent());
            createNotificationChannel(INCOMING_CHANNEL_ID, R.string.incoming_call, IMPORTANCE_HIGH);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(CallingService.this, INCOMING_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_call_24dp)
                    .setCustomBigContentView(headUpLayout)
                    .setCustomContentView(headUpLayout)
                    .setCustomHeadsUpContentView(headUpLayout)
                    .setContentTitle(getString(R.string.incoming_call))
                    .setContentText(remoteTitle)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(createCallingPendingIntent(false, remoteNumber, remoteName), true);
            Notification notification = builder.build();
            startForeground(INCOMING_NOTIFICATION_ID, notification);
        }
        startRingtone();
    }

    private void startRingtone() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (activeVoiceConnection.isInCallOfAnotherApp()) {
                    ringer.startBeeping();
                } else {
                    ringer.start();
                }
            }
        });
    }

    private void stopRingtone() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ringer.stop();
            }
        });
    }

    @Override
    public void onErrorState(final String errorMessage, final Exception e) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                log("ERROR: " + errorMessage, e);
                localBroadcastManager.sendBroadcast(new Intent(INTENT_FILTER_CALL_STATE)
                        .putExtra("error", errorMessage)
                        .putExtra("callState", CALL_STATE.ERROR));
            }
        });
    }

    @Override
    public void onCallState(final String callState, final String remoteNumber, final String remoteName) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                log("Call state: " + callState);
                localBroadcastManager.sendBroadcast(new Intent(INTENT_FILTER_CALL_STATE)
                        .putExtra("remoteNumber", remoteNumber)
                        .putExtra("remoteName", remoteName)
                        .putExtra("callState", callState));

            }
        });
    }

    @Override
    public void onCallUpdate(final Bundle keyValue) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                localBroadcastManager.sendBroadcast(new Intent(INTENT_FILTER_CALL_UPDATE).putExtras(keyValue));
                if (Logger.ALLOW_LOGGING) return;
                for (String key : keyValue.keySet()) {
                    Logger.d(TAG, "Call update: " + key + "=" + keyValue.get(key));
                }
            }
        });
    }

    @Override
    public void onExtensionState(final String extensionState, final String extensionNumber, final String extensionName) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                log("Extension state: " + extensionState);
                localBroadcastManager.sendBroadcast(new Intent(INTENT_FILTER_EXTENSION_STATE)
                        .putExtra("extensionNumber", extensionNumber)
                        .putExtra("extensionName", extensionName)
                        .putExtra("extensionState", extensionState));

            }
        });
    }

    @Override
    public void onExtensionUpdate(final Bundle keyValue) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                localBroadcastManager.sendBroadcast(new Intent(INTENT_FILTER_EXTENSION_UPDATE).putExtras(keyValue));
                if (Logger.ALLOW_LOGGING) return;
                for (String key : keyValue.keySet()) {
                    Logger.d(TAG, "Extension update: " + key + "=" + keyValue.get(key));
                }
            }
        });
    }

    @Override
    public void onConnectionDestroyed() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopRingtone();
                activeVoiceConnection = null;
                if (nextCallAhead) {
                    nextCallAhead = false;
                    log("Another incoming call is on the way...");
                    CallingCommands.incomingCall(getApplicationContext());
                } else {
                    log("Stopping self...");
                    stopSelf();
                }
            }
        });
    }

    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Exception e) {
        Logger.logToFile(getApplicationContext(), message);
        if (e != null) {
            Logger.e(TAG, message, e);
        } else {
            Logger.d(TAG, message);
        }
    }


}
