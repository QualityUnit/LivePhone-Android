package com.qualityunit.android.sip;

import org.pjsip.pjsua2.pjsip_status_code;

/* Interface to separate UI & engine a bit better */
public interface SipAppObserver {

    void notifyRegState(pjsip_status_code code, String reason, int expiration);
    void notifyIncomingCall(SipCall call);
    void notifyCallState(SipCall call);
    void notifyCallMediaState(SipCall call);
    void notifyBuddyState(SipBuddy buddy);
}
