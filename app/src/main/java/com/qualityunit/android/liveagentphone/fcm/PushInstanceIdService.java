package com.qualityunit.android.liveagentphone.fcm;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class PushInstanceIdService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, PushRegistrationIntentService.class);
        startService(intent);
    }
}
