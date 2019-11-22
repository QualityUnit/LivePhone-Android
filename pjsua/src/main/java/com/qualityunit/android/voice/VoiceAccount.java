package com.qualityunit.android.voice;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pjsip_status_code;

public class VoiceAccount extends Account {

    private Callbacks callbacks;

    public VoiceAccount(Callbacks callbacks) {
        super();
        this.callbacks = callbacks;
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        pjsip_status_code code = prm.getCode();
        int expiration = prm.getExpiration();
        try {
            int returnCode = code.swigValue();
            if (returnCode / 100 == 2 && expiration != 0) {
                callbacks.onSipRegistrationSuccess();
            } else {
                String errMsg = "SERVICE: Error while SIP registration: Asterisk returned code '" + returnCode + "'";
                callbacks.onSipRegistrationFailure(errMsg);
            }
        } catch (Exception e) {
            callbacks.onSipRegistrationFailure(e.getMessage());
        }
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        int callId = prm.getCallId();
        callbacks.onIncomingVoiceCall(callId);
    }

    public interface Callbacks {
        void onSipRegistrationSuccess();
        void onSipRegistrationFailure(String errorMessage);
        VoiceCall onIncomingVoiceCall(int callId);
    }

}
