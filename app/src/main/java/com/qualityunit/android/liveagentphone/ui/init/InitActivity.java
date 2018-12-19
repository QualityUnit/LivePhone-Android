package com.qualityunit.android.liveagentphone.ui.init;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.fcm.PushRegistrationIntentService;
import com.qualityunit.android.liveagentphone.net.loader.GenericLoader;
import com.qualityunit.android.liveagentphone.net.loader.LoaderResult;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.ui.auth.AuthActivity;
import com.qualityunit.android.liveagentphone.ui.home.HomeActivity;
import com.qualityunit.android.liveagentphone.util.EmptyValueException;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.restful.util.ResponseProcessor;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by rasto on 26.01.16.
 */
public class InitActivity extends AppCompatActivity {

    private static final String TAG = InitActivity.class.getSimpleName();
    private static final int REQUEST_CODE_LOGIN = 0;
    private static final int DEFAULT_COUNTDOWN_MILLIS = 1500;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int LOADER_ID_PHONE_GET = 0;
    private static final int MAX_RETRY_NUMBER = 3;
    private TextView tvLoading;
    private LinearLayout pbLoading;
    private CountDownTimer countDownTimer;
    private long countDownStarted;
    private View llButtons;
    private Button bRetry;
    private Button bQuit;
    private PhoneGetLoaderCallbacks phoneGetLoaderCallbacks;
    private BroadcastReceiver fcmRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;
    private final int REQ_GET_ACCOUNTS = 1001;
    private final int REQ_RECORD_AUDIO = 1002;
    private final int REQ_WRITE_EXTERNAL_STORAGE = 1003;
    private boolean grantedGetAccounts;
    private boolean grantedWriteExternalStorage;
    private boolean grantedRecordAudio;
    // variables to save into instanceBundle
    private long countDownMillisLeft = DEFAULT_COUNTDOWN_MILLIS;
    private int retryNumber;
    private boolean fcmIsRegistered;
    private boolean phoneIsLoaded;
    private String deviceId;
    private String pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_activity);
        llButtons = findViewById(R.id.ll_buttons);
        tvLoading = (TextView) findViewById(R.id.tv_loading_status);
        pbLoading = (LinearLayout) findViewById(R.id.pb_loading);
        bRetry = (Button) findViewById(R.id.b_retry);
        bQuit = (Button) findViewById(R.id.b_quit);
        bRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (retryNumber++ < MAX_RETRY_NUMBER) {
                    startInit(true);
                }
                else {
                    retryNumber = 0;
                    startActivityForResult(new Intent(InitActivity.this, AuthActivity.class), REQUEST_CODE_LOGIN);
                    overridePendingTransition(0, 0);
                }
            }
        });
        bQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        registerReceiver(true);
        if (countDownTimer == null) {
            countDownTimer = new InitCountDownTimer(countDownMillisLeft);
        }
        countDownStarted = System.currentTimeMillis();
        countDownTimer.start();
        startInit(false);
        super.onResume();
    }

    @Override
    protected void onPause() {
        registerReceiver(false);
        countDownTimer.cancel();
        countDownTimer = null;
        countDownMillisLeft = System.currentTimeMillis() - countDownStarted;
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                startInit(true);
            }
        }
    }

    /**
     * Start (or restart again) initialization of app
     * @param forceReset
     */
    private void startInit(boolean forceReset) {
        if (!checkPlayServices() || !checkAppPermissions()) {
            return;
        }
        showProgress();
        if (!LaAccount.isSet() || !accountIsAdded()) {
            startActivityForResult(new Intent(this, AuthActivity.class), REQUEST_CODE_LOGIN);
            overridePendingTransition(0, 0);
            return;
        }
        phoneGet(forceReset);
    }

    private void phoneGet(boolean force) {
        if (phoneGetLoaderCallbacks == null) {
            phoneGetLoaderCallbacks = new PhoneGetLoaderCallbacks();
        }
        if (force) {
            phoneIsLoaded = false;
            getSupportLoaderManager().restartLoader(LOADER_ID_PHONE_GET, null, phoneGetLoaderCallbacks);
        }
        else {
            if (phoneIsLoaded) {
                registerPushNotifications();
            } else {
                getSupportLoaderManager().restartLoader(LOADER_ID_PHONE_GET, null, phoneGetLoaderCallbacks);
            }
        }
    }

    private void registerPushNotifications() {
        if (!fcmIsRegistered) {
            registerReceiver(true);
            Intent intent = new Intent(InitActivity.this, PushRegistrationIntentService.class);
            intent.putExtra("remoteDeviceId", deviceId);
            intent.putExtra("remotePushToken", pushToken);
            startService(intent);
        } else {
            go();
        }
    }

    /**
     * Register on unregister receiver to receive broadcast messages from service which sends PUT /phones/{id}/_updateParams
     * @param register
     */
    private void registerReceiver(boolean register){
        if (register) {
            // registering receiver
            if (fcmRegistrationBroadcastReceiver == null) {
                fcmRegistrationBroadcastReceiver = new FcmRegistrationBroadcastReceiver();
            }
            if (!isReceiverRegistered) {
                LocalBroadcastManager.getInstance(this).registerReceiver(fcmRegistrationBroadcastReceiver,
                        new IntentFilter(PushRegistrationIntentService.INTENT_REGISTRATION_COMPLETE));
                isReceiverRegistered = true;
            }
        }
        else {
            // unregistering receiver
            if (fcmRegistrationBroadcastReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(fcmRegistrationBroadcastReceiver);
            }
            isReceiverRegistered = false;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                showError(getString(R.string.play_resolvable_problem));

            } else {
                String msg = getString(R.string.play_no_support);
                showError(msg);
                Log.i(TAG, msg);
                if (bRetry != null) {
                    bRetry.setVisibility(View.GONE);
                }
                showError(msg);
            }
            return false;
        }
        return true;
    }

    /**
     * Check if the user granted all the permission that app needs
     * @return
     */
    private boolean checkAppPermissions() {
        // Dangerous permissions: GET_ACCOUNTS, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO
        if (!checkOnePermission(GET_ACCOUNTS, REQ_GET_ACCOUNTS)) {
            return false;
        } else if (!checkOnePermission(WRITE_EXTERNAL_STORAGE, REQ_WRITE_EXTERNAL_STORAGE)) {
            return false;
        }  else if (!checkOnePermission(RECORD_AUDIO, REQ_RECORD_AUDIO)) {
            return false;
        }
        return true;
    }

    private boolean checkOnePermission(String permissionName, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(InitActivity.this, permissionName) != PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            // returns false only if the user selected Never ask again
            if (ActivityCompat.shouldShowRequestPermissionRationale(InitActivity.this, permissionName)) {
                showPermissionNotification("You cannot use app without '" + permissionName + "' permission", permissionName, requestCode);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(InitActivity.this, new String[]{ permissionName }, requestCode);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        }
        return true;
    }

    private void showPermissionNotification(String message, final String permissionName, final int requestCode) {
        AlertDialog alertDialog = new AlertDialog.Builder(InitActivity.this).create();
        alertDialog.setTitle("Uh oh");
        alertDialog.setMessage(message);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(InitActivity.this, new String[]{ permissionName }, requestCode);
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQ_GET_ACCOUNTS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d(TAG, "Permission granted: GET_ACCOUNTS");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "Permission denied: GET_ACCOUNTS");
                }
                break;
            case REQ_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted: WRITE_EXTERNAL_STORAGE");
                } else {
                    Log.d(TAG, "Permission denied: WRITE_EXTERNAL_STORAGE");
                }
                break;
            case REQ_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted: RECORD_AUDIO");
                } else {
                    Log.d(TAG, "Permission denied: RECORD_AUDIO");
                }
                break;
        }
    }


    /**
     * Check if last used account is not removed from android accounts
     * @return
     */
    private boolean accountIsAdded() {
        Account currentAccount = LaAccount.get();
        if (currentAccount != null) {
            Account[] accounts = AccountManager.get(this).getAccountsByType(LaAccount.ACCOUNT_TYPE);
            for(Account account : accounts) {
                if(account.name.equals(currentAccount.name)) {
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * Try to loop main activity of the app. All you need is to be ready - that means everything included in IF condition should be loaded or executed
     */
    private void go() {
        if (countDownMillisLeft == 0
                && fcmIsRegistered
                && phoneIsLoaded) {
            startActivity(new Intent(this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        }
    }

    /**
     * Show buttons, progressbar and error message when initiating is interupted
     * @param errorMsg Error message
     */
    private void showError(String errorMsg) {
        pbLoading.setVisibility(View.GONE);
        llButtons.setVisibility(View.VISIBLE);
        tvLoading.setText(errorMsg);
    }

    /**
     * Hide buttons, progressbar and error message when initiating starts
     */
    private void showProgress () {
        pbLoading.setVisibility(View.VISIBLE);
        llButtons.setVisibility(View.GONE);
    }

    /**
     * This class is receiving local broadcast messages about GCM registration
     */
    private class FcmRegistrationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PushRegistrationIntentService.INTENT_REGISTRATION_COMPLETE.equals(intent.getAction())) {
                fcmIsRegistered = intent.getBooleanExtra(PushRegistrationIntentService.IS_REGISTRATION_SUCCESS, false);
                if (fcmIsRegistered) {
                    go();
                }
                else {
                    String msg = intent.getStringExtra(PushRegistrationIntentService.FAILURE_MESSAGE);
                    if (!TextUtils.isEmpty(msg)) {
                        showError(msg);
                    }
                    else {
                        showError(getString(R.string.oops));
                    }
                }
            }
        }

    }

    /**
     * Loader GET /Phones/_app_
     */
    private static class PhoneGetLoader extends GenericLoader<Response> {

        private Call call;

        public PhoneGetLoader(Activity activity) {
            super(activity);
        }

        @Override
        protected Response requestData(String basePath, String token) throws IOException {
            final Client client = Client.getInstance();
            final Request request = client.GET(basePath, "/phones/_app_", token).build();
            if (call != null) {
                call.cancel();
            }
            call = client.newCall(request);
            return call.execute();
        }

    }

    /**
     * LoaderCallbacks GET /Phones/_app_
     */
    private class PhoneGetLoaderCallbacks implements LoaderManager.LoaderCallbacks<LoaderResult<Response>> {

        @Override
        public Loader<LoaderResult<Response>> onCreateLoader(int id, Bundle args) {
            PhoneGetLoader loader = new PhoneGetLoader(InitActivity.this);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<LoaderResult<Response>> loader, final LoaderResult<Response> data) {
            if (data == null) {
                InitActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Error: LoaderResult cannot be null";
                        showError(message);
                        Logger.e(TAG, message);
                    }
                });
            }
            if (data.getObject() == null) {
                InitActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Error '" + data.getCode() + "': " + data.getMessage();
                        showError(message);
                        Logger.e(TAG, message);
                    }
                });
            }
            ResponseProcessor.bodyToJson(data.getObject(), new ResponseProcessor.ResponseCallback() {
                @Override
                public void onSuccess(final JSONObject object) {
                    InitActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final Account account = LaAccount.get();
                                final AccountManager accountManager = AccountManager.get(InitActivity.this);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_PHONE_ID, "id", true);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_NUMBER, "number", false);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_HOST, "connection_host", true);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_USER, "connection_user", true);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_PASS, "connection_pass", true);
                                addAccountData(accountManager, account, object, LaAccount.USERDATA_AGENT_ID, "agent_id", true);
                                phoneIsLoaded = true;
                                if (object.has("params")) {
                                    String paramsString = object.getString("params");
                                    JSONObject paramsObj = new JSONObject(paramsString);
                                    deviceId = paramsObj.getString("deviceId");
                                    pushToken = paramsObj.getString("pushToken");
                                }
                                registerPushNotifications();
                            } catch (EmptyValueException | JSONException e) {
                                showError(e.getMessage());
                                Logger.e(TAG, e);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final Exception e) {
                    InitActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showError(e.getMessage());
                            Logger.e(TAG, e);
                        }
                    });
                }
            });
        }

        private void addAccountData(AccountManager accountManager, Account account, JSONObject object, String key, String objectKey, boolean required) throws EmptyValueException, JSONException {
            if (required && (!object.has(objectKey) || TextUtils.isEmpty(object.getString(objectKey)))) {
                throw new EmptyValueException(key);
            }
            else {
                accountManager.setUserData(account, key, object.getString(objectKey));
            }
        }

        @Override
        public void onLoaderReset(Loader<LoaderResult<Response>> loader) {

        }
    }

    /**
     * Countdown timer for splash delay
     */
    private class InitCountDownTimer extends CountDownTimer {

        public InitCountDownTimer(long millisInFuture) {
            super(millisInFuture, DEFAULT_COUNTDOWN_MILLIS);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            // do nothing
        }
        @Override
        public void onFinish() {
            countDownMillisLeft = 0;
            go();
        }

    }

}
