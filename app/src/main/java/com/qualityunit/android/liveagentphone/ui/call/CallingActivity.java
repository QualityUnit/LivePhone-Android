package com.qualityunit.android.liveagentphone.ui.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.common.ToolbarActivity;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_STATE.ACTIVE;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_STATE.DISCONNECTED;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_STATE.RINGING;

/**
 * Created by rasto on 18.10.16.
 */

public class CallingActivity extends ToolbarActivity {

    private FloatingActionButton fabHangup;
    private FloatingActionButton fabAnswer;
    private FloatingActionButton fabReject;
    private CallStateReceiver callStateReceiver;
    private Timer finishTimer;
    private String callState;

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
        if (callStateReceiver == null) {
            callStateReceiver = new CallStateReceiver();
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(callStateReceiver, new IntentFilter(CallingService.INTENT_FILTER_CALL_STATE));
        }
        if (savedInstanceState == null) {
            addFragment(new CallingFragment(), CallingFragment.TAG);
        }
        fabHangup = findViewById(R.id.fab_hangup);
        fabAnswer = findViewById(R.id.fab_answer);
        fabReject = findViewById(R.id.fab_reject);
        fabHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hangupCall();
            }
        });
        fabAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                answerCall();
            }
        });
        fabReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRingingButtons(false);
                hangupCall();
            }
        });
        if (getIntent().getBooleanExtra("answer", false)) {
            answerCall();
        }
        Logger.logToFile(getApplicationContext() ,"ACTIVITY: UI is showed");
    }

    @Override
    protected void onDestroy() {
        if (callStateReceiver != null) {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(callStateReceiver);
            callStateReceiver = null;
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
        if (RINGING.equals(callState) && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)){
            CallingCommands.silenceRinging(getApplicationContext());
            return true;
        }
        if (ACTIVE.equals(callState)) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                CallingCommands.adjustIncallVolume(getApplicationContext(), false);
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                CallingCommands.adjustIncallVolume(getApplicationContext(), true);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void hangupCall() {
        if (finishTimer != null) {
            // already hung up, just close the UI on second tap
            fabHangup.setOnClickListener(null);
            cancelDelayedFinish();
            finish();
            return;
        }
        CallingCommands.hangupCall(CallingActivity.this);
        finishDelayed();
    }

    public void answerCall() {
        showRingingButtons(false);
        CallingCommands.answerCall(CallingActivity.this);
    }

    private void showRingingButtons(boolean show) {
        if (show) {
            fabAnswer.show();
            fabReject.show();
            fabHangup.hide();
        } else {
            fabAnswer.hide();
            fabReject.hide();
            fabHangup.show();
        }
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

    private class CallStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            callState = intent.getStringExtra("callState");
            showRingingButtons(RINGING.equals(callState));
            if (DISCONNECTED.equals(callState)) {
                popToFragment(CallingFragment.TAG);
                finishDelayed();
            }
        }

    }

}
