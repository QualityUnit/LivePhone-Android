package com.qualityunit.android.liveagentphone.acc;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by rasto on 22.01.16.
 */
public class LaAuthenticatorService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        LaAccountAuthenticator authenticator = new LaAccountAuthenticator(this);
        return authenticator.getIBinder();
    }

}
