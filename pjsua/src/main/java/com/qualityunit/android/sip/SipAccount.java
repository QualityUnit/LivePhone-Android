package com.qualityunit.android.sip;

import org.pjsip.pjsua2.Account;
import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.BuddyConfig;
import org.pjsip.pjsua2.OnIncomingCallParam;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.OnRegStateParam;

import java.util.ArrayList;

public class SipAccount extends Account {
    public ArrayList<SipBuddy> buddyList = new ArrayList<SipBuddy>();
    public AccountConfig cfg;

    public SipAccount(AccountConfig config) {
        super();
        cfg = config;
    }

    public SipBuddy addBuddy(BuddyConfig bud_cfg) {
    /* Create Buddy */
        SipBuddy bud = new SipBuddy(bud_cfg);
        try {
            bud.create(this, bud_cfg);
        } catch (Exception e) {
            bud.delete();
            bud = null;
        }

        if (bud != null) {
            buddyList.add(bud);
            if (bud_cfg.getSubscribe())
                try {
                    bud.subscribePresence(true);
                } catch (Exception e) {
                }
        }

        return bud;
    }

    public void delBuddy(SipBuddy buddy) {
        buddyList.remove(buddy);
        buddy.delete();
    }

    public void delBuddy(int index) {
        SipBuddy bud = buddyList.get(index);
        buddyList.remove(index);
        bud.delete();
    }

    @Override
    public void onRegState(OnRegStateParam prm) {
        SipCore.observer.notifyRegState(prm.getCode(), prm.getReason(), prm.getExpiration());
    }

    @Override
    public void onIncomingCall(OnIncomingCallParam prm) {
        System.out.println("======== Incoming call ======== ");
        SipCall call = new SipCall(this, prm.getCallId());
        SipCore.observer.notifyIncomingCall(call);
    }

    @Override
    public void onInstantMessage(OnInstantMessageParam prm) {
        System.out.println("======== Incoming pager ======== ");
        System.out.println("From     : " + prm.getFromUri());
        System.out.println("To       : " + prm.getToUri());
        System.out.println("Contact  : " + prm.getContactUri());
        System.out.println("Mimetype : " + prm.getContentType());
        System.out.println("Body     : " + prm.getMsgBody());
    }
}
