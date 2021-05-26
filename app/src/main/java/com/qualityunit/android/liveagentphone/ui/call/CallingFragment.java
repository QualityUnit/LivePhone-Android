package com.qualityunit.android.liveagentphone.ui.call;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_UPDATE.UPDATE_DURATION;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_UPDATE.UPDATE_EXTENSION_HOLD;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_UPDATE.UPDATE_HOLD;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_UPDATE.UPDATE_MUTE;
import static com.qualityunit.android.liveagentphone.service.CallingService.CALL_UPDATE.UPDATE_SPEAKER;

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
    private ImageButton ibTransfer;
    private CallStateReceiver callStateReceiver;
    private CallUpdateReceiver callUpdateReceiver;
    private TextView tvState;
    private TextView tvDuration;
    private TextView tvRemoteName;
    private ExtensionStateReceiver extensionStateReceiver;
    private LinearLayout llExtension;
    private TextView tvExtensionName;
    private ImageButton ibExtensionDialpad;
    private ImageButton ibExtensionHold;
    private ImageButton ibExtensionHangup;
    private Button bCompleteTransfer;

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
        setText(tvRemoteName, nameToShow(remoteNumber, remoteName));
    }

    private String nameToShow(String remoteNumber, String remoteName) {
        return !TextUtils.isEmpty(remoteName) ? remoteName : (!TextUtils.isEmpty(remoteNumber) ? remoteNumber : getString(R.string.unknown));
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
        ibTransfer = (ImageButton) view.findViewById(R.id.ib_transfer);
        ibTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallingCommands.transferHoldActive(getContext());
                activity.switchFragments(new TransferFragment(), TransferFragment.TAG);
            }
        });
        llExtension = (LinearLayout) view.findViewById(R.id.ll_extension);
        tvExtensionName = (TextView) view.findViewById(R.id.tv_extension_name);
        ibExtensionHangup = (ImageButton) view.findViewById(R.id.ib_extension_hangup);
        ibExtensionHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CallingCommands.transferExtensionHangup(getContext());
            }
        });
        ibExtensionDialpad = (ImageButton) view.findViewById(R.id.ib_extension_dialpad);
        ibExtensionHold = (ImageButton) view.findViewById(R.id.ib_extension_hold);
        ibExtensionHold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CallingCommands.transferToggleHoldExtension(getContext());
            }
        });
        bCompleteTransfer = (Button) view.findViewById(R.id.b_complete_transfer);
        bCompleteTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CallingCommands.transferCompleteTransfer(getContext());
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
        if (extensionStateReceiver == null) {
            extensionStateReceiver = new ExtensionStateReceiver();
            lbm.registerReceiver(extensionStateReceiver, new IntentFilter(CallingService.INTENT_FILTER_EXTENSION_STATE));
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
        if (extensionStateReceiver != null) {
            lbm.unregisterReceiver(extensionStateReceiver);
            extensionStateReceiver = null;
        }
    }

    private class CallUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            for (String key : extras.keySet()) {
                switch (key) {
                    case UPDATE_DURATION:
                        long duration = intent.getLongExtra(key, 0);
                        if (duration > 0) {
                            tvDuration.setVisibility(View.VISIBLE);
                            if (duration < 3600) {
                                setText(tvDuration, String.format("%02d:%02d", duration / 60, duration % 60));
                            } else {
                                setText(tvDuration, String.format("%02d:%02d:%02d", duration / 3600, (duration % 3600) / 60, duration % 60));
                            }
                        }
                        break;
                    case UPDATE_HOLD:
                        boolean isHold = intent.getBooleanExtra(key, false);
                        toggle(ibHold, isHold);
                        setText(tvState, getString(isHold ? R.string.call_hold : R.string.call_state_active));
                        if (isHold) {
                            enable(ibExtensionHold);
                        } else {
                            disable(ibExtensionHold);
                        }
                        break;
                    case UPDATE_EXTENSION_HOLD:
                        boolean isExtensionHold = intent.getBooleanExtra(key, false);
                        toggle(ibExtensionHold, isExtensionHold);
                        if (isExtensionHold) {
                            enable(ibHold);
                        } else {
                            disable(ibHold);
                        }
                        break;
                    case UPDATE_MUTE:
                        toggle(ibMute, intent.getBooleanExtra(key, false));
                        break;
                    case UPDATE_SPEAKER:
                        toggle(ibSpeaker, intent.getBooleanExtra(key, false));
                        break;

                }
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
                    setText(tvRemoteName, nameToShow(intent.getStringExtra("remoteNumber"), intent.getStringExtra("remoteName")));
                    break;
                case CallingService.CALL_STATE.RINGING:
                    setText(tvState, "");
                    setText(tvRemoteName, nameToShow(intent.getStringExtra("remoteNumber"), intent.getStringExtra("remoteName")));
                    break;
                case CallingService.CALL_STATE.ACTIVE:
                    setText(tvState, getString(R.string.call_state_active));
                    setText(tvRemoteName, nameToShow(intent.getStringExtra("remoteNumber"), intent.getStringExtra("remoteName")));
                    show(ibHold);
                    show(ibDialpad);
                    show(ibTransfer);
                    disable(ibExtensionHold);
                    CallingCommands.getCallUpdates(getContext());
                    break;
                case CallingService.CALL_STATE.HOLD:
                    setText(tvState, getString(R.string.call_hold));
                    show(ibHold);
                    show(ibDialpad);
                    show(ibTransfer);
                    enable(ibExtensionHold);
                    CallingCommands.getCallUpdates(getContext());
                    break;
                case CallingService.CALL_STATE.DISCONNECTED:
                    setText(tvState, getString(R.string.call_state_call_ended));
                    hide(ibMute);
                    hide(ibSpeaker);
                    hide(ibDialpad);
                    hide(ibHold);
                    hide(ibTransfer);
                    break;
                case CallingService.CALL_STATE.ERROR:
                    String error = intent.getStringExtra("error");
                    setText(tvState, error);
                    Toast.makeText(activity, error, Toast.LENGTH_SHORT).show();
            }

        }

    }

    private class ExtensionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            final String extensionState = intent.getStringExtra("extensionState");
            switch (extensionState) {
                case CallingService.EXTENSION_STATE.DISCONNECTED:
                    hide(ibExtensionHold);
                    hide(ibExtensionDialpad);
                    show(ibTransfer);
                    llExtension.setVisibility(View.GONE);
                    break;
                case CallingService.EXTENSION_STATE.ACTIVE:
                    show(ibExtensionHold);
                    show(ibExtensionDialpad);
                    hide(ibTransfer);
                    disable(ibHold);
                    bCompleteTransfer.setVisibility(View.VISIBLE);
                case CallingService.EXTENSION_STATE.DIALING:
                    llExtension.setVisibility(View.VISIBLE);
                    setText(tvExtensionName, intent.getStringExtra("extensionName"));
                    hide(ibTransfer);
                    break;
                case CallingService.EXTENSION_STATE.HOLD:
                    show(ibExtensionHold);
                    show(ibExtensionDialpad);
                    enable(ibHold);
                    llExtension.setVisibility(View.VISIBLE);
                    break;
                case CallingService.EXTENSION_STATE.ERROR:
                    llExtension.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void toggle(ImageButton imageButton, boolean toggle) {
        if (toggle) {
            imageButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.bg_calling_toggle));
        } else {
            imageButton.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void show(ImageButton imageButton) {
        ((View)imageButton.getParent()).setVisibility(View.VISIBLE);
    }

    private void hide(ImageButton imageButton) {
        ((View)imageButton.getParent()).setVisibility(View.GONE);
    }

    private void enable(ImageButton imageButton) {
        imageButton.setEnabled(true);
    }

    private void disable(ImageButton imageButton) {
        imageButton.setEnabled(false);
    }

}
