package com.qualityunit.android.liveagentphone.ui.init;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.ui.auth.AuthActivity;
import com.qualityunit.android.liveagentphone.ui.home.HomeActivity;
import com.qualityunit.android.liveagentphone.util.EmptyValueException;
import com.qualityunit.android.liveagentphone.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by rasto on 26.01.16.
 */
public class InitActivity extends AppCompatActivity {

    private static final String TAG = InitActivity.class.getSimpleName();
    private static final int REQUEST_CODE_LOGIN = 0;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int MAX_RETRY_NUMBER = 3;
    private TextView tvLoading;
    private LinearLayout pbLoading;
    private View llButtons;
    private Button bRetry;
    private BroadcastReceiver fcmRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;
    private int retryNumber;
    private boolean isFcmRegistered;
    private boolean isPhoneLoaded;
    private String deviceId;
    private String pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_activity);
        llButtons = findViewById(R.id.ll_buttons);
        tvLoading = findViewById(R.id.tv_loading_status);
        pbLoading = findViewById(R.id.pb_loading);
        bRetry =  findViewById(R.id.b_retry);
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
        View bQuit = findViewById(R.id.b_quit);
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
        startInit(false);
        super.onResume();
    }

    @Override
    protected void onPause() {
        registerReceiver(false);
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
        if (forceReset) {
            isPhoneLoaded = false;
            phoneGet();
            return;
        }
        if (isPhoneLoaded) {
            registerPushNotifications();
        } else {
            phoneGet();
        }
    }

    /**
     * Make a GET /phones/_app_ call to API
     */
    private void phoneGet() {
        Client.getPhone(this, new Client.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject object) {
                onPhoneGet(object);
            }

            @Override
            public void onFailure(Exception e) {
                showError(e.getMessage());
            }
        });
    }

    /**
     * Process success response of request GET /phones/_app_
     * @param obj
     */
    private void onPhoneGet(JSONObject obj) {
        try {
            final Account acc = LaAccount.get();
            final AccountManager accManager = AccountManager.get(InitActivity.this);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_PHONE_ID, "id", true);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_NUMBER, "number", false);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_SIP_HOST, "connection_host", true);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_SIP_USER, "connection_user", true);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_SIP_PASS, "connection_pass", true);
            addAccData(accManager, acc, obj, LaAccount.USERDATA_AGENT_ID, "agent_id", true);
            isPhoneLoaded = true;
            if (obj.has("params")) {
                String paramsString = obj.getString("params");
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

    private void addAccData(AccountManager accountManager, Account account, JSONObject object, String key, String objectKey, boolean required) throws EmptyValueException, JSONException {
        if (required && (!object.has(objectKey) || TextUtils.isEmpty(object.getString(objectKey)))) {
            throw new EmptyValueException(key);
        }
        else {
            accountManager.setUserData(account, key, object.getString(objectKey));
        }
    }

    private void registerPushNotifications() {
        if (!isFcmRegistered) {
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
        // Dangerous permissions: RECORD_AUDIO, READ_PHONE_STATE
        if (!checkOnePermission(RECORD_AUDIO, getString(R.string.permission_reason_handle_calls_properly))) {
            return false;
        } else if (!checkOnePermission(READ_PHONE_STATE, getString(R.string.permission_reason_read_phone_state))) {
            return false;
        }
        return true;
    }

    private boolean checkOnePermission(String permissionName, String reason) {
        if (ContextCompat.checkSelfPermission(InitActivity.this, permissionName) != PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            // returns false only if the user selected Never ask again
            if (ActivityCompat.shouldShowRequestPermissionRationale(InitActivity.this, permissionName)) {
                showPermissionNotification(permissionName, reason);
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(InitActivity.this, new String[]{ permissionName }, 1000);
            }
            return false;
        }
        return true;
    }

    private void showPermissionNotification(final String permissionName, final String reason) {
        AlertDialog alertDialog = new AlertDialog.Builder(InitActivity.this).create();
        alertDialog.setTitle(permissionName.replace("android.permission.", ""));
        alertDialog.setMessage(reason);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(InitActivity.this, new String[]{ permissionName }, 1000);
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d(TAG, "Permission '" + permissions[0] + "' granted: " + (grantResults[0] == PERMISSION_GRANTED));
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
        if (isFcmRegistered && isPhoneLoaded) {
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
                isFcmRegistered = intent.getBooleanExtra(PushRegistrationIntentService.IS_REGISTRATION_SUCCESS, false);
                if (isFcmRegistered) {
                    go();
                    return;
                }
                String errMsg = intent.getStringExtra(PushRegistrationIntentService.FAILURE_MESSAGE);
                if (!TextUtils.isEmpty(errMsg)) {
                    errMsg = getString(R.string.oops);
                }
                showError(errMsg);
            }
        }

    }

}
