package com.qualityunit.android.liveagentphone.ui.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;

import net.frakbot.glowpadbackport.GlowPadView;

/**
 * A placeholder fragment containing a simple view.
 * PORTRAIT ONLY!!!
 */
public class CallingFragment extends BaseFragment<CallingActivity> {

    public static final String TAG = CallingFragment.class.getSimpleName();
    private ImageButton ibSpeaker;
    private ImageButton ibMute;
    private ImageButton ibDialpad;
    private ImageButton ibHold;
    private String remoteNumber;
    private String remoteName;
    private SipBroadcastReceiver sipBroadcastReceiver = new SipBroadcastReceiver();
    private boolean isSipEventsReceiverRegistered;
    private TextView tvState;
    private LinearLayout llCallState;
    private Chronometer chTimer;
    private GlowPadView glowPad;
    private TextView tvRemoteName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.calling_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = getActivity().getIntent();
        remoteNumber = intent.getStringExtra("remoteNumber");
        remoteName = intent.getStringExtra("remoteName");
        String nameToShow = TextUtils.isEmpty(remoteName) ? remoteNumber : remoteName;
        nameToShow = TextUtils.isEmpty(nameToShow) ? getString(R.string.unknown) : nameToShow;
        setText(tvRemoteName, nameToShow);
        registerSipEventsReceiver();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvRemoteName = (TextView) view.findViewById(R.id.tv_remote_name);
        tvState = (TextView) view.findViewById(R.id.tv_state);
        ibSpeaker = (ImageButton) view.findViewById(R.id.ib_speaker);
        ibSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallingCommands.toggleSpeaker(getContext());
            }
        });
        ((View)ibSpeaker.getParent()).setVisibility(View.VISIBLE);
        ibMute = (ImageButton) view.findViewById(R.id.ib_mute);
        ibMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallingCommands.toggleMute(getContext());
            }
        });
        ((View)ibMute.getParent()).setVisibility(View.VISIBLE);
        ibDialpad = (ImageButton) view.findViewById(R.id.ib_dialpad);
        ibDialpad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.switchFragments(new CallingDtmfFragment(), CallingDtmfFragment.TAG);
            }
        });
        ibHold = (ImageButton) view.findViewById(R.id.ib_hold);
        ibHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallingCommands.toggleHold(getContext());
            }
        });
        chTimer = (Chronometer) view.findViewById(R.id.ch_timer);
        chTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {

            @Override
            public void onChronometerTick(Chronometer arg) {
                arg.setText(Tools.millisToHhMmSs(System.currentTimeMillis() - arg.getBase()));
            }

        });
        glowPad = (GlowPadView) view.findViewById(R.id.glowPad);
        glowPad.setPointsMultiplier(8);
        glowPad.setOnTriggerListener(new GlowPadView.OnTriggerListener() {

            @Override
            public void onGrabbed(View v, int handle) {
            }

            @Override
            public void onReleased(View v, int handle) {
            }

            @Override
            public void onTrigger(View v, int target) {
                if (target == 0) {
                    // answer
                    Logger.logToFile("Call UI: Answer call triggered.");
                    glowPad.setVisibility(View.GONE);
                    llCallState.setVisibility(View.VISIBLE);
                    activity.getFabHangupCall().setVisibility(View.VISIBLE);
                    activity.receiveCall();
                } else if (target == 2) {
                    // decline
                    Logger.logToFile("Call UI: Decline call triggered.");
                    glowPad.setVisibility(View.GONE);
                    llCallState.setVisibility(View.VISIBLE);
                    activity.declineCall();
                }
            }

            @Override
            public void onGrabbedStateChange(View v, int handle) {
            }

            @Override
            public void onFinishFinalAnimation() {
            }

        });
        llCallState = (LinearLayout) view.findViewById(R.id.ll_callState);
    }

    @Override
    public void onStart() {
        super.onStart();
        registerSipEventsReceiver();
        CallingCommands.updateState(getContext());
    }

    @Override
    public void onStop() {
        chTimer.stop();
        unregisterSipEventsReceiver();
        super.onStop();
    }

    private void setText(TextView textView, String text) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(text);
    }

    private void registerSipEventsReceiver() {
        if (!isSipEventsReceiverRegistered) {
            isSipEventsReceiverRegistered = true;
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(sipBroadcastReceiver, new IntentFilter(CallingService.INTENT_FILTER_CALLBACK));
        }
    }

    private void unregisterSipEventsReceiver() {
        if (isSipEventsReceiverRegistered) {
            isSipEventsReceiverRegistered = false;
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(sipBroadcastReceiver);
        }
    }

    private class SipBroadcastReceiver extends BroadcastReceiver {

        private final String TAG = CallingFragment.class.getSimpleName() + "." + SipBroadcastReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                Logger.e(TAG, "Intent cannot be null in 'onReceive' method!");
                return;
            }
            final String callback = intent.getStringExtra(CallingService.KEY_CALLBACK);
            switch (callback) {
                case CallingService.CALLBACKS.INITIALIZING:
                    setText(tvState, getString(R.string.call_state_initializing));
                    break;
                case CallingService.CALLBACKS.REGISTERING_SIP_USER:
                    setText(tvState, getString(R.string.call_state_registering));
                    break;
                case CallingService.CALLBACKS.RINGING:
                    llCallState.setVisibility(View.GONE);
                    activity.getFabHangupCall().setVisibility(View.GONE);
                    glowPad.setVisibility(View.VISIBLE);
                    break;
                case CallingService.CALLBACKS.CALLING:
                    setText(tvState, getString(R.string.call_state_calling));
                    break;
                case CallingService.CALLBACKS.CONNECTING:
                    setText(tvState, getString(R.string.call_state_connecting));
                    break;
                case CallingService.CALLBACKS.CALL_ESTABLISHED:
                    setText(tvState, getString(R.string.call_state_established));
                    activity.getFabHangupCall().setVisibility(View.VISIBLE);
                    ((View)ibHold.getParent()).setVisibility(View.VISIBLE);
                    ((View)ibDialpad.getParent()).setVisibility(View.VISIBLE);
                    CallingCommands.updateAll(getContext());
                    break;
                case CallingService.CALLBACKS.UPDATE_DURATION:
                    long startTime = intent.getLongExtra("startTime", -1);
                    if (startTime > 0) {
                        chTimer.setBase(startTime);
                        chTimer.start();
                        chTimer.setVisibility(View.VISIBLE);
                    } else {
                        chTimer.setVisibility(View.INVISIBLE);
                    }
                    break;
                case CallingService.CALLBACKS.UPDATE_MUTE:
                    toggleImageButton(ibMute, intent.getBooleanExtra("isMute", false));
                    break;
                case CallingService.CALLBACKS.UPDATE_SPEAKER:
                    toggleImageButton(ibSpeaker, intent.getBooleanExtra("isSpeaker", false));
                    break;
                case CallingService.CALLBACKS.UPDATE_HOLD:
                    boolean isHolded = intent.getBooleanExtra("isHold", false);
                    toggleImageButton(ibHold, isHolded);
                    if (isHolded) {
                        setText(tvState, getString(R.string.call_hold));
                    } else {
                        setText(tvState, getString(R.string.call_state_established));
                    }
                    break;
                case CallingService.CALLBACKS.HANGING_UP_CALL:
                case CallingService.CALLBACKS.CALL_ENDED:
                    if (chTimer != null) {
                        chTimer.stop();
                    }
                    ((View)ibMute.getParent()).setVisibility(View.GONE);
                    ((View)ibSpeaker.getParent()).setVisibility(View.GONE);
                    ((View)ibDialpad.getParent()).setVisibility(View.GONE);
                    ((View)ibHold.getParent()).setVisibility(View.GONE);
                    llCallState.setVisibility(View.VISIBLE);
                    activity.getFabHangupCall().setVisibility(View.VISIBLE);
                    glowPad.setVisibility(View.GONE);
                    setText(tvState, getString(R.string.call_state_call_ended));
                    break;
                case CallingService.CALLBACKS.ERROR:
                    setText(tvState, callback + "\n" + intent.getStringExtra("errorMessage"));
                    Toast.makeText(activity, intent.getStringExtra("errorMessage"), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Logger.e(TAG, "Unknown event type in 'onReceive' method!");
            }

        }

        private void toggleImageButton(ImageButton imageButton, boolean toggle) {
            if (toggle) {
                imageButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_calling_toggle));
            } else {
                imageButton.setBackgroundColor(Color.TRANSPARENT);
            }
        }

    }

}
