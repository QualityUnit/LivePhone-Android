package com.qualityunit.android.sip;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnRegStateParam;

public class SipAccount extends Account {
    public AccountConfig cfg;
    private SipCore sipCore;

    public SipAccount(AccountConfig config, SipCore sipCore) {
        super();
        cfg = config;
        this.sipCore = sipCore;
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        sipCore.observer.notifyRegState(prm.getCode(), prm.getReason(), prm.getExpiration());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        System.out.println("======== Incoming call ======== ");
        SipCall call = new SipCall(this, prm.getCallId(), sipCore);
        sipCore.observer.notifyIncomingCall(call);
    }

    @Override
    public void onInstantMessage(OnInstantMessageParam prm) {
       // nothing
    }
}
