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
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.Process;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
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
    private static final String SIP_THREAD_NAME_MAIN = "SipThrdMain";
    private static final String SIP_THREAD_NAME_WORKER = "SipThrdWorker";
    private static final long[] VIBRATOR_PATTERN = {0, 1000, 0, 1000};
    private static final long WAITING_TO_CALL_MILLIS = 5000;
    public static final String INTENT_FILTER_CALL_STATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_CALL_STATE";
    public static final String INTENT_FILTER_CALL_UPDATE = "com.qualityunit.android.liveagentphone.INTENT_FILTER_CALL_UPDATE";
    private LocalBroadcastManager localBroadcastManager;

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
    }
    public static final class CALL_STATE {
        public static final String INITIALIZING_SIP_LIBRARY = "INITIALIZING_SIP_LIBRARY";
        public static final String REGISTERING_SIP_USER = "REGISTERING_SIP_USER";
        public static final String DIALING = "DIALING";
        public static final String WAITING_TO_CALL = "WAITING_TO_CALL";
        public static final String RINGING = "RINGING";
        public static final String CONNECTING = "CONNECTING";
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
    private PhoneAccountHandle phoneAccountHandle;
    private HandlerThread mainThread;
    private Handler mainHandler;
    private HandlerThread workerThread;
    private Handler workerHandler;
    private volatile boolean nextCallAhead;
    private static PowerManager.WakeLock wakeLock;
    private MediaPlayer ringTone;
    private Vibrator vibrator;
    private VoiceConnection activeVoiceConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.logToFile(getApplicationContext(), "SERVICE: Start 'onCreate'");
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        wakeLock.acquire();
        phoneAccountHandle = new PhoneAccountHandle(new ComponentName(getApplicationContext(), CallingService.class), PHONE_ACCOUNT_HANDLE_ID);

        startMainThread();
        startWorkerThread();
        Logger.logToFile(getApplicationContext(), "SERVICE: Done 'onCreate'");
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (intent == null) throw new CallingException("Intent is null");
                    String command = intent.getStringExtra("command");
                    Logger.logToFile(getApplicationContext(), "SERVICE: command " + command);
                    switch (command) {
                        case COMMANDS.MAKE_CALL:
                            Logger.logToFile(getApplicationContext(), "--------------------------------------------------");
                            if (activeVoiceConnection != null) throw new CallingException(getString(R.string.only_one_call));
                            Uri outgoingUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, intent.getStringExtra(VoiceConnection.EXTRA_REMOTE_NUMBER), null);
                            Bundle outgoingExtras = new Bundle();
                            outgoingExtras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
                            outgoingExtras.putBundle(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, new Bundle(intent.getExtras()));
                            try {
                                getTelecomManager().placeCall(outgoingUri, outgoingExtras);
                            } catch (SecurityException e) {
                                throw new CallingException(e.getMessage());
                            }
                            break;
                        case COMMANDS.INCOMING_CALL:
                            Logger.logToFile(getApplicationContext(), "--------------------------------------------------");
                            if (activeVoiceConnection != null) {
                                nextCallAhead = true;
                                return;
                            }
                            Bundle incomingExtras = new Bundle();
                            incomingExtras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
                            getTelecomManager().addNewIncomingCall(phoneAccountHandle, incomingExtras);
                            break;
                        case COMMANDS.ANSWER_CALL:
                            getActiveConnection().answerIncomingCall();
                            break;
                        case COMMANDS.SEND_DTMF:
                            getActiveConnection().sendDtfm(intent.getStringExtra("character"));
                            break;
                        case COMMANDS.HANGUP_CALL:
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
                        default:
                            throw new CallingException("Unknown CallingService command:" + command);
                    }
                } catch (Exception e) {
                    onErrorState("SERVICE: Error: " + e.getMessage(), e);
                }
            }
        });
        return START_NOT_STICKY;
    }

    private VoiceConnection getActiveConnection() throws CallingException {
        if(activeVoiceConnection == null) {
            throw new CallingException("No active voice connection");
        }
        return activeVoiceConnection;
    }

    private TelecomManager getTelecomManager() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        PhoneAccount.Builder builder = PhoneAccount.builder(phoneAccountHandle, PHONE_ACCOUNT_HANDLE_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED);
        }
        PhoneAccount phoneAccount = builder.build();
        telecomManager.registerPhoneAccount(phoneAccount);
        return telecomManager;
    }

    @Override
    public void onDestroy() {
        Logger.logToFile(getApplicationContext() ,"SERVICE: 'onDestroy'");
        stopRingtone();
        stopWorkerThread();
        stopMainThread();
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        Logger.logToFile(getApplicationContext() ,"SERVICE: 'DESTROYED'");
        super.onDestroy();
//        android.os.Process.killProcess(android.os.Process.myPid()); // this also kills calling activity (which is running on the same PID)
    }



    /** Telephony callbacks **/

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.d(TAG, "#### onCreateOutgoingConnection");
        activeVoiceConnection = new VoiceConnection(this);
        activeVoiceConnection.makeVoiceCall(request);
        return activeVoiceConnection;
    }

    @Override
    public void onCreateOutgoingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.d(TAG, "#### onCreateOutgoingConnectionFailed");
        Toast.makeText(this, "Failed to create LivePhone outgoing call", Toast.LENGTH_LONG).show();
        super.onCreateOutgoingConnectionFailed(connectionManagerPhoneAccount, request);
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.d(TAG, "#### onCreateIncomingConnection");
        activeVoiceConnection = new VoiceConnection(this);
        activeVoiceConnection.incomingVoiceCall();
        return activeVoiceConnection;
    }

    @Override
    public void onCreateIncomingConnectionFailed(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Log.d(TAG, "#### onCreateIncomingConnectionFailed");
        Toast.makeText(this, "Failed to create LivePhone incoming call", Toast.LENGTH_LONG).show();
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request);
    }

    final class VoiceConnection extends Connection implements VoiceCall.Callbacks, VoiceAccount.Callbacks {

        public static final String EXTRA_REMOTE_NUMBER = "EXTRA_REMOTE_NUMBER";
        public static final String EXTRA_REMOTE_NAME = "EXTRA_REMOTE_NAME";
        public static final String EXTRA_PREFIX = "EXTRA_PREFIX";
        private final AudioManager audioManager;
        private final String sipHost;
        private final String sipUser;
        private final String sipPassword;
        private PowerManager.WakeLock proximityWakeLock;
        private VoiceCall voiceCall;
        private VoiceCore voiceCore;
        private VoiceAccount voiceAccount;
        private boolean isOutgoing;
        private boolean isMute;
        private boolean isSpeaker;
        private boolean isHold;
        private boolean isHangingUp;
        private long duration = 0;
        private String lastCallState;
        private String remoteNumber;
        private String remoteName;
        private String prefix;
        private Timer finishTimer;
        private Timer durationTimer;
        private VoiceConnectionCallbacks callbacks;

        public VoiceConnection (VoiceConnectionCallbacks callbacks) {
            Log.d(TAG, "#### VoiceConnection: create");
            setInitializing();
            this.callbacks = callbacks;
            durationTimer = new Timer();
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
            Log.d(TAG, "#### makeVoiceCall");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    isOutgoing = true;
                    Bundle extras = request.getExtras();
                    prefix = extras.getString(EXTRA_PREFIX);
                    remoteNumber = extras.getString(EXTRA_REMOTE_NUMBER);
                    remoteName = extras.getString(EXTRA_REMOTE_NAME);
                    startActivity(createCallingActivityIntent(false, remoteNumber, remoteName));
                    acquireProximityWakelock();
                    initAndRegister();
                }
            });
        }

        public void incomingVoiceCall() {
            Log.d(TAG, "#### incomingVoiceCall");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    isOutgoing = false;
                    initAndRegister();
                }
            });
        }

        public void answerIncomingCall() {
            Log.d(TAG, "#### answerIncomingCall");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        stopRingtone();
                        CallOpParam prm = VoiceCall.createDefaultParams();
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
                        voiceCall.answer(prm);
                        acquireProximityWakelock();
                        Logger.logToFile(getApplicationContext() ,"CONNECTION: Call successfully received");
                    } catch (Exception e) {
                        callbacks.onErrorState("Error while answering call: " + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });
        }

        public void setCallState(final String callState) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    lastCallState = callState;
                    callbacks.onCallState(callState, remoteNumber, remoteName);
                }
            });
        }

        public void toggleHold() {
            Log.d(TAG, "#### toggleHold");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isHold) {
                        onUnhold();
                    } else {
                        onHold();
                    }
                }
            });
        }

        public void toggleSpeaker() {
            Log.d(TAG, "#### toggleSpeaker");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    audioManager.setSpeakerphoneOn(!isSpeaker);
                    isSpeaker = !isSpeaker;
                    Bundle keyValue = new Bundle(1);
                    keyValue.putBoolean(CALL_UPDATE.UPDATE_SPEAKER, isSpeaker);
                    callbacks.onCallUpdate(keyValue);
                }
            });
        }

        public void toggleMute() {
            Log.d(TAG, "#### toggleMute");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    audioManager.setMicrophoneMute(!isMute);
                    isMute = !isMute;
                    Bundle keyValue = new Bundle(1);
                    keyValue.putBoolean(CALL_UPDATE.UPDATE_MUTE, isMute);
                    callbacks.onCallUpdate(keyValue);
                }
            });
        }

        public void adjustIncallVolume(final boolean increase) {
            Log.d(TAG, "#### adjustIncallVolume: " + increase);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                }
            });
        }

        public void sendDtfm(final String character) {
            Log.d(TAG, "#### sendDtfm: " + character);
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (TextUtils.isEmpty(character)) throw new CallingException("Empty DTMF character");
                        if (!voiceCall.isActive()) throw new CallingException("Cannot send DTMF signal while call is not active");
                        voiceCall.dialDtmf(character);
                    } catch (Exception e) {
                        callbacks.onErrorState("Error while sending DTMF signal: " + e.getMessage(), e);
                    }
                }
            });
        }

        private void continueMakeCall() {
            Log.d(TAG, "#### continueMakeCall");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (TextUtils.isEmpty(remoteNumber)) throw new CallingException("Argument 'remoteNumber' is empty");
                        voiceCall = new VoiceCall(voiceAccount, -1, voiceCore, activeVoiceConnection);
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
//                        Logger.logToFile(getApplicationContext() ,"SERVICE: Call started successfully");
                    } catch (final Exception e) {
                        callbacks.onErrorState("Error while making SIP call:" + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });
        }

        private void initAndRegister() {
            Log.d(TAG, "#### initAndRegister");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
//                        Logger.logToFile(getApplicationContext() ,"CONNECTION: Initialization of SIP lib...");
                        if (TextUtils.isEmpty(sipHost)) throw new Exception("Argument 'sipHost' is empty or null");
                        if (TextUtils.isEmpty(sipUser)) throw new Exception("Argument 'sipUser' is empty or null");
                        if (TextUtils.isEmpty(sipPassword)) throw new Exception("Argument 'sipPassword' is empty or null");
                        if (voiceCore == null) {
                            setCallState(CALL_STATE.INITIALIZING_SIP_LIBRARY);
                            voiceCore = new VoiceCore();
                            voiceCore.init(workerThread.getName());
//                            Logger.logToFile(getApplicationContext() ,"CONNECTION: SIP initialized successfully");
                        }
                        if (voiceAccount == null || !voiceAccount.isRegistered()) {
                            setCallState(CALL_STATE.REGISTERING_SIP_USER);
                            voiceAccount = new VoiceAccount(VoiceConnection.this);
                            voiceAccount.register(sipHost, sipUser, sipPassword);
//                            Logger.logToFile(getApplicationContext() ,"CONNECTION: Account registration sent");
                        }
                        // wait for registration callback and continue in flow
                    } catch (final Exception e) {
                        callbacks.onErrorState("Error while initializing SIP lib: " + e.getMessage(), e);
                        hangupAndDestroy(DisconnectCause.ERROR);
                    }
                }
            });

        }

        public void hangupAndDestroy(final int cause) {
            Log.d(TAG, "#### hangupAndDestroy");
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
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
                                        Logger.logToFile(getApplicationContext() ,"CONNECTION: Library deinitialed");
                                        voiceCore = null;
                                    }
                                    Logger.logToFile(getApplicationContext() ,"CONNECTION: Hang up call: Call successfully hanged up");
                                } catch (final Exception e) {
                                    String err = "Error while hanging up call: " + e.getMessage();
                                    callbacks.onErrorState(err, e);
                                    Logger.logToFile(getApplicationContext() ,"CONNECTION: " + err);
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

        /* PJSIP callbacks */

        @Override
        public void onSipRegistrationSuccess() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(isOutgoing) {
                        continueMakeCall();
                    } else {
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
                }
            });
        }

        @Override
        public void onSipRegistrationFailure(final String errorMessage) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callbacks.onErrorState(errorMessage, null);
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
                    Pattern pattern = Pattern.compile("\\<sip\\:(.+)\\@.+\\>");
                    Matcher matcher = pattern.matcher(remoteUri);
                    if (matcher.find()) remoteNumber = matcher.group(1);
                    pattern = Pattern.compile("\\\"?([^\\\"]*)\\\"?");
                    matcher = pattern.matcher(remoteUri);
                    if (matcher.find()) remoteName = matcher.group(1);
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setInitialized();
                        setCallState(CALL_STATE.RINGING);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // pre-oreo versions
                            startActivity(createCallingActivityIntent(false, remoteNumber, remoteName));
                            startRingtone();
                            return;
                        }
                        RemoteViews headUpLayout = new RemoteViews(getPackageName(), R.layout.notification_incoming_call);
                        headUpLayout.setTextViewText(R.id.text, remoteName);
                        headUpLayout.setOnClickPendingIntent(R.id.accept, createCallingPendingIntent(true, remoteNumber, remoteName));
                        headUpLayout.setOnClickPendingIntent(R.id.decline, createHangupPendingIntent());
                        createNotificationChannel(INCOMING_CHANNEL_ID, R.string.incoming_call, IMPORTANCE_HIGH);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(CallingService.this, INCOMING_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_call_24dp)
                                .setCustomBigContentView(headUpLayout)
                                .setCustomContentView(headUpLayout)
                                .setCustomHeadsUpContentView(headUpLayout)
                                .setContentTitle(getString(R.string.incoming_call))
                                .setContentText(remoteNumber)
                                .setOngoing(true)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_CALL)
                                .setFullScreenIntent(createCallingPendingIntent(false, remoteNumber, remoteName), true);
                        Notification notification = builder.build();
                        startForeground(INCOMING_NOTIFICATION_ID, notification);
                        startRingtone();
                    }
                });
            } catch (Exception e) {
                callbacks.onErrorState("Error while notifying incoming call: " + e.getMessage(), e);
                hangupAndDestroy(DisconnectCause.ERROR);
            }
        }

        /* Telephony callbacks */

        @Override
        public void onHold() {
            Log.d(TAG, "#### onHold");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        voiceCall.setHold(VoiceCall.createDefaultParams());
                        isHold = true;
                        Bundle keyValue = new Bundle(1);
                        keyValue.putBoolean(CALL_UPDATE.UPDATE_HOLD, isHold);
                        callbacks.onCallUpdate(keyValue);
                        setCallState(CALL_STATE.HOLD);
                    } catch (Exception e) {
                        callbacks.onErrorState("Error while holding a call: " + e.getMessage(), e);
                    }
                }
            });
        }

        @Override
        public void onUnhold() {
            Log.d(TAG, "#### onUnhold");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CallOpParam param = VoiceCall.createDefaultParams();
                        param.getOpt().setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                        voiceCall.reinvite(param);
                        isHold = false;
                        Bundle bundle = new Bundle(1);
                        bundle.putBoolean(CALL_UPDATE.UPDATE_HOLD, isHold);
                        callbacks.onCallUpdate(bundle);
                        setCallState(CALL_STATE.ACTIVE);
                    } catch (Exception e) {
                        callbacks.onErrorState("Error while unholding a call: " + e.getMessage(), e);
                    }
                }
            });
        }

        @Override
        public void onAbort() {
            Log.d(TAG, "#### onAbort");
            hangupAndDestroy(DisconnectCause.UNKNOWN);
        }

        @SuppressLint("WakelockTimeout")
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

        @Override
        public void onAnswerCall() {
            Log.d(TAG, "#### onAnswerCall");
            super.onAnswer();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    durationTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            Bundle bundle = new Bundle(1);
                            bundle.putLong(CALL_UPDATE.UPDATE_DURATION, ++duration);
                            callbacks.onCallUpdate(bundle);
                        }
                    }, 0, 1000);
                    setActive();
                    setCallState(CALL_STATE.ACTIVE);
                }
            });
        }

        /** Can be called from either telephony or pjsip **/
        @Override
        public void onDisconnect() {
            Log.d(TAG, "#### onDisconnect");
            setCallState(CALL_STATE.DISCONNECTED);
            hangupAndDestroy(DisconnectCause.LOCAL);
        }

        @Override
        public void onReject() {
            Log.d(TAG, "#### onReject");
            hangupAndDestroy(DisconnectCause.REJECTED);
        }

        @Override
        public void onShowIncomingCallUi() {
            // DO NOT SHOW UI, WE SHOW IT MANUALLY
            Log.d(TAG, "#### Connection: onShowIncomingCallUi");
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
                }
            });
        }
    }

    private Intent createCallingActivityIntent(boolean answer, String remoteNumber, String remoteName) {
        return new Intent(CallingService.this, CallingActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName)
                .putExtra("answer", answer);
    }

    private void runAsForeground(final String callState, final String remoteNumber, final String remoteName) {
        Log.d(TAG, "#### runAsForeground: " + callState);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                String titleText;
                int icon;
                switch (callState) {
                    case CALL_STATE.HOLD:
                        titleText = getString(R.string.call_hold);
                        icon = R.drawable.ic_call_paused_24dp;
                        break;
                    case CALL_STATE.DISCONNECTED:
                        titleText = getString(R.string.call_state_call_ended);
                        icon = R.drawable.ic_call_end_24dp;
                        break;
                    default:
                        titleText = getString(R.string.ongoing_call);
                        icon = R.drawable.ic_call_24dp;
                }
                String notificationContentText = !TextUtils.isEmpty(remoteName) ? remoteName : !TextUtils.isEmpty(remoteNumber) ? remoteNumber : getString(R.string.unknown);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), SERVICE_CHANNEL_ID);
                createNotificationChannel(SERVICE_CHANNEL_ID, R.string.ongoing_call, IMPORTANCE_LOW);
                NotificationCompat.Action hangupAction = new NotificationCompat.Action.Builder(R.drawable.ic_call_end_24dp, getString(R.string.hangup), createHangupPendingIntent()).build();
                Notification notification = builder
                        .setSmallIcon(icon)
                        .setTicker(titleText)
                        .setContentTitle(titleText)
                        .setContentText(notificationContentText)
                        .setContentIntent(createCallingPendingIntent(false, remoteNumber, remoteName))
                        .addAction(hangupAction)
                        .build();
                startForeground(ONGOING_NOTIFICATION_ID, notification);
            }
        });
    }

    private PendingIntent createCallingPendingIntent(boolean answer, String remoteNumber, String remoteName) {
        Intent intent = createCallingActivityIntent(answer, remoteNumber, remoteName);
        return PendingIntent.getActivity(CallingService.this, answer ? 1 : 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent createHangupPendingIntent() {
        Intent intent = new Intent(CallingService.this, CallingService.class).putExtra("command", COMMANDS.HANGUP_CALL);
        return PendingIntent.getService(CallingService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
                workerThread = new HandlerThread(SIP_THREAD_NAME_WORKER, Thread.MAX_PRIORITY);
                workerThread.setPriority(Thread.MAX_PRIORITY);
                workerThread.start();
            }
            workerHandler = new Handler(workerThread.getLooper());
            Logger.logToFile(getApplicationContext() ,"SERVICE: New worker thread initiated");
        } else {
            Logger.logToFile(getApplicationContext() ,"SERVICE: Previous worker thread used");
        }
    }

    private void stopWorkerThread() {
        if (workerThread != null) {
            workerThread.quitSafely();
            workerThread = null;
            workerHandler = null;
            Logger.logToFile(getApplicationContext() ,"SERVICE: Worker thread destroyed");
        }
    }

    private void startMainThread() {
        if (mainHandler == null) {
            if (mainThread == null) {
                mainThread = new HandlerThread(SIP_THREAD_NAME_MAIN, Process.THREAD_PRIORITY_FOREGROUND);
                mainThread.setPriority(Thread.MAX_PRIORITY);
                mainThread.start();
            }
            mainHandler = new Handler(mainThread.getLooper());
            Logger.logToFile(getApplicationContext() ,"SERVICE: New main thread initiated");
        } else {
            Logger.logToFile(getApplicationContext() ,"SERVICE: Previous main thread used");
        }

    }

    private void stopMainThread() {
        if (mainThread != null) {
            mainThread.quitSafely();
            mainThread = null;
            mainHandler = null;
            Logger.logToFile(getApplicationContext() ,"SERVICE: Main thread destroyed");
        }
    }


    private void startRingtone() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
                    ringTone = new MediaPlayer();
                    ringTone.setDataSource(getApplicationContext(), ringtoneUri);
                    ringTone.setAudioStreamType(AudioManager.STREAM_RING);
                    ringTone.setLooping(true);
                    ringTone.prepare();
                    ringTone.start();
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .build();
                    vibrator.vibrate(VIBRATOR_PATTERN, 0, audioAttributes);
                } catch (Exception e) {
                    Logger.e(TAG, "Error while trying to play ringtone!", e);
                }
            }
        });
    }

    private void stopRingtone() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (vibrator != null) {
                    vibrator.cancel();
                    vibrator = null;
                }
                if (ringTone != null) {
                    try {
                        if (ringTone.isPlaying())
                            ringTone.stop();
                    } catch (Exception ignored) {}
                    try {
                        ringTone.reset();
                        ringTone.release();
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    @Override
    public void onErrorState(final String error, Exception e) {
        Log.e(TAG, error, e);
        Logger.logToFile(getApplicationContext() ,"SERVICE: ERROR STATE: " + error);
        broadcast(new Intent(INTENT_FILTER_CALL_STATE)
                .putExtra("error", error)
                .putExtra("callState", CALL_STATE.ERROR));
    }

    @Override
    public void onCallState(final String callState, final String remoteNumber, final String remoteName) {
        Logger.logToFile(getApplicationContext() ,"SERVICE: CALL STATE: " + callState);
        runAsForeground(callState, remoteNumber, remoteName);
        broadcast(new Intent(INTENT_FILTER_CALL_STATE)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName)
                .putExtra("callState", callState));
    }

    @Override
    public void onCallUpdate(final Bundle keyValue) {
        for (String key : keyValue.keySet()) {
            Logger.logToFile(getApplicationContext() ,"SERVICE: CALL UPDATE: " + key + "=" + keyValue.get(key));
        }
        broadcast(new Intent(INTENT_FILTER_CALL_UPDATE).putExtras(keyValue));
    }

    @Override
    public void onConnectionDestroyed() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                stopRingtone();
                activeVoiceConnection = null;
                if (nextCallAhead) {
                    // another incoming call is on the way...
                    CallingCommands.incomingCall(getApplicationContext());
                } else {
                    stopSelf();
                }
            }
        });
    }

    private void broadcast(final Intent intent) {
        if (mainHandler != null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (localBroadcastManager == null) {
                        localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                    }
                    localBroadcastManager.sendBroadcast(intent);
                }
            });
        }

    }

}
