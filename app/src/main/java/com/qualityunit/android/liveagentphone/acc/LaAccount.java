package com.qualityunit.android.liveagentphone.acc;

import android.accounts.Account;
import android.text.TextUtils;
import android.util.Log;

import com.qualityunit.android.liveagentphone.App;
import com.qualityunit.android.liveagentphone.Const;

/**
 * Created by rasto on 28.01.16.
 */
public class LaAccount {

    private static final String TAG = LaAccount.class.getSimpleName();
    public static final String ACCOUNT_TYPE = "com.qualityunit.android.liveagentphone";
    public static final String AUTH_TOKEN_TYPE = "access"; // hardcoded type - api don't use types
    public static final String USERDATA_URL_API = "USERDATA_URL_API";
    public static final String USERDATA_URL_TYPED = "USERDATA_URL_TYPED";
    public static final String USERDATA_PHONE_ID = "USERDATA_PHONE_ID";
    public static final String USERDATA_NUMBER = "USERDATA_NUMBER";
    public static final String USERDATA_SIP_HOST = "USERDATA_SIP_HOST";
    public static final String USERDATA_SIP_USER = "USERDATA_SIP_USER";
    public static final String USERDATA_SIP_PASS = "USERDATA_SIP_PASS";
    public static final String USERDATA_AGENT_ID = "USERDATA_AGENT_ID";
    public static final String USERDATA_DEVICE_ID = "USERDATA_DEVICE_ID";
    private static Account account;

    /**
     * Get the current account in use. If account is not set then return null.
     * @return account
     */
    public static Account get() {
        if (account != null) {
            return account;
        }
        String currentAccount = App.getSharedPreferences().getString(Const.MemoryKeys.CURRENT_ACCOUNT, "");
        if (TextUtils.isEmpty(currentAccount)) {
            return null; // account not set for application
        }
        setAccount(currentAccount, ACCOUNT_TYPE);
        return account;
    }

    /**
     * Set account and return it.
     * @param name
     * @param type
     * @return account
     */
    public static Account setAccount(String name, String type) {
        if (account == null || !name.equals(account.name)) {
            account = new Account(name, type);
            App.getSharedPreferences().edit().putString(Const.MemoryKeys.CURRENT_ACCOUNT, name).apply();
            Log.d(TAG, "New current account has been set.\naccount name = '" + name + "'\naccount type = '" + type + "'");
        }
        return account;
    }

    /**
     * Unset current account
     */
    public static void unset() {
        App.getSharedPreferences().edit().remove(Const.MemoryKeys.CURRENT_ACCOUNT).commit();
        account = null;
    }

    /**
     * Get true if is any account set as current account.
     * @return
     */
    public static boolean isSet() {
        if (account != null || !TextUtils.isEmpty(App.getSharedPreferences().getString(Const.MemoryKeys.CURRENT_ACCOUNT, ""))) {
            return true;
        } else {
            return false;
        }
    }
}
