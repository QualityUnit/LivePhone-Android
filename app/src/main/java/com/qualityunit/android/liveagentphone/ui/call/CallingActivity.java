package com.qualityunit.android.liveagentphone.ui.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.common.ToolbarActivity;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.Timer;
import java.util.TimerTask;

import static com.qualityunit.android.liveagentphone.service.CallingService.CALLBACKS.CALL_ENDED;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALLBACKS.CALL_ESTABLISHED;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALLBACKS.HANGING_UP_CALL;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALLBACKS.RINGING;

/**
 * Created by rasto on 18.10.16.
 */

public class CallingActivity extends ToolbarActivity {

    private FloatingActionButton fabHangupCall;
    private boolean isRinging;
    private boolean isCallEstablished;
    private SipBroadcastReceiver sipBroadcastReceiver;
    private Timer finishTimer;

    @Override
    protected boolean allwaysShowHomeAsUp() {
        return false;
    }

    @Override
    protected int getContentViewRes() {
        return R.layout.calling_activity;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sipBroadcastReceiver == null) {
            sipBroadcastReceiver = new SipBroadcastReceiver();
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(sipBroadcastReceiver, new IntentFilter(CallingService.INTENT_FILTER_CALLBACK));
        }
        if (savedInstanceState == null) {
            addFragment(new CallingFragment(), CallingFragment.TAG);
        }
        fabHangupCall = findViewById(R.id.fab_hangCall);
        if (fabHangupCall != null) {
            fabHangupCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    declineCall();
                }
            });
        }
        Logger.logToFile(getApplicationContext() ,"Info: UI is showed");
    }

    @Override
    protected void onDestroy() {
        if (sipBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(sipBroadcastReceiver);
            sipBroadcastReceiver = null;
        }
        cancelDelayedFinish();
        super.onDestroy();
    }

    @Override
    protected void beforeSetContentView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.beforeSetContentView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isRinging && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            CallingCommands.silenceRinging(getApplicationContext());
            isRinging = false;
            return true;
        }
        if (isCallEstablished) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                CallingCommands.adjustIncallVolume(getApplicationContext(), false);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                CallingCommands.adjustIncallVolume(getApplicationContext(), true);
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void declineCall() {
        if (finishTimer != null) {
            cancelDelayedFinish();
            finish();
            return;
        }
        CallingCommands.hangupCall(CallingActivity.this);
        finishDelayed();
    }

    public void answerCall() {
        CallingCommands.answerCall(CallingActivity.this);
    }

    public FloatingActionButton getFabHangupCall() {
        return fabHangupCall;
    }

    private void finishDelayed() {
        if (finishTimer != null) {
            return;
        }
        finishTimer = new Timer();
        finishTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);
    }

    private void cancelDelayedFinish() {
        if (finishTimer != null) {
            finishTimer.cancel();
            finishTimer = null;
        }
    }

    private class SipBroadcastReceiver extends BroadcastReceiver {

        private final String TAG = CallingActivity.class.getSimpleName() + "." + CallingActivity.SipBroadcastReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Logger.e(TAG, "Intent cannot be null in 'onReceive' method!");
                return;
            }
            final String callback = intent.getStringExtra("callback");
            switch (callback) {
                case RINGING:
                    isRinging = true;
                    break;
                case CALL_ESTABLISHED:
                    isRinging = false;
                    isCallEstablished = true;
                    break;
                case HANGING_UP_CALL:
                    isCallEstablished = false;
                    break;
                case CALL_ENDED:
                    isCallEstablished = false;
                    popToFragment(CallingFragment.TAG);
                    finishDelayed();
                    break;
                default:
                    Logger.e(TAG, "Unknown event type in 'onReceive' method!");
            }

        }

    }

}
