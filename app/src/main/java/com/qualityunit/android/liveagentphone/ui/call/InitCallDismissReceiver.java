package com.qualityunit.android.liveagentphone.ui.call;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.qualityunit.android.liveagentphone.fcm.PushMessagingService;

public class InitCallDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(PushMessagingService.INIT_CALL_NOTIFICATION_ID);
    }
}