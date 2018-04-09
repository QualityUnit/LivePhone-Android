package com.qualityunit.android.liveagentphone.net;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.qualityunit.android.liveagentphone.ErrorCode;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.LoaderResult;
import com.qualityunit.android.liveagentphone.net.rest.ApiException;
import com.qualityunit.android.liveagentphone.ui.init.InitActivity;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


/**
 * Created by rasto on 22.01.16.
 */
public class ApiCall<T> {

    private static final String TAG = ApiCall.class.getSimpleName();
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"; //2015-12-07 04:48:28
    private Account account;
    private AccountManager accountManager;
    private Activity activity;
    private String url;
    private PingPong pingPong;
    private boolean reCalledAuthorize;

    public ApiCall(Activity activity) {
        this.activity = activity;
        account = LaAccount.get();
        assert account != null;
        accountManager = AccountManager.get(activity);
    }

    public void executeAuthorized(final PingPong<Response> pingPong) {
        if (!Tools.isNetworkConnected()) {
            pingPong.onResponse(new LoaderResult<Response>(null, ErrorCode.NO_CONNECTION, activity.getString(com.qualityunit.android.liveagentphone.R.string.no_connection)));
            return;
        }
        this.pingPong = pingPong;
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle result = future.getResult();
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String token = result.getString(AccountManager.KEY_AUTHTOKEN);
                    makeRequest(basePath, token);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    Log.e(TAG, "", e);
                    pingPong.onResponse(new LoaderResult<Response>(null, ErrorCode.AUTHENTICATOR_ERROR, e.getMessage()));
                }

            }
        }, handler);

    }

    private void makeRequest(final String url, final String token) {
        this.url = url;
        try {
            if (TextUtils.isEmpty(url)) {
                removeAccountAndShowLogin();
            } else if (TextUtils.isEmpty(token)) {
                makeReCallAuthorized(ErrorCode.UNAUTHORIZED, "Empty token", token);
            } else {
                Response resp = (Response) pingPong.onRequest(url, token);
                if ((resp.code() / 100) != 2) {
                    // check if body contains 'message'
                    JSONObject object;
                    String message = null;
                    try {
                        object = new JSONObject(resp.body().string());
                        message = object.getString("message");
                    } catch (JSONException e) {
                        // do nothing - response does not contain body
                    }
                    if (TextUtils.isEmpty(message)) {
                        message = resp.message();
                    }
                    throw new ApiException(message, resp.code());
                }
                pingPong.onResponse(new LoaderResult(resp, resp.code()));
            }
        } catch (ApiException e) {
            int httpCode = e.getCode();
            String httpMessage = e.getMessage();
            switch (httpCode) {
                case ErrorCode.UNAUTHORIZED:
                case ErrorCode.FORBIDDEN:
                    makeReCallAuthorized(httpCode, httpMessage, token);
                    break;
                case ErrorCode.NOT_FOUND:
                    pingPong.onResponse(new LoaderResult(null, httpCode, httpMessage));
                    break;
                default:
                    showAndLogError(httpCode, httpMessage, null);
                    pingPong.onResponse(new LoaderResult(null, httpCode, httpMessage));
            }
        } catch (IOException e) {
            pingPong.onResponse(new LoaderResult(null, ErrorCode.NETWORK_ERROR, e.getMessage()));
        }
    }

    private void makeReCallAuthorized(int httpCode, String httpMessage, String token) {
        if (reCalledAuthorize) {
            // if calling was fired second time with invalidating token then credetials are not valid
            if (activity != null) {
                removeAccountAndShowLogin();
            } else {
                pingPong.onResponse(new LoaderResult(null, httpCode, httpMessage));
            }
        }
        else {
            reCalledAuthorize = true;
            accountManager.invalidateAuthToken(account.type, token);
            executeAuthorized(pingPong); // executeAuthorized again
        }
    }

    private void removeAccountAndShowLogin() {
        if (accountManager == null || activity == null) {
            return;
        }
        // if call is from activity, then finish activity and restart app into init
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
            @Override
            public void run(AccountManagerFuture<Boolean> future) {
                LaAccount.unset();
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.finish();
                        activity.startActivity(new Intent(activity, InitActivity.class));
                    }
                });
            }
        }, handler);
    }

    private void showAndLogError (int errorCode, String errorMessage, @Nullable Throwable e) {
        Logger.e(TAG, errorMessage, e, createLogBundle(errorCode, errorMessage));
    }

    private Bundle createLogBundle(int errorCode, String errorMessage) {
        Bundle additionalInfo = new Bundle();
        additionalInfo.putString("versionCode", String.valueOf(Tools.getVersionCode()));
        additionalInfo.putString("errorCode", String.valueOf(errorCode));
        additionalInfo.putString("errorMessage", errorMessage);
        additionalInfo.putString("apiBasePath", url);
        return additionalInfo;
    }

    public interface PingPong<T> {
        T onRequest(String basePath, String token) throws IOException, ApiException;
        void onResponse(LoaderResult<T> response);
    }

}
