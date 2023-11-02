package com.qualityunit.android.liveagentphone.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.service.CallingService;
import com.qualityunit.android.liveagentphone.ui.call.InitCallActivity;
import com.qualityunit.android.liveagentphone.ui.call.InitCallDismissReceiver;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.Date;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import fr.turri.jiso8601.Iso8601Deserializer;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static com.qualityunit.android.liveagentphone.Const.ChannelId.MAKE_CALL_CHANNEL_ID;
import static com.qualityunit.android.liveagentphone.Const.NotificationId.INIT_CALL_NOTIFICATION_ID;

public class PushMessagingService extends FirebaseMessagingService {

    private static final String TAG = PushMessagingService.class.getSimpleName();
    private static final int pendingIntentFlags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;

    @Override
    public void onNewToken(@NonNull String pushToken) {
        Intent intent = new Intent(this, PushRegistrationIntentService.class);
        intent.putExtra("newPushToken", pushToken);
        startService(intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> remoteMessageData = remoteMessage.getData();
        if (remoteMessageData.size() > 0) {
            String infoMsg = "--------------------------------------------------\n"
                    + "NEW PUSH NOTIFICATION: type:'" + remoteMessageData.get("type") + "' - " + remoteMessageData.get("title");
            Logger.logToFile(getApplicationContext(), infoMsg);
        }
        Bundle data = new Bundle();
        for (Map.Entry<String, String> entry : remoteMessageData.entrySet()) {
            data.putString(entry.getKey(), entry.getValue());
        }
        try {
            // check if push is actual
            String time = data.getString("time");
            if (TextUtils.isEmpty(time)) {
                throw new CallingException("Push notification value 'time' is empty");
            }
            Date datePush = Iso8601Deserializer.toDate(time);
            Date dateSystem = new Date();
            long delta = (dateSystem.getTime() - datePush.getTime());
            if (delta > Const.Push.MAX_INCOMING_CALL_PUSH_DELAY) {
                Logger.logToFile(getApplicationContext(), "PUSH: Late push (" + delta + " millis) Cancelling SIP registration.");
                return;
            }

            // check if user is logged in app
            if (!LaAccount.isSet()) {
                throw new CallingException("Push notification received, but account is not set");
            }

            // check type of push notification
            String type = data.getString("type");
            if (type == null) {
                throw new CallingException("Missing type field in calling push notification");
            }
            switch (type) {
                case Const.Push.PUSH_TYPE_INCOMING_CALL:
                    CallingCommands.incomingCall(getApplicationContext());
                    break;
                case Const.Push.PUSH_TYPE_INIT_CALL:
                    Intent initCallIntent = new Intent(getApplicationContext(), InitCallActivity.class);
                    initCallIntent.putExtras(data);
                    initCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // pre-oreo versions
                        getApplicationContext().startActivity(initCallIntent);
                    } else {
                        RemoteViews headUpLayout = new RemoteViews(getPackageName(), R.layout.notification_init_call);
                        String contact = data.getString(InitCallActivity.EXTRA_CONTACT_NAME, data.getString(InitCallActivity.EXTRA_REMOTE_NUMBER, getString(R.string.unknown)));
                        String dialString = data.getString(InitCallActivity.EXTRA_DIAL_STRING);
                        headUpLayout.setTextViewText(R.id.text, contact);
                        //create service pending intent
                        Intent callingServiceIntent = new Intent(this, CallingService.class)
                                .putExtra("command", CallingService.COMMANDS.MAKE_CALL)
                                .putExtra("prefix", "")
                                .putExtra("remoteNumber", dialString)
                                .putExtra("remoteName", contact);
                        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, callingServiceIntent, pendingIntentFlags);
                        headUpLayout.setOnClickPendingIntent(R.id.call, servicePendingIntent);
                        Intent initCallDismissBroadcast = new Intent(this, InitCallDismissReceiver.class);
                        PendingIntent initCallDismissPendingIntent = PendingIntent.getBroadcast(this, 0, initCallDismissBroadcast, pendingIntentFlags);
                        headUpLayout.setOnClickPendingIntent(R.id.cancel, initCallDismissPendingIntent);
                        NotificationChannel channel = new NotificationChannel(MAKE_CALL_CHANNEL_ID, getString(R.string.make_call), IMPORTANCE_HIGH);
                        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.createNotificationChannel(channel);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MAKE_CALL_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_call_24dp)
                                .setCustomBigContentView(headUpLayout)
                                .setCustomContentView(headUpLayout)
                                .setCustomHeadsUpContentView(headUpLayout)
                                .setContentTitle(getString(R.string.make_call))
                                .setContentText(contact)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setCategory(NotificationCompat.CATEGORY_CALL)
                                .setFullScreenIntent(PendingIntent.getActivity(this, 0, initCallIntent, pendingIntentFlags), true);
                        Notification notification = builder.build();
                        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(INIT_CALL_NOTIFICATION_ID, notification);
                    }
                    break;
                case Const.Push.PUSH_TYPE_CANCEL_INIT_CALL:
                    Intent initCallDismissBroadcast = new Intent(this, InitCallDismissReceiver.class);
                    PendingIntent initCallDismissPendingIntent = PendingIntent.getBroadcast(this, 0, initCallDismissBroadcast, pendingIntentFlags);
                    initCallDismissPendingIntent.send();
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(InitCallActivity.INTENT_FILTER_INITCALL_DISMISS));
                    break;
                default:
                    throw new CallingException("Unknown push type: '" + type + "'");
            }
        } catch (Exception e) {
            Logger.logToFile(getApplicationContext(), "PUSH ERROR: " + e.getMessage());
            Logger.e(TAG, e.getMessage(), e);
        }
    }

}
