package com.qualityunit.android.voice;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.AuthCredInfoVector;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnRegStateParam;
import org.pjsip.pjsua2.pj_qos_type;
import org.pjsip.pjsua2.pjsip_status_code;

import java.util.UUID;

public class VoiceAccount extends Account {

    private Callbacks callbacks;
    private boolean isRegistered;

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
                if (!isRegistered) {
                    callbacks.onSipRegistrationSuccess();
                    isRegistered = true;
                }
            } else {
                isRegistered = false;
                callbacks.onSipRegistrationFailure("Asterisk returned code '" + returnCode + "' while SIP registration");
            }
        } catch (Exception e) {
            callbacks.onSipRegistrationFailure(e.getMessage());
        }
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        callbacks.onIncomingVoiceCall(prm.getCallId());
    }

    public static AccountConfig createAccountConfig(@NonNull String sipHost, @NonNull String sipUser, @NonNull String sipPassword) {
        // create URIs
        String sipRegisterUri = "sip:" + sipHost;
        String sipAccountUri = "sip:" + sipUser + "@" + sipHost;
        // set voiceAccount config
        AccountConfig accountConfig = new AccountConfig();
        accountConfig.getMediaConfig().getTransportConfig().setQosType(pj_qos_type.PJ_QOS_TYPE_VOICE);
        // create random callId (https://github.com/VoiSmart/pjsip-android/issues/72)
        String callId = UUID.randomUUID().toString();
        if (!TextUtils.isEmpty(callId)) {
            accountConfig.getRegConfig().setCallID(callId);
        }
        accountConfig.getRegConfig().setRegistrarUri(sipRegisterUri);
        accountConfig.getRegConfig().setRegisterOnAdd(true);
        accountConfig.setIdUri(sipAccountUri);
        AuthCredInfoVector creds = accountConfig.getSipConfig().getAuthCreds();
        creds.clear();
        creds.add(new AuthCredInfo("digest", "*", sipUser, 0, sipPassword));
        accountConfig.getSipConfig().setAuthCreds(creds);
        accountConfig.getNatConfig().setIceEnabled(true);
        accountConfig.getVideoConfig().setAutoTransmitOutgoing(false);
        accountConfig.getVideoConfig().setAutoShowIncoming(false);
        return accountConfig;
    }

    public void register(String sipHost, String sipUser, String sipPassword) throws Exception {
        create(createAccountConfig(sipHost, sipUser, sipPassword), true);
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public interface Callbacks {
        void onSipRegistrationSuccess();
        void onSipRegistrationFailure(String errorMessage);
        void onIncomingVoiceCall(int callId);
    }

}
