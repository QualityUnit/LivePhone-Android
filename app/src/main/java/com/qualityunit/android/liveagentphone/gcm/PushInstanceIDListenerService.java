package com.qualityunit.android.liveagentphone.gcm;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

public class PushInstanceIDListenerService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, PushRegistrationIntentService.class);
        startService(intent);
    }
}