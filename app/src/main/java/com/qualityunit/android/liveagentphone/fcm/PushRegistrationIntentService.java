package com.qualityunit.android.liveagentphone.fcm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.util.EmptyValueException;
import com.qualityunit.android.liveagentphone.util.Tools;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PushRegistrationIntentService extends IntentService {

    private static final String TAG = PushRegistrationIntentService.class.getSimpleName();
    public static final String INTENT_REGISTRATION_COMPLETE = "INTENT_REGISTRATION_COMPLETE";
    public static final String IS_REGISTRATION_SUCCESS = "isRegistrationSuccess";
    public static final String FAILURE_MESSAGE = "failureMessage";
    private static final String REQUEST_KEY_DEVICE_ID = "deviceId";
    private static final String REQUEST_KEY_PUSH_TOKEN = "pushToken";
    private static final String REQUEST_KEY_PLATFORM_ANDROID = "platform";
    private static final String REQUEST_VALUE_PLATFORM_ANDROID = "android";

    public PushRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Account account = LaAccount.get();
        AccountManager accountManager = AccountManager.get(getApplicationContext());
        try {
            String pushToken = FirebaseInstanceId.getInstance().getToken();
            Log.d(TAG, "FCM Registration Token: " + pushToken);
            final String phoneId = accountManager.getUserData(account, LaAccount.USERDATA_PHONE_ID);
            final String deviceId = Tools.getDeviceUniqueId();
            final String remotePushToken = intent.getStringExtra("remotePushToken");
            final String remoteDeviceId = intent.getStringExtra("remoteDeviceId");
            if (TextUtils.isEmpty(remoteDeviceId) || TextUtils.isEmpty(remotePushToken) || !remoteDeviceId.equals(deviceId) || !remotePushToken.equals(pushToken)) {
                // update params with FCM token to server
                sendRegistrationToServer(pushToken, phoneId, deviceId);
            } else {
                // do not update params - just notify that it's done
                sendFcmRegistraionBroadcast(true, null);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            sendFcmRegistraionBroadcast(false, e.getMessage());
        }
    }

    /**
     * Send app broadcast about registraion step
     *
     * @param success Set to true when FCM registration has been succeeded
     */
    private void sendFcmRegistraionBroadcast(boolean success, String failureMessage) {
        Intent registrationComplete = new Intent(INTENT_REGISTRATION_COMPLETE);
        registrationComplete.putExtra(IS_REGISTRATION_SUCCESS, success);
        if (!TextUtils.isEmpty(failureMessage)) {
            registrationComplete.putExtra(FAILURE_MESSAGE, failureMessage);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * @param pushToken The new token.
     * @param phoneId Phone ID got from /phones/_app_.
     * @param deviceId Device unique ID.
     */
    private void sendRegistrationToServer(String pushToken, final String phoneId, String deviceId) throws EmptyValueException, JSONException {
        if (TextUtils.isEmpty(pushToken)) {
            throw new EmptyValueException("Push token");
        }
        if (TextUtils.isEmpty(phoneId)) {
            throw new EmptyValueException("Phone ID");
        }
        if (TextUtils.isEmpty(deviceId)) {
            throw new EmptyValueException("Device unique ID");
        }
        final JSONObject params = new JSONObject();
        params.put(REQUEST_KEY_DEVICE_ID, deviceId);
        params.put(REQUEST_KEY_PLATFORM_ANDROID, REQUEST_VALUE_PLATFORM_ANDROID);
        params.put(REQUEST_KEY_PUSH_TOKEN, pushToken);
        final Map<String, Object> requestParam = new HashMap<String, Object>(1){{
            put("params", params.toString());
        }};
        final Account account = LaAccount.get();
        final AccountManager accountManager = AccountManager.get(getApplicationContext());
//        accountManager.invalidateAuthToken(account.type, null);
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, true, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(final AccountManagerFuture<Bundle> future) {
                try {
                    Bundle result = future.getResult();
                    String basepath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String apikey = result.getString(AccountManager.KEY_AUTHTOKEN);
                    com.qualityunit.android.liveagentphone.net.Client.updatePhoneParams(getApplicationContext(), basepath, phoneId, apikey, requestParam, new com.qualityunit.android.liveagentphone.net.Client.Callback<JSONObject>() {
                        @Override
                        public void onSuccess(JSONObject object) {
                            sendFcmRegistraionBroadcast(true, null);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                            sendFcmRegistraionBroadcast(false, e.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    sendFcmRegistraionBroadcast(false, e.getMessage());
                }
            }

        }, null);

    }

}