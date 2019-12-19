package com.qualityunit.android.liveagentphone.acc;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.ui.auth.AuthActivity;

/**
 * Created by rasto on 22.01.16.
 */
public class LaAccountAuthenticator extends AbstractAccountAuthenticator {

    private static final String TAG = LaAccountAuthenticator.class.getSimpleName();
    private Context context;

    public LaAccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) {
        final Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(AuthActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
        return null;
    }

    @Override
    public Bundle getAuthToken(final AccountAuthenticatorResponse response, final Account account, String authTokenType, Bundle options)  {
        final AccountManager accountManager = AccountManager.get(context);
        final String authToken = accountManager.peekAuthToken(account, authTokenType);
        final String password = accountManager.getPassword(account);
        final String basepath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);

        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            return createResult(account, authToken);
        } else if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(basepath)) {
            Client.login(context, basepath, account.name, password, null, new Client.LoginCallback() {

                @Override
                public void onInvalidPassword() {
                    response.onResult(createLoginBundle(response, account));
                }

                @Override
                public void onVerificationCodeRequired() {
                    response.onResult(createLoginBundle(response, account));
                }

                @Override
                public void onVerificationCodeFailure() {
                    response.onResult(createLoginBundle(response, account));
                }

                @Override
                public void onTooManyLogins() {
                    response.onResult(createLoginBundle(response, account));
                }

                @Override
                public void onSuccess(String apikey, String apikeyId) {
                    accountManager.setUserData(LaAccount.get(), LaAccount.USERDATA_APIKEY_ID, apikeyId);
                    response.onResult(createResult(account, apikey));
                }

                @Override
                public void onFailure(Exception e) {
                    response.onResult(createLoginBundle(response, account));
                }
            });
            return null;
        }
        return createLoginBundle(response, account);
    }

    private Bundle createResult(Account account, String authToken) {
        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
        return result;
    }

    private Bundle createLoginBundle(final AccountAuthenticatorResponse response, Account account) {
        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        final Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AuthActivity.ARG_ACCOUNT_TYPE, account.type);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws NetworkErrorException {
        Bundle bundle = new Bundle(1);
        bundle.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        return bundle;
    }


}
