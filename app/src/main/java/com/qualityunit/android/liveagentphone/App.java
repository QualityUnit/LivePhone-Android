package com.qualityunit.android.liveagentphone;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by rasto on 22.08.16.
 */
public class App extends Application {

    public static final boolean ALLOW_HTTP = BuildConfig.DEBUG;
    private static final String SHAREDPREFERENCES_NAME = "com.qualityunit.android.liveagentphone";
    private static Context context;

    @Override
    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(SHAREDPREFERENCES_NAME, MODE_PRIVATE);
    }
}
