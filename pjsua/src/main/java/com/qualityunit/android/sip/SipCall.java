package com.qualityunit.android.sip;

import android.util.Log;

import org.pjsip.pjsua2.AudioMedia;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.CallInfo;
import org.pjsip.pjsua2.CallMediaInfo;
import org.pjsip.pjsua2.CallMediaInfoVector;
import org.pjsip.pjsua2.Media;
import org.pjsip.pjsua2.OnCallMediaStateParam;
import org.pjsip.pjsua2.OnCallStateParam;
import org.pjsip.pjsua2.pjmedia_type;
import org.pjsip.pjsua2.pjsip_inv_state;
import org.pjsip.pjsua2.pjsua_call_media_status;

public class SipCall extends Call {
//    public VideoWindow vidWin;
//    public VideoPreview vidPrev;
    private SipCore sipCore;

    public SipCall(SipAccount acc, int call_id, SipCore sipCore) {
        super(acc, call_id);
        this.sipCore = sipCore;
//        vidWin = null;
    }

    @Override
    public void onCallState(OnCallStateParam prm) {
        sipCore.observer.notifyCallState(this);
        try {
            CallInfo ci = getInfo();
            if (ci.getState() == pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED) {
                this.delete();
            }
        } catch (Exception e) {}
    }

    @Override
    public void onCallMediaState(OnCallMediaStateParam prm) {
        CallInfo ci;
        try {
            ci = getInfo();
            String callIdString = ci.getCallIdString();
            Log.d("SipCall", "#### CALL-ID" + callIdString);
        } catch (Exception e) {
            return;
        }


        CallMediaInfoVector cmiv = ci.getMedia();

        for (int i = 0; i < cmiv.size(); i++) {
            CallMediaInfo cmi = cmiv.get(i);
            if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                    (cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE || cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_REMOTE_HOLD)) {
                // unfortunately, on Java too, the returned Media cannot be
                // downcasted to AudioMedia
                Media m = getMedia(i);
                AudioMedia am = AudioMedia.typecastFromMedia(m);

                // connect ports
                try {
                    sipCore.ep.audDevManager().getCaptureDevMedia().startTransmit(am);
                    am.startTransmit(sipCore.ep.audDevManager().getPlaybackDevMedia());
                } catch (Exception e) {}
            }
//            else if (cmi.getType() == pjmedia_type.PJMEDIA_TYPE_VIDEO &&
//                    cmi.getStatus() == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE &&
//                    cmi.getVideoIncomingWindowId() != pjsua2.INVALID_ID) {
//                vidWin = new VideoWindow(cmi.getVideoIncomingWindowId());
//                vidPrev = new VideoPreview(cmi.getVideoCapDev());
//            }
        }

        sipCore.observer.notifyCallMediaState(this);
    }
}
