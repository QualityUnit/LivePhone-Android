package com.qualityunit.android.voice;

import android.util.Log;

import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.CallSetting;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsua_call_media_status;

import static org.pjsip.pjsua2.pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED;
import static org.pjsip.pjsua2.pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED;

public class VoiceCall extends Call {

    private static final String TAG = VoiceCall.class.getSimpleName();
    private VoiceCore voiceCore;
    private Callbacks callbacks;

    public VoiceCall(VoiceAccount acc, int call_id, VoiceCore voiceCore, Callbacks callbacks) {
        super(acc, call_id);
        this.voiceCore = voiceCore;
        this.callbacks = callbacks;
    }

    public static CallOpParam createDefaultParams() {
        CallOpParam prm = new CallOpParam();
        CallSetting opt = prm.getOpt();
        opt.setAudioCount(1);
        opt.setVideoCount(0);
        opt.setFlag(0);
        return prm;
    }

    @Override
    public void onCallState(OnCallStateParam prm) {
        try {
            CallInfo ci = getInfo();
            pjsip_inv_state callState = ci.getState();
            Log.d(TAG, "#### onCallState: " + callState.toString() + " ID=" + ci.getId());
            if (callState == PJSIP_INV_STATE_DISCONNECTED) {
                callbacks.onDisconnect();
//                this.delete();
            } else if (callState == PJSIP_INV_STATE_CONFIRMED) {
                callbacks.onAnswerCall();
            }
        } catch (Exception e) {}
    }

    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        CallInfo ci;
        try {
            ci = getInfo();
            Log.d(TAG, "#### onCallMediaState: ID=" + ci.getId());
        } catch (Exception e) {
            return;
        }
        CallMediaInfoVector cmiv = ci.getMedia();
        for (int i = 0; i < cmiv.size(); i++) {
            CallMediaInfo cmi = cmiv.get(i);
            if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                    (cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE || cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD)) {

                // unfortunately, on Java too, the returned Media cannot be downcasted to AudioMedia
                Media m = getMedia(i);
                AudioMedia am = AudioMedia.typecastFromMedia(m);

                // connect ports
                try {
                    Log.d(TAG, "#### onCallMediaState (connect ports): " + ci.toString());
                    voiceCore.getEndpoint().audDevManager().getCaptureDevMedia().startTransmit(am);
                    am.startTransmit(voiceCore.getEndpoint().audDevManager().getPlaybackDevMedia());
                } catch (Exception e) {}
            }
        }
    }

    public interface Callbacks {
        void onAnswerCall();
        void onDisconnect();
    }
}
