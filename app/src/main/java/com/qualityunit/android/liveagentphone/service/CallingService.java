package com.qualityunit.android.liveagentphone.service;

import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
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
import com.qualityunit.android.sip.SipBuddy;
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
    private static final long CALL_ENDED_DELAY_MILLIS = 2000;
    public static final String KEY_COMMAND = "KEY_COMMAND";
    public static final String KEY_CALLBACK = "KEY_CALLBACK";
    public static final String KEY_CALL_DIRECTION = "KEY_CALL_DIRECTION";
    public static final String INTENT_FILTER_CALLBACK = "com.qualityunit.android.liveagentphone.SIPEVENTS";
    public static final class COMMANDS {
        public static final int MAKE_CALL = 1;
        public static final int INCOMING_CALL = 2;
        public static final int RECEIVE_CALL = 3;
        public static final int HANGUP_CALL = 4;
        public static final int UPDATE_DURATION = 5;
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
    public static boolean isRunning;
    private boolean isMute;
    private boolean isSpeaker;
    private boolean isHold;
    private boolean isMissedCall;
    private long startTime = System.currentTimeMillis();
    private String lastState = "";
    private String notificationContentText;
    private long notificationWhen = System.currentTimeMillis();
    private HandlerThread workerThread;
    private Handler workerHandler;
    private final Handler mainHandler = new Handler();
    private volatile boolean hangingUpCall;
    private volatile boolean waitingToCall;
    private volatile boolean finishingService;
    private String sipHost;
    private String callingPrefix;
    private String calleeNumber;
    private String callerNumber;
    private String sipUser;
    private String sipPassword;
    private int callDirection;
    private String callingAsNumber;
    private String callingAsName;
    private static PowerManager.WakeLock wakeLock;
    private static PowerManager.WakeLock proximityWakeLock;
    private MediaPlayer ringTone;
    private Vibrator vibrator;
    // pjsua objects
    private SipCore sipCore;
    private SipCall sipCurrentCall;
    private SipAccount sipAccount;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "#### SERVICE onCreate");
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            proximityWakeLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getClass().getSimpleName());
            proximityWakeLock.acquire();
        }
        wakeLock.acquire();
        isRunning = true;
        startWorkerThread();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        Log.d(TAG, "#### SERVICE onStartCommand (" + startId + ") with intent (" + (intent != null) + ")");
        try {
            int command = intent.getIntExtra(KEY_COMMAND, 0);
            switch (command) {
                case COMMANDS.MAKE_CALL:
                    callingAsNumber = intent.getStringExtra("callingAsNumber");
                    callingAsName = intent.getStringExtra("callingAsName");
                    calleeNumber = intent.getStringExtra("calleeNumber");
                    callingPrefix = intent.getStringExtra("callingPrefix");
                    callDirection = intent.getIntExtra(KEY_CALL_DIRECTION, 0);
                    notificationContentText = calleeNumber;
                    startActivity(createCallingActivityIntent());
                    initAndRegister();
                    break;
                case COMMANDS.INCOMING_CALL:
                    callDirection = intent.getIntExtra(KEY_CALL_DIRECTION, 0);
                    initAndRegister();
                    break;
                case COMMANDS.RECEIVE_CALL:
                    receiveCall();
                    break;
                case COMMANDS.SEND_DTMF:
                    sendDtfm(intent.getStringExtra("character"));
                    break;
                case COMMANDS.HANGUP_CALL:
                    hangupCallAndFinishService();
                    break;
                case COMMANDS.ADJUST_INCALL_VOLUME:
                    boolean increase = intent.getBooleanExtra("increase", true);
                    adjustIncallVolume(increase);
                    break;
                case COMMANDS.SILENCE_RINGING:
                    stopRingtone();
                    break;
                case COMMANDS.UPDATE_STATE:
                    setCallState(lastState);
                    break;
                case COMMANDS.TOGGLE_MUTE:
                    enableMute(!isMute);
                    break;
                case COMMANDS.TOGGLE_SPEAKER:
                    enableSpeaker(!isSpeaker);
                    break;
                case COMMANDS.TOGGLE_HOLD:
                    callHold(!isHold);
                    break;
                case COMMANDS.UPDATE_ALL:
                    sendUpdateDurationBroadcast();
                    sendUpdateMuteBroadcast();
                    sendUpdateSpeakerBroadcast();
                    sendUpdateHoldBroadcast();
                    break;
                default:
                    throw new CallingException("Service started with unknown command:" + command);
            }
            return START_NOT_STICKY;
        } catch (Exception e) {
            setError("#### Error while starting " + TAG, e);
            return START_REDELIVER_INTENT;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "#### SERVICE onDestroy");
        stopRingtone();
        stopWorkerThread();
        isRunning = false;
        super.onDestroy();
        if (proximityWakeLock != null) {
            proximityWakeLock.release();
            proximityWakeLock = null;
        }
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
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
                .putExtra("callingAsNumber", callingAsNumber)
                .putExtra("callingAsName", callingAsName)
                .putExtra("calleeNumber", calleeNumber)
                .putExtra("callerNumber", callerNumber)
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
                        titleText = getString(R.string.call_holded);
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
                Notification notification = new Notification.Builder(getApplicationContext())
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
            Log.d(TAG, "#### Worker handler is null");
            if (workerThread == null) {
                Log.d(TAG, "#### Worker thread is null");
                workerThread = new HandlerThread(SIP_THREAD_NAME, Thread.MAX_PRIORITY);
                workerThread.start();
                Log.d(TAG, "#### Worker thread started");
            }
            workerHandler = new Handler(workerThread.getLooper());
            Log.d(TAG, "#### Worker handler initiated");
        }
    }

    private void stopWorkerThread() {
        if (workerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                workerThread.quitSafely();
            }
            else {
                workerThread.quit();
            }
            Log.d(TAG, "#### Worker thread and handler stopped");
            workerThread = null;
            workerHandler = null;
        }
    }

    private void initAndRegister() {
        try {
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
                            Log.d(TAG, "#### Lib initialized successfully");
                        } catch (final Exception e) {
                            setError("Error while initializing sip lib", e);
                            finishService();
                        }
                    }

                    // register sip user
                    if (hangingUpCall) {
                        Log.d(TAG, "#### Calling terminated before registering user");
                        return;
                    }
                    if (sipAccount == null) {
                        setCallState(CALLBACKS.REGISTERING_SIP_USER);
                        AccountConfig sipAccountConfig = createAccountConfig();
                        try {
                            sipAccount = new SipAccount(sipAccountConfig);
                            sipAccount.create(sipAccountConfig, true);
                            Log.d(TAG, "#### Account registration fired");
                        } catch (final Exception e) {
                            setError("Error while creating and registering sipAccount", e);
                            finishService();
                        }
                    }
                    // wait for registration callback and continue from listener
                }
            });
        } catch (final Exception e) {
            setError("Error while initializing sip lib" + e.getMessage(), e);
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
                if (hangingUpCall) {
                    Log.d(TAG, "#### Calling terminated before making call");
                    return;
                }
                if (sipCurrentCall != null) {
                    Log.d(TAG, "Only one call at anytime!");
                    return;
                }
                setCallState(CALLBACKS.STARTING_CALL);
                SipCall call = new SipCall(sipAccount, -1);
                CallOpParam prm = new CallOpParam();
                CallSetting opt = prm.getOpt();
                opt.setAudioCount(1);
                opt.setVideoCount(0);
                opt.setFlag(0);
                try {
                    String cleanCalleeNumber = Tools.Sip.cleanNumber(calleeNumber);
                    String sipCalleeUri = Tools.Sip.createCalleeUri(callingPrefix, cleanCalleeNumber, sipHost);
                    call.makeCall(sipCalleeUri, prm);
                    sipCurrentCall = call;
                    Log.d(TAG, "#### Call to '" + sipCalleeUri + "' started successfully");
                } catch (final Exception e) {
                    call.delete();
                    setError("Error while making SIP call:" + e.getMessage(), e);
                    finishService();
                }
            }
        });
    }

    private void waitForIncomingCall() {
        setCallState(CALLBACKS.WAITING_TO_CALL);
        waitingToCall = true;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (waitingToCall) {
                    Log.d(TAG, "#### Hanging up call because waiting reached " + WAITING_TO_CALL_MILLIS + " seconds");
                    finishService();
                }
            }
        }, WAITING_TO_CALL_MILLIS);
    }

    private void receiveCall() {
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (hangingUpCall) {
                    Log.d(TAG, "#### Calling terminated before making call");
                    return;
                }
                if (sipCurrentCall == null) {
                    Log.d(TAG, "#### No ringing call now");
                    return;
                }
                try {
                    stopRingtone();
                    CallOpParam prm = new CallOpParam();
                    prm.setStatusCode(pjsip_status_code.PJSIP_SC_OK);
                    sipCurrentCall.answer(prm);
                    Log.d(TAG, "#### Call answered");
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            isMissedCall = false;
                        }
                    });
                    setCallState(CALLBACKS.CALL_ESTABLISHED);
                } catch (Exception e) {
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
        if (hangingUpCall) {
            Log.d(TAG, "#### Hanging up already started");
            return;
        }
        stopRingtone();
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                hangingUpCall = true;
                setCallState(CALLBACKS.HANGING_UP_CALL);
                if (sipCurrentCall != null) {
                    try {
                        CallOpParam prm = new CallOpParam();
                        prm.setStatusCode(pjsip_status_code.PJSIP_SC_DECLINE);
                        sipCurrentCall.hangup(prm);
                        Log.d(TAG, "#### Call successfully hanged up");
                    } catch (final Exception e) {
                        setError("Error while hanging up call", e);
                        finishService();
                    }
                } else {
                    finishService();
                }

                // finishing of service is called from notifyCallState
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
                        Log.d(TAG, "#### Trying to hold...");
                        sipCurrentCall.setHold(param);
                    } else {
                        Log.d(TAG, "#### Trying to unhold...");
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
                    setError("Error while trying to hold a call.", null);
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
        if (finishingService) {
            Log.d(TAG, "#### Finnishing already started");
            return;
        }
        workerHandler.post(new Runnable() {
            @Override
            public void run() {
                finishingService = true;
                if (sipCore != null) {
                    Log.d(TAG, "#### Deinitializing library");
                    sipCore.deinit();
                }
                sipCore = null;
                sipCurrentCall = null;
                sipAccount = null;
                stopSelfDelayed();
            }
        });
    }

    private void stopSelfDelayed() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "#### Stopping service with delay " + CALL_ENDED_DELAY_MILLIS + " millis");
                stopSelf();
            }
        }, CALL_ENDED_DELAY_MILLIS);
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

    private void showMissedCall(String remoteNumber) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
        String missedCallStr = getString(R.string.missed_call);
        notificationBuilder.setContentTitle(missedCallStr);
        notificationBuilder.setContentText(remoteNumber);
        notificationBuilder.setTicker(missedCallStr);
        notificationBuilder.setSmallIcon(R.drawable.ic_phone_icon);
