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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;

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
    private CallStateReceiver callStateReceiver;
    private CallUpdateReceiver callUpdateReceiver;
    private TextView tvState;
    private TextView tvDuration;
    private TextView tvRemoteName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.calling_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Intent intent = getActivity().getIntent();
        String remoteNumber = intent.getStringExtra("remoteNumber");
        String remoteName = intent.getStringExtra("remoteName");
        String nameToShow = !TextUtils.isEmpty(remoteName) ? remoteName : !TextUtils.isEmpty(remoteNumber) ? remoteNumber : getString(R.string.unknown);
        setText(tvRemoteName, nameToShow);
    }

    @Override
    public void onResume() {
        super.onResume();
        CallingCommands.getCallState(getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvRemoteName = (TextView) view.findViewById(R.id.tv_remote_name);
        tvState = (TextView) view.findViewById(R.id.tv_state);
        tvDuration = (TextView) view.findViewById(R.id.tv_duration);
        tvDuration.setVisibility(View.INVISIBLE);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        registerReceivers();
    }

    @Override
    public void onStop() {
        unregisterReceivers();
        super.onStop();
    }

    private void setText(TextView textView, String text) {
        textView.setVisibility(View.VISIBLE);
        textView.setText(text);
    }

    private void registerReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
        if (callStateReceiver == null) {
            callStateReceiver = new CallStateReceiver();
            lbm.registerReceiver(callStateReceiver, new IntentFilter(CallingService.INTENT_FILTER_CALL_STATE));
        }
        if (callUpdateReceiver == null) {
            callUpdateReceiver = new CallUpdateReceiver();
            lbm.registerReceiver(callUpdateReceiver, new IntentFilter(CallingService.INTENT_FILTER_CALL_UPDATE));
        }
    }

    private void unregisterReceivers() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
        if (callStateReceiver != null) {
            lbm.unregisterReceiver(callStateReceiver);
            callStateReceiver = null;
        }
        if (callUpdateReceiver != null) {
            lbm.unregisterReceiver(callUpdateReceiver);
            callUpdateReceiver = null;
        }
    }

    private class CallUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            for (String key : extras.keySet()) {
                switch (key) {
                    case CallingService.CALL_UPDATE.UPDATE_DURATION:
                        long duration = intent.getLongExtra(CallingService.CALL_UPDATE.UPDATE_DURATION, 0);
                        if (duration > 0) {
                            tvDuration.setVisibility(View.VISIBLE);
                            if (duration < 3600) {
                                setText(tvDuration, String.format("%02d:%02d", duration / 60, duration % 60));
                            } else {
                                setText(tvDuration, String.format("%02d:%02d:%02d", duration / 3600, (duration % 3600) / 60, duration % 60));
                            }
                        }
                        break;
                    case CallingService.CALL_UPDATE.UPDATE_HOLD:
                        boolean isHold = intent.getBooleanExtra(CallingService.CALL_UPDATE.UPDATE_HOLD, false);
                        toggleImageButton(ibHold, isHold);
                        setText(tvState, getString(isHold ? R.string.call_hold : R.string.call_state_active));
                        break;
                    case CallingService.CALL_UPDATE.UPDATE_MUTE:
                        toggleImageButton(ibMute, intent.getBooleanExtra(CallingService.CALL_UPDATE.UPDATE_MUTE, false));
                        break;
                    case CallingService.CALL_UPDATE.UPDATE_SPEAKER:
                        toggleImageButton(ibSpeaker, intent.getBooleanExtra(CallingService.CALL_UPDATE.UPDATE_SPEAKER, false));
                        break;

                }
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

    private class CallStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            final String callback = intent.getStringExtra("callState");
            switch (callback) {
                case CallingService.CALL_STATE.INITIALIZING_SIP_LIBRARY:
                    setText(tvState, getString(R.string.call_state_initializing));
                    break;
                case CallingService.CALL_STATE.REGISTERING_SIP_USER:
                    setText(tvState, getString(R.string.call_state_registering));
                    break;
                case CallingService.CALL_STATE.DIALING:
                    setText(tvState, getString(R.string.call_state_dialing));
                    break;
                case CallingService.CALL_STATE.RINGING:
                    setText(tvState, "");
                    break;
                case CallingService.CALL_STATE.CONNECTING:
                    setText(tvState, getString(R.string.call_state_connecting));
                    break;
                case CallingService.CALL_STATE.ACTIVE:
                    setText(tvState, getString(R.string.call_state_active));
                    ((View)ibHold.getParent()).setVisibility(View.VISIBLE);
                    ((View)ibDialpad.getParent()).setVisibility(View.VISIBLE);
                    CallingCommands.getCallUpdates(getContext());
                    break;
                case CallingService.CALL_STATE.DISCONNECTED:
                    setText(tvState, getString(R.string.call_state_call_ended));
                    ((View)ibMute.getParent()).setVisibility(View.GONE);
                    ((View)ibSpeaker.getParent()).setVisibility(View.GONE);
                    ((View)ibDialpad.getParent()).setVisibility(View.GONE);
                    ((View)ibHold.getParent()).setVisibility(View.GONE);
                    break;
                case CallingService.CALL_STATE.ERROR:
                    String error = intent.getStringExtra("error");
                    setText(tvState, error);
                    Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
                    break;
            }

        }

    }

}
