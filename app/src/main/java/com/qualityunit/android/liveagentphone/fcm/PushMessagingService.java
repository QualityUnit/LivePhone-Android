package com.qualityunit.android.liveagentphone.fcm;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.ui.call.InitCallActivity;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.Date;
import java.util.Map;

import fr.turri.jiso8601.Iso8601Deserializer;

public class PushMessagingService extends FirebaseMessagingService {

    private static final String TAG = PushMessagingService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> remoteMessageData = remoteMessage.getData();
        if (remoteMessageData.size() > 0) {
            String infoMsg = "-----------------------------------------------------------------------------------\nPUSH NOTIFICATION: " + remoteMessageData;
            Logger.logToFile(infoMsg);
            Log.d(TAG, infoMsg);
        }
        Bundle data = new Bundle();
        for (Map.Entry<String, String> entry : remoteMessageData.entrySet()) {
            data.putString(entry.getKey(), entry.getValue());
        }
        try {
            // check if push is actual
            String time = data.getString("time");
            if (TextUtils.isEmpty(time)) {
                String errMsg = "Error: Push notification value 'time' is empty";
                Logger.logToFile(errMsg);
                throw new CallingException(errMsg);
            }
            Date datePush = Iso8601Deserializer.toDate(time);
            Date dateSystem = new Date();
            long delta = (dateSystem.getTime() - datePush.getTime());
            String infoMsg = "Info: Push delayed: " + delta + " millis";
            Logger.logToFile(infoMsg);
            if (delta > Const.Push.MAX_INCOMING_CALL_PUSH_DELAY) {
                String warnMsg = "Warning: Late push (" + delta + " millis) Cancelling SIP registration.";
                Logger.logToFile(warnMsg);
                Log.d(TAG, warnMsg);
                return;
            }

            // check if user is logged in app
            if (!LaAccount.isSet()) {
                throw new CallingException("Error: Push notification received, but account is not set");
            }

            // check type of push notification
            String type = data.getString("type");
            if (type == null) {
                throw new CallingException("Error: Missing type field in calling push notification");
            }
            switch (type) {
                case Const.Push.PUSH_TYPE_INCOMING_CALL:
                    Logger.logToFile("PUSH: Starting CallingService...");
                    CallingCommands.incomingCall(getApplicationContext());
                    break;
                case Const.Push.PUSH_TYPE_INIT_CALL:
                    Intent intent = new Intent(getApplicationContext(), InitCallActivity.class);
                    intent.putExtras(data);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                    break;
                case Const.Push.PUSH_TYPE_CANCEL_INIT_CALL:
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(InitCallActivity.INTENT_FILTER_INIT_CALL).putExtras(data));
                    break;
                default:
                    throw new CallingException("Error: Unknown push type: '" + type + "'");
            }
        } catch (Exception e) {
            Logger.logToFile(e.getMessage());
            Logger.e(TAG, e);
        }
    }
}
