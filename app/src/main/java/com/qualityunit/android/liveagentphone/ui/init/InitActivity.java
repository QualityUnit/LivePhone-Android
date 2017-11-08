package com.qualityunit.android.liveagentphone.ui.init;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
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
import com.qualityunit.android.liveagentphone.gcm.PushRegistrationIntentService;
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
    private BroadcastReceiver gcmRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;
    // variables to save into instanceBundle
    private long countDownMillisLeft;
    private String errorMsg;
    private int retryNumber;
    private boolean gcmIsRegistered;
    private boolean phoneIsLoaded;
    private boolean isError;
    private String deviceId;
    private String pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);
        llButtons = findViewById(R.id.ll_buttons);
        tvLoading = (TextView) findViewById(R.id.tv_loading_status);
        pbLoading = (LinearLayout) findViewById(R.id.pb_loading);
        bRetry = (Button) findViewById(R.id.b_retry);
        bQuit = (Button) findViewById(R.id.b_quit);
        if (savedInstanceState != null) {
            countDownMillisLeft = savedInstanceState.getLong("countDownMillisLeft", DEFAULT_COUNTDOWN_MILLIS);
            errorMsg = savedInstanceState.getString("errorMsg", "");
            isError = savedInstanceState.getBoolean("isError", false);
            retryNumber = savedInstanceState.getInt("retryNumber");
            gcmIsRegistered = savedInstanceState.getBoolean("gcmIsRegistered", false);
            phoneIsLoaded = savedInstanceState.getBoolean("phoneIsLoaded", false);
            deviceId = savedInstanceState.getString("deviceId",null);
            pushToken = savedInstanceState.getString("pushToken", null);
        } else {
            countDownMillisLeft = DEFAULT_COUNTDOWN_MILLIS;
            errorMsg = "";
            isError = false;
            retryNumber = 0;
            gcmIsRegistered = false;
            phoneIsLoaded = false;
            deviceId = null;
            pushToken = null;
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("countDownMillisLeft", countDownMillisLeft);
        outState.putString("errorMsg", errorMsg);
        outState.putBoolean("isError", isError);
        outState.putInt("retryNumber", retryNumber);
        outState.putBoolean("gcmIsRegistered", gcmIsRegistered);
        outState.putBoolean("phoneIsLoaded", phoneIsLoaded);
        outState.putString("deviceId", deviceId);
        outState.putString("pushToken", pushToken);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        registerReceiver(true);
        if (countDownTimer == null) {
            countDownTimer = new InitCountDownTimer(countDownMillisLeft);
        }
        countDownStarted = System.currentTimeMillis();
        countDownTimer.start();
        if (checkPlayServices()) {
            startInit(false);
        }
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
        showProgress();
        if (!LaAccount.isSet() || !accountIsAdded()) {
            startActivityForResult(new Intent(this, AuthActivity.class), REQUEST_CODE_LOGIN);
            overridePendingTransition(0, 0);
            return;
        }
        else {
            phoneGet(forceReset);
        }
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
        if (!gcmIsRegistered) {
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
            if (gcmRegistrationBroadcastReceiver == null) {
                gcmRegistrationBroadcastReceiver = new GcmRegistrationBroadcastReceiver();
            }
            if (!isReceiverRegistered) {
                LocalBroadcastManager.getInstance(this).registerReceiver(gcmRegistrationBroadcastReceiver,
                        new IntentFilter(PushRegistrationIntentService.INTENT_REGISTRATION_COMPLETE));
                isReceiverRegistered = true;
            }
        }
        else {
            // unregistering receiver
            if (gcmRegistrationBroadcastReceiver != null) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(gcmRegistrationBroadcastReceiver);
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
     * Try to start main activity of the app. All you need is to be ready - that means everything included in IF condition should be loaded or executed
     */
    private void go() {
        if (countDownMillisLeft == 0
                && gcmIsRegistered
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
        isError = true;
        this.errorMsg = errorMsg;
        pbLoading.setVisibility(View.GONE);
        llButtons.setVisibility(View.VISIBLE);
        tvLoading.setText(errorMsg);
    }

    /**
     * Hide buttons, progressbar and error message when initiating starts
     */
    private void showProgress () {
        isError = false;
        this.errorMsg = "";
        pbLoading.setVisibility(View.VISIBLE);
        llButtons.setVisibility(View.GONE);
    }

    /**
     * This class is receiving local broadcast messages about GCM registration
     */
    private class GcmRegistrationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PushRegistrationIntentService.INTENT_REGISTRATION_COMPLETE.equals(intent.getAction())) {
                gcmIsRegistered = intent.getBooleanExtra(PushRegistrationIntentService.IS_REGISTRATION_SUCCESS, false);
                if (gcmIsRegistered) {
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
                InitActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            processResponse(data);
                        } catch (EmptyValueException | NullPointerException | JSONException | IOException e) {
                            showError(e.getMessage());
                            Logger.e(TAG, e);
                        }
                    }

                    private void processResponse(LoaderResult<Response> data) throws NullPointerException, EmptyValueException, IOException, JSONException {
                        if (data == null) {
                            throw new NullPointerException("Error: LoaderResult cannot be null");
                        }
                        if (data.getObject() == null) {
                            throw new NullPointerException("Error '" + data.getCode() + "': " + data.getMessage());
                        }
                        if (data.getObject().body() == null) {
                            throw new NullPointerException("Error: Missing body.");
                        }
                        final JSONObject object = ResponseProcessor.bodyToJson(data.getObject());
                        final Account account = LaAccount.get();
                        final AccountManager accountManager = AccountManager.get(InitActivity.this);
                        addAccountData(accountManager, account, object, LaAccount.USERDATA_PHONE_ID, "id", true);
                        addAccountData(accountManager, account, object, LaAccount.USERDATA_NUMBER, "number", false);
                        addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_HOST, "connection_host", true);
                        addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_USER, "connection_user", true);
                        addAccountData(accountManager, account, object, LaAccount.USERDATA_SIP_PASS, "connection_pass", true);
                        phoneIsLoaded = true;
                        if (object.has("params")) {
                            String paramsString = object.getString("params");
                            JSONObject paramsObj = new JSONObject(paramsString);
                            deviceId = paramsObj.getString("deviceId");
                            pushToken = paramsObj.getString("pushToken");
                        }
                        registerPushNotifications();

                    }

                    private void addAccountData(AccountManager accountManager, Account account, JSONObject object, String key, String objectKey, boolean required) throws EmptyValueException, JSONException {
                        if (required && (!object.has(objectKey) || TextUtils.isEmpty(object.getString(objectKey)))) {
                            throw new EmptyValueException(key);
                        }
                        else {
                            accountManager.setUserData(account, key, object.getString(objectKey));
                        }
                    }
                });
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
