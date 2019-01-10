package com.qualityunit.android.liveagentphone.service;

import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.ui.call.CallingActivity;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.qualityunit.android.sip.SipAccount;
import com.qualityunit.android.sip.SipAppObserver;
import com.qualityunit.android.sip.SipCall;
import com.qualityunit.android.sip.SipCore;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsip_status_code;
import org.pjsip.pjsua2.pjsua_call_flag;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rasto on 17.10.16.
 */

public class CallingService extends Service implements SipAppObserver {

    private static final String TAG = CallingService.class.getSimpleName();
    private static final String SIP_THREAD_NAME = "SipThrd";
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final long[] VIBRATOR_PATTERN = {0, 1000, 1000};
    private static final long WAITING_TO_CALL_MILLIS = 5000;
    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_CALLBACK = "KEY_CALLBACK";
    public static final String KEY_CALL_DIRECTION = "KEY_CALL_DIRECTION";
    public static final String INTENT_FILTER_CALLBACK = "com.qualityunit.android.liveagentphone.SIPEVENTS";
    public static final class COMMANDS {
        public static final int MAKE_CALL = 1;
        public static final int INCOMING_CALL = 2;
        public static final int RECEIVE_CALL = 3;
        public static final int HANGUP_CALL = 4;
        public static final int UPDATE_STATE = 6;
        public static final int SEND_DTMF = 7;
        public static final int SILENCE_RINGING = 8;
        public static final int TOGGLE_MUTE = 9;
        public static final int TOGGLE_SPEAKER = 10;
        public static final int UPDATE_ALL = 11;
        public static final int ADJUST_INCALL_VOLUME = 12;
        public static final int TOGGLE_HOLD = 13;
    }
    public static final class CALLBACKS {
        public static final String INITIALIZING = "INITIALIZING";
        public static final String REGISTERING_SIP_USER = "REGISTERING_SIP_USER";
        public static final String STARTING_CALL = "STARTING_CALL";
        public static final String CONNECTING = "CONNECTING";
        public static final String CALLING = "CALLING";
        public static final String HANGING_UP_CALL = "HANGING_UP_CALL";
        public static final String CALL_ENDED = "CALL_ENDED";
        public static final String CALL_ESTABLISHED = "CALL_ESTABLISHED";
        public static final String RINGING = "RINGING";
        public static final String WAITING_TO_CALL = "WAITING_TO_CALL";
        public static final String ERROR = "ERROR";
        public static final String UPDATE_DURATION = "UPDATE_DURATION";
        public static final String UPDATE_MUTE = "TOGGLE_MUTE";
        public static final String UPDATE_SPEAKER = "TOGGLE_SPEAKER";
        public static final String UPDATE_HOLD = "UPDATE_HOLD";
    }
    public static final class CALL_DIRECTION {
        public static final int OUTGOING = 1;
        public static final int INCOMING = 2;
    }
    private boolean isGsmIdle;
    private boolean isMute;
    private boolean isSpeaker;
    private boolean isHold;
    private boolean isMissedCall;
    private long startTime = -1;
    private String lastState = "";
    private String notificationContentText;
    private long notificationWhen = System.currentTimeMillis();
    private HandlerThread mainThread;
    private Handler mainHandler;
    private HandlerThread workerThread;
    private Handler workerHandler;
    private volatile boolean waitingToCall;
    private String sipUser;
    private String sipHost;
    private String prefix;
    private String remoteNumber;
    private String remoteName;
    private String sipPassword;
    private int callDirection;
    private static PowerManager.WakeLock wakeLock;
    private static PowerManager.WakeLock proximityWakeLock;
    private MediaPlayer ringTone;
    private Vibrator vibrator;
    private TelephonyManager telephonyManager;
    private BroadcastReceiver gsmStateReceiver = new GsmStateChangedReceiver();
    // pjsua objects
    private SipCore sipCore;
    private SipCall sipCurrentCall;
    private SipAccount sipAccount;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.logToFile("SERVICE: Start 'onCreate'");
        // acquire wakelock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        wakeLock.acquire();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            proximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getClass().getSimpleName());
            proximityWakeLock.acquire();
        }
        Logger.logToFile("SERVICE: Wakelocks acquired");
        startMainThread();
        startWorkerThread();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                isGsmIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
                IntentFilter phoneStateFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
                registerReceiver(gsmStateReceiver, phoneStateFilter);
            }
        });
        Logger.logToFile("SERVICE: Done 'onCreate'");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                String info = "SERVICE: command ";
                try {
                    if (intent == null) {
                        throw new CallingException("Intent is null");
                    }
                    int command = intent.getIntExtra(KEY_COMMAND, 0);
                    info += " " + command + " ";
                    switch (command) {
                        case COMMANDS.MAKE_CALL:
                            Logger.logToFile("=============================================================================================");
                            info += "MAKE_CALL";
                            Logger.logToFile(info);
                            if (sipCurrentCall != null) throw new CallingException(getString(R.string.only_one_call));
                            prefix = intent.getStringExtra("prefix");
                            remoteNumber = intent.getStringExtra("remoteNumber");
                            if (TextUtils.isEmpty(remoteNumber)) throw new CallingException("Argument 'remoteNumber' is empty");
                            remoteName = intent.getStringExtra("remoteName");
                            callDirection = CALL_DIRECTION.OUTGOING;
                            notificationContentText = TextUtils.isEmpty(remoteName) ? remoteNumber : remoteName ;
                            startActivity(createCallingActivityIntent());
                            initAndRegister();
                            break;
                        case COMMANDS.INCOMING_CALL:
                            info += "INCOMING_CALL";
                            Logger.logToFile(info);
                            if (!isGsmIdle) {
                                Logger.logToFile("SERVICE: Ongoing GSM call");
                                break;
                            }
                            callDirection = CALL_DIRECTION.INCOMING;
                            initAndRegister();
                            break;
                        case COMMANDS.RECEIVE_CALL:
                            info += "RECEIVE_CALL";
                            Logger.logToFile(info);
                            receiveCall();
                            break;
                        case COMMANDS.SEND_DTMF:
                            info += "SEND_DTMF";
                            Logger.logToFile(info);
                            String character = intent.getStringExtra("character");
                            if (TextUtils.isEmpty(character)) throw new CallingException("Empty DTMF argument");
                            if (sipCurrentCall == null) throw new CallingException("Cannot send DTMF signal when calling service is not running");
                            sendDtfm(character);
                            break;
                        case COMMANDS.HANGUP_CALL:
                            info += "HANGUP_CALL";
                            Logger.logToFile(info);
                            hangupCallAndFinishService();
                            break;
                        case COMMANDS.ADJUST_INCALL_VOLUME:
                            info += "ADJUST_INCALL_VOLUME";
                            Logger.logToFile(info);
                            boolean increase = intent.getBooleanExtra("increase", true);
                            adjustIncallVolume(increase);
                            break;
                        case COMMANDS.SILENCE_RINGING:
                            info += "SILENCE_RINGING";
                            Logger.logToFile(info);
                            stopRingtone();
                            break;
                        case COMMANDS.UPDATE_STATE:
                            info += "UPDATE_STATE";
                            Logger.logToFile(info);
                            setCallState(lastState);
                            break;
                        case COMMANDS.TOGGLE_MUTE:
                            info += "TOGGLE_MUTE";
                            Logger.logToFile(info);
                            enableMute(!isMute);
                            break;
                        case COMMANDS.TOGGLE_SPEAKER:
                            info += "TOGGLE_SPEAKER";
                            Logger.logToFile(info);
                            enableSpeaker(!isSpeaker);
                            break;
                        case COMMANDS.TOGGLE_HOLD:
                            info += "TOGGLE_HOLD";
                            Logger.logToFile(info);
                            callHold(!isHold);
                            break;
                        case COMMANDS.UPDATE_ALL:
                            info += "UPDATE_ALL";
                            Logger.logToFile(info);
                            sendUpdateDurationBroadcast();
                            sendUpdateMuteBroadcast();
                            sendUpdateSpeakerBroadcast();
                            sendUpdateHoldBroadcast();
                            break;
                        default:
                            throw new CallingException("Service started with unknown command:" + command);
                    }
                } catch (Exception e) {
                    String err = "SERVICE: Error: Starting " + e.getMessage();
                    setError(err, e);
                    Logger.logToFile(err);
                }
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logToFile("SERVICE: 'onDestroy'");
        stopRingtone();
        unregisterReceiver(gsmStateReceiver);
        stopWorkerThread();
        stopMainThread();
        if (proximityWakeLock != null) {
            proximityWakeLock.release();
            proximityWakeLock = null;
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        super.onDestroy();
        Logger.logToFile("SERVICE: 'Killing process...'");
        android.os.Process.killProcess(android.os.Process.myPid()); // this also kills calling activity (which is running on the same PID)
    }

    private void sendDtfm(final String character) {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sipCurrentCall != null && sipCurrentCall.isActive()) {
                        sipCurrentCall.dialDtmf(character);
                    } else {
                        setError("Cannot send DTMF signal while call is not active", null);
                    }
                } catch (Exception e) {
                    setError("Error while sending DTMF signal", e);
                }
            }
        });
    }

    private Intent createCallingActivityIntent() {
        return new Intent(CallingService.this, CallingActivity.class)
                .putExtra(KEY_CALL_DIRECTION, callDirection)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private void runAsForeground() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                String titleText = "";
                int icon = 0;
                if (CALLBACKS.INITIALIZING.equals(lastState)) {
                    if (callDirection == CALL_DIRECTION.OUTGOING) {
                        titleText = getString(R.string.outgoing_call);
                        icon = R.drawable.ic_call_24dp;
                    }
                    else if (callDirection == CALL_DIRECTION.INCOMING) {
                        titleText = getString(R.string.incoming_call);
                        icon = R.drawable.ic_ring_volume_24dp;
                    }
                } else if (CALLBACKS.CALL_ESTABLISHED.equals(lastState)) {
                    if (isHold) {
                        titleText = getString(R.string.call_hold);
                        icon = R.drawable.ic_call_paused_24dp;
                    } else {
                        titleText = getString(R.string.ongoing_call);
                        icon = R.drawable.ic_call_24dp;
                    }
                } else if (CALLBACKS.CALL_ENDED.equals(lastState)) {
                    titleText = getString(R.string.call_state_call_ended);
                    icon = R.drawable.ic_call_end_24dp;
                } else {
                    // else do not change notification
                    return;
                }
                PendingIntent pendingNotificationIntent = PendingIntent.getActivity(CallingService.this, 0,
                        createCallingActivityIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
                PendingIntent pendingHangupIntent = PendingIntent.getService(CallingService.this, 0,
                        new Intent(CallingService.this, CallingService.class).putExtra(KEY_COMMAND, COMMANDS.HANGUP_CALL),
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String channelId = "calling_service";
                    NotificationChannel channel = new NotificationChannel(channelId, "Calling Service" , NotificationManager.IMPORTANCE_NONE);
                    channel.setLightColor(Color.GREEN);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                    builder = new Notification.Builder(getApplicationContext(), channelId);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.createNotificationChannel(channel);
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    builder = new Notification.Builder(getApplicationContext());
                }
                Notification notification = builder
                        .setSmallIcon(icon)
                        .setTicker(titleText)
                        .setContentTitle(titleText)
                        .setContentText(notificationContentText)
                        .setWhen(notificationWhen)
                        .setContentIntent(pendingNotificationIntent)
                        .addAction(0, getString(R.string.hangup), pendingHangupIntent)
                        .build();
                startForeground(ONGOING_NOTIFICATION_ID, notification);
            }
        });
    }

    private void startWorkerThread() {
        if (workerHandler == null) {
            if (workerThread == null) {
                workerThread = new HandlerThread(SIP_THREAD_NAME, Thread.MAX_PRIORITY);
                workerThread.start();
            }
            workerHandler = new Handler(workerThread.getLooper());
            Logger.logToFile("SERVICE: New worker thread initiated");
        } else {
            Logger.logToFile("SERVICE: Previous worker thread used");
        }
    }

    private void stopWorkerThread() {
        if (workerThread != null) {
            workerThread.quitSafely();
            workerThread = null;
            workerHandler = null;
            Logger.logToFile("SERVICE: Worker thread destroyed");
        }
    }

    private void startMainThread() {
        if (mainHandler == null) {
            if (mainThread == null) {
                mainThread = new HandlerThread(SIP_THREAD_NAME, Process.THREAD_PRIORITY_FOREGROUND);
                mainThread.setPriority(Thread.MAX_PRIORITY);
                mainThread.start();
            }
            mainHandler = new Handler(mainThread.getLooper());
            Logger.logToFile("SERVICE: New main thread initiated");
        } else {
            Logger.logToFile("SERVICE: Previous main thread used");
        }

    }

    private void stopMainThread() {
        if (mainThread != null) {
            mainThread.quitSafely();
            mainThread = null;
            mainHandler = null;
            Logger.logToFile("SERVICE: Main thread destroyed");
        }
    }

    private void initAndRegister() {
        try {
            Logger.logToFile("SERVICE: Initialization of SIP lib...");
            // unmute and turn off speaker from previous sessions
            enableMute(false);
            enableSpeaker(false);
            AccountManager accountManager = AccountManager.get(getApplicationContext());
            sipHost = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_HOST);
            sipUser = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_USER);
            sipPassword = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_SIP_PASS);
            if (TextUtils.isEmpty(sipHost)) throw new Exception("Argument 'sipHost' is empty or null");
            if (TextUtils.isEmpty(sipUser)) throw new Exception("Argument 'sipUser' is empty or null");
            if (TextUtils.isEmpty(sipPassword)) throw new Exception("Argument 'sipPassword' is empty or null");
            workerHandler.post(new Runnable() {
                @Override
                public void run() {
                    // init sip lib
                    if (sipCore == null) {
                        setCallState(CALLBACKS.INITIALIZING);
                        sipCore = new SipCore();
                        try {
                            sipCore.init(CallingService.this, workerThread.getName());
                            Logger.logToFile("SERVICE: SIP initialized successfully");
                        } catch (final Exception e) {
                            String errMsg = "Error while initializing: SIP lib: " + e.getMessage();
                            Logger.logToFile(errMsg);
                            setError(errMsg, e);
                            finishService();
                        }
                    }

                    if (sipAccount == null) {
                        setCallState(CALLBACKS.REGISTERING_SIP_USER);
                        AccountConfig sipAccountConfig = createAccountConfig();
                        try {
                            sipAccount = new SipAccount(sipAccountConfig, sipCore);
                            sipAccount.create(sipAccountConfig, true);
                            Logger.logToFile("SERVICE: Account registration sent");
                        } catch (final Exception e) {
                            String errMsg = "Error while creating and registering sipAccount" + e.getMessage();
                            Logger.logToFile(errMsg);
                            setError(errMsg, e);
                            finishService();
                        }
                    }
                    // wait for registration callback and continue from listener "notifyRegState"
                }
            });
        } catch (final Exception e) {
            String errMsg = "Error while initializing sip lib: " + e.getMessage();
            Logger.logToFile(errMsg);
            setError(errMsg, e);
            finishService();
        }
    }

    private AccountConfig createAccountConfig() {
        // create URIs
        String sipRegisterUri = Tools.Sip.createRegisterUri(sipHost);
        String sipAccountUri = Tools.Sip.createAccountUri(sipUser, sipHost);
        // set sipAccount config
        AccountConfig sipAccountConfig = new AccountConfig();
        sipAccountConfig.getRegConfig().setRegistrarUri(sipRegisterUri);
        sipAccountConfig.getRegConfig().setRegisterOnAdd(true);
        sipAccountConfig.setIdUri(sipAccountUri);
        AuthCredInfoVector creds = sipAccountConfig.getSipConfig().getAuthCreds();
        creds.clear();
        creds.add(new AuthCredInfo("digest", "*", sipUser, 0, sipPassword));
        sipAccountConfig.getSipConfig().setAuthCreds(creds);
        sipAccountConfig.getNatConfig().setIceEnabled(true);
        sipAccountConfig.getVideoConfig().setAutoTransmitOutgoing(false);
        sipAccountConfig.getVideoConfig().setAutoShowIncoming(false);
        return sipAccountConfig;
    }

    private void makeCall() {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sipCurrentCall != null) {
                    Logger.logToFile("Warning: Make call: Only one call at anytime!");
                    return;
                }
                setCallState(CALLBACKS.STARTING_CALL);
                SipCall call = new SipCall(sipAccount, -1, sipCore);
                CallOpParam prm = new CallOpParam();
                CallSetting opt = prm.getOpt();
                opt.setAudioCount(1);
                opt.setVideoCount(0);
                opt.setFlag(0);
                try {
                    String cleanedRemoteNumber = Tools.Sip.cleanNumber(remoteNumber);
                    String sipCalleeUri = Tools.Sip.createCalleeUri(prefix, cleanedRemoteNumber, sipHost);
                    call.makeCall(sipCalleeUri, prm);
                    sipCurrentCall = call;
                    Log.d(TAG, "#### Call to '" + sipCalleeUri + "' started successfully");
                    Logger.logToFile("SERVICE: Call started successfully");
                } catch (final Exception e) {
                    call.delete();
                    setError("Error while making SIP call:" + e.getMessage(), e);
                    finishService();
                }
            }
        });
    }

    private void waitForIncomingCall() {
        Logger.logToFile("SERVICE: Waiting to incoming call...");
        setCallState(CALLBACKS.WAITING_TO_CALL);
        waitingToCall = true;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (waitingToCall) {
                    Logger.logToFile("SERVICE: Hanging up call because waiting reached " + WAITING_TO_CALL_MILLIS + " seconds");
                    finishService();
                }
            }
        }, WAITING_TO_CALL_MILLIS);
    }

    private void receiveCall() {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sipCurrentCall == null) {
                    Logger.logToFile("Warning: Receive call: No ringing call now");
                    return;
                }
                try {
                    stopRingtone();
                    CallOpParam prm = new CallOpParam();
                    prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
                    sipCurrentCall.answer(prm);
                    Logger.logToFile("SERVICE: Call successfully received");
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isMissedCall = false;
                        }
                    });
                    setCallState(CALLBACKS.CALL_ESTABLISHED);
                } catch (Exception e) {
                    String err = "Error while receiving call: " + e.getMessage();
                    Logger.logToFile(err);
                    Logger.e(TAG, e);
                }
            }
        });
    }

    private void adjustIncallVolume(final boolean increase) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, increase ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            }
        });
    }

    private void hangupCallAndFinishService() {
        stopRingtone();
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                setCallState(CALLBACKS.HANGING_UP_CALL);
                if (sipCurrentCall != null) {
                    try {
                        CallOpParam prm = new CallOpParam();
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
                        sipCurrentCall.hangup(prm);
                        Logger.logToFile("SERVICE: Hang up call: Call successfully hanged up");
//                        sipCurrentCall = null;
                        // finishing of service is then called from "notifyCallState"
                    } catch (final Exception e) {
                        setError("Error while hanging up call", e);
                        finishService();
                    }
                } else {
                    String err = "SERVICE: No call to hang up";
                    Logger.logToFile(err);
                    setError(err, null);
                    finishService();
                }
            }
        });
    }

    private void callHold(final boolean hold) {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sipCurrentCall == null) {
                    setError("There is no any ongoing call.", null);
                    return;
                }
                try {
                    CallOpParam param = new CallOpParam();
                    if (hold) {
                        Logger.logToFile("SERVICE: Holding...");
                        sipCurrentCall.setHold(param);
                    } else {
                        Logger.logToFile("SERVICE: Unholding...");
                        CallSetting opt = param.getOpt();
                        opt.setAudioCount(1);
                        opt.setVideoCount(0);
                        opt.setFlag(pjsua_call_flag.PJSUA_CALL_UNHOLD.swigValue());
                        sipCurrentCall.reinvite(param);
                    }
                    isHold = hold;
                    sendUpdateHoldBroadcast();
                    runAsForeground(); // update status bar icon
                } catch (Exception e) {
                    String errMsg = "Error while trying to hold a call: " + e.getMessage();
                    Logger.logToFile(errMsg);
                    setError(errMsg, null);
                }
            }
        });
    }

    private void enableMute(final boolean mute) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setMicrophoneMute(mute);
                isMute = mute;
                sendUpdateMuteBroadcast();
            }
        });
    }

    private void enableSpeaker(final boolean speaker) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setSpeakerphoneOn(speaker);
                isSpeaker = speaker;
                sendUpdateSpeakerBroadcast();
            }
        });
    }

    private void finishService() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                workerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        sipCurrentCall = null;
                        if (sipCore != null) {
                            sipCore.deinit();
                            Logger.logToFile("SERVICE: Finishing service: Library deinitialized");
                        }
                        sipCore = null;
                        sipAccount = null;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                stopSelf();
                            }
                        });
                    }
                });
            }
        });
    }

    private void startRingtone() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(VIBRATOR_PATTERN, 0);
                try {
                    Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
                    ringTone = new MediaPlayer();
                    ringTone.setDataSource(getApplicationContext(), ringtoneUri);
                    ringTone.setAudioStreamType(AudioManager.STREAM_RING);
                    ringTone.setLooping(true);
                    ringTone.prepare();
                    ringTone.start();
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
                    } catch (Exception ignored) { }
                    try {
                        ringTone.reset();
                        ringTone.release();
                    } catch (Exception ignored) { }
                }
            }
        });
    }

    private void showMissedCall(String remoteNumber, String remoteName) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        String missedCallStr = getString(R.string.missed_call);
        notificationBuilder.setContentTitle(missedCallStr);
        notificationBuilder.setContentText(TextUtils.isEmpty(remoteName) ? remoteNumber : remoteName);
        notificationBuilder.setTicker(missedCallStr);
        notificationBuilder.setSmallIcon(R.drawable.ic_phone_missed_black_24dp);
        notificationBuilder.setAutoCancel(true);

        Intent notificationIntent = new Intent(getApplicationContext(), DialerActivity.class);
        notificationIntent.putExtra("number", remoteNumber);
        if (!TextUtils.isEmpty(remoteName)) {
            notificationIntent.putExtra("remoteName", remoteName);
        }
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addParentStack(DialerActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
        notificationBuilder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager;
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }

    private void setCallState(final String callback) {
        Logger.logToFile("SERVICE: CALL STATE: " + callback);
        runAsForeground();
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                lastState = callback;
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                        .putExtra(KEY_CALLBACK, callback));
            }
        });
    }

    private void setError(final String errorMessage, @Nullable Throwable e) {
        Logger.e(TAG, errorMessage, e);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                        .putExtra(KEY_CALLBACK, CALLBACKS.ERROR)
                        .putExtra("errorMessage", errorMessage));
            }
        });
    }

    private void sendUpdateDurationBroadcast() {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                .putExtra(KEY_CALLBACK, CALLBACKS.UPDATE_DURATION)
                .putExtra("startTime", startTime));
    }

    private void sendUpdateMuteBroadcast() {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                .putExtra(KEY_CALLBACK, CALLBACKS.UPDATE_MUTE)
                .putExtra("isMute", isMute));
    }

    private void sendUpdateSpeakerBroadcast() {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                .putExtra(KEY_CALLBACK, CALLBACKS.UPDATE_SPEAKER)
                .putExtra("isSpeaker", isSpeaker));
    }

    private void sendUpdateHoldBroadcast() {
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(INTENT_FILTER_CALLBACK)
                .putExtra(KEY_CALLBACK, CALLBACKS.UPDATE_HOLD)
                .putExtra("isHold", isHold));
    }

    @Override
    public void notifyIncomingCall(final SipCall call) {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    CallOpParam prm = new CallOpParam();
                    CallSetting opt = prm.getOpt();
                    opt.setAudioCount(1);
                    opt.setVideoCount(0);
                    opt.setFlag(0);
                    if (sipCurrentCall != null) {
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_BUSY_HERE);
                        call.hangup(prm);
                        call.delete();
                    }
                    else {
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_RINGING);
                        call.answer(prm);
                        waitingToCall = false;
                        String remoteUri = call.getInfo().getRemoteUri();
                        if (!TextUtils.isEmpty(remoteUri)) {
                            Pattern pattern = Pattern.compile("\\<sip\\:(.+)\\@.+\\>");
                            Matcher matcher = pattern.matcher(remoteUri);
                            if (matcher.find()) {
                                notificationContentText = remoteNumber = matcher.group(1);
                                Log.d(TAG, "SERVICE: Remote number found: " + remoteNumber);
                            }
                            else {
                                Log.d(TAG, "SERVICE: Remote number not found.");
                            }
                            pattern = Pattern.compile("\\\"?([^\\\"]*)\\\"?");
                            matcher = pattern.matcher(remoteUri);
                            if (matcher.find()) {
                                notificationContentText = remoteName = matcher.group(1);
                                Log.d(TAG, "SERVICE: Remote name found: " + remoteName);
                            }
                            else {
                                Log.d(TAG, "SERVICE: Remote name not found.");
                            }
                        }
                        Logger.logToFile("SERVICE: Ringing...");
                        startRingtone();
                        setCallState(CALLBACKS.RINGING);
                        sipCurrentCall = call;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                isMissedCall = true;
                                startActivity(createCallingActivityIntent());
                            }
                        });
                    }
                } catch (Exception e) {
                    String errMsg = "Error while notifying incoming call: " + e.getMessage();
                    Logger.logToFile(errMsg);
                    setError(errMsg, e);
                    finishService();
                }
            }
        });
    }

    @Override
    public void notifyRegState(final pjsip_status_code code, String reason, final int expiration) {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sipCurrentCall != null) {
                    Logger.logToFile("SERVICE: Registration: Only one call at anytime!");
                    return;
                }
                try {
                    int returnCode = code.swigValue();
                    if (returnCode / 100 == 2) {
                        if (expiration != 0) {
                            if (callDirection == CALL_DIRECTION.OUTGOING) {
                                makeCall();
                            } else if (callDirection == CALL_DIRECTION.INCOMING) {
                                Logger.logToFile("SERVICE: Successfully registered to SIP");
                                waitForIncomingCall();
                            } else {
                                throw new CallingException("Error: Unknown call direction, hanging up call...");
                            }
                        }
                    } else {
                        String errMsg = "SERVICE: Error while SIP registration: Asterisk returned code '" + returnCode + "'";
                        Logger.logToFile(errMsg);
                        setError(errMsg, null);
                        finishService();
                    }
                } catch (CallingException e) {
                    Logger.logToFile(e.getMessage());
                    Logger.e(TAG, e);
                    finishService();
                }
            }
        });
    }

    @Override
    public void notifyCallState(SipCall call) {
        try {
            Logger.logToFile("SERVICE: CALL STATE: " + call.getInfo().getStateText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (sipCurrentCall == null || call.getId() != sipCurrentCall.getId()) {
                // get rid of states from all calls except current call
                return;
            }
            pjsip_inv_state state = call.getInfo().getState();
            if (state == pjsip_inv_state.PJSIP_INV_STATE_CALLING) {
                setCallState(CALLBACKS.CALLING);
            }
            else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONNECTING) {
                setCallState(CALLBACKS.CONNECTING);
            }
            else if (state == pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED) {
                startTime = System.currentTimeMillis();
                setCallState(CALLBACKS.CALL_ESTABLISHED);
            }
            else if (state == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                setCallState(CALLBACKS.CALL_ENDED);
//                mainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (isMissedCall) {
//                            showMissedCall(remoteNumber, remoteName); // uncomment to enable missed call notification
//                        }
//                    }
//                });
                finishService();
            }
        } catch (Exception e) {
            setError("Error while getting info about calling", e);
        }
    }

    @Override
    public void notifyCallMediaState(SipCall call) {
        try {
            Logger.logToFile("SERVICE: MEDIA STATE: " + call.getInfo().getStateText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class GsmStateChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String extraState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(extraState) || TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
                Logger.logToFile("SERVICE: GSM call is coming");
                isGsmIdle = false;
                if (isHold) {
                    return;
                }
                CallingCommands.toggleHold(getApplicationContext());
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
                Logger.logToFile("SERVICE: GSM call ended");
                isGsmIdle = true;
            }
        }
    }

}