//        notificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notificationBuilder.setAutoCancel(true);

        // Big view
//        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
//        String[] lines = { "line 1","line 2" };
//        inboxStyle.setBigContentTitle("");
//        for (String line : lines) {
//            inboxStyle.addLine(line);
//        }
//        notificationBuilder.setStyle(inboxStyle);

        Intent notificationIntent = new Intent(getApplicationContext(), DialerActivity.class);
        notificationIntent.putExtra("number", remoteNumber);
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
        Log.d(TAG, "#### CALL STATE: " + callback);
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
                        String callerId = call.getInfo().getRemoteUri();
                        Pattern pattern = Pattern.compile("\\<sip\\:(.+)\\@.+\\>");
                        Matcher matcher = pattern.matcher(callerId);
                        if (!TextUtils.isEmpty(callerId)) {
                            boolean found = matcher.find();
                            if (found) {
                                notificationContentText = callerNumber = matcher.group(1);;
                                Logger.e(TAG, "#### Caller number found: " + callerNumber);
                            }
                            else {
                                Logger.e(TAG, "#### Caller number not found.");
                            }
                        }
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
                    setError("Error while notifying incoming call", e);
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
                    Log.d(TAG, "Only one call at anytime!"); // skip rest of func while re-registering
                    return;
                }
                try {
                    int returnCode = code.swigValue();
                    if (returnCode / 100 == 2) {
                        if (expiration != 0) {
                            if (callDirection == CALL_DIRECTION.OUTGOING) {
                                makeCall();
                            } else if (callDirection == CALL_DIRECTION.INCOMING) {
                                waitForIncomingCall();
                            } else {
                                throw new CallingException("#### Unknown call direction, hanging up call...");
                            }
                        }
                    } else {
                        String errMsg = "Registration: Asterisk returned code '" + returnCode + "'.";
                        Logger.e(TAG, errMsg);
                        setError(errMsg, null);
                    }
                } catch (CallingException e) {
                    Logger.e(TAG, e);
                    finishService();
                }
            }
        });
    }

    @Override
    public void notifyCallState(SipCall call) {
        try {
            Log.d(TAG, "#### notifyCallState: " + call.getInfo().getStateText());
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
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isMissedCall) {
                            showMissedCall(callerNumber);
                        }
                    }
                });
                finishService();
            }
        } catch (Exception e) {
            setError("Error while getting info about calling", e);
        }
    }

    @Override
    public void notifyCallMediaState(SipCall call) {
        try {
            Log.d(TAG, "#### notifyCallMediaState: " + call.getInfo().getStateText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void notifyBuddyState(SipBuddy buddy) {
        // not in use
    }

}
