package com.qualityunit.android.liveagentphone.gcm;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.Date;

import fr.turri.jiso8601.Iso8601Deserializer;

public class PushListenerService extends GcmListenerService {

    private static final String TAG = PushListenerService.class.getSimpleName();

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### PUSH NOTIFICATION RECEIVED ####\n");
        for (String key : data.keySet()) {
            sb.append("\t" + key + ": " + data.get(key));
        }
        Log.d(TAG, sb.toString());
        try {
            // check type of push notification
            String type = data.getString("type");
            if (type == null) {
                throw new CallingException("Missing type field in calling push notification");
            }
            switch (type) {
                case Const.Push.PUSH_TYPE_INCOMING_CALL:
                    processPushIncomingCall(data);
                    break;
                default:
                    throw new CallingException("Unknown push type: '" + type + "'");
            }
        } catch (Exception e) {
            Logger.e(TAG, e);
        }
    }

    private void processPushIncomingCall(Bundle data) throws CallingException {
        // check if push is actual
        String time = data.getString("time");
        if (TextUtils.isEmpty(time)) {
            throw new CallingException("Invalid value in time field of 'I' push notification: '" + time + "'.");
        }
        Date datePush = Iso8601Deserializer.toDate(time);
        if (datePush == null) {
            throw new CallingException("Failed to parse 'time' from calling push notification");
        }
        Date dateSystem = new Date();
        long delta = (dateSystem.getTime() - datePush.getTime()) / 1000;
        Log.d(TAG, "Push has come in " + delta + " seconds");
        if (delta > Const.Push.MAX_INCOMING_CALL_PUSH_DELAY) {
            Log.d(TAG, "Late push has come - cancelling SIP registration.");
            return;
        }

        // check if user is logged in app
        if (!LaAccount.isSet()) {
            throw new CallingException("Push notification received, but account is not set");
        }

        // get SIP library ready for incoming call
        CallingCommands.incomingCall(getApplicationContext());
    }
}