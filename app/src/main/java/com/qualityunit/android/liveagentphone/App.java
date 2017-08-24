package com.qualityunit.android.liveagentphone;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by rasto on 22.08.16.
 */
public class App extends Application {

    private static final String SHAREDPREFERENCES_NAME = "com.qualityunit.android.liveagentphone";
    private static Context context;
    private static Properties config;

    @Override
    public void onCreate(){
        super.onCreate();
        App.context = getApplicationContext();
        App.config = readConfigFile();
    }

    private Properties readConfigFile() {
        try {
            InputStream inputStream = getAssets().open("cfg.properties");
            Properties properties = new Properties();
            properties.load(inputStream);
            Log.d(App.class.getSimpleName(), "App configuration loaded successfully.");
            return properties;
        } catch (IOException e) {
            String errMsg = "Failed to load app configuration.";
            Log.e(App.class.getSimpleName(), errMsg);
            return new Properties(); // empty properties
        }
    }

    public static Context getAppContext() {
        return App.context;
    }

    public static Properties getAppConfig() {
        return App.config;
    }

    public static SharedPreferences getSharedPreferences() {
        return getAppContext().getSharedPreferences(SHAREDPREFERENCES_NAME, MODE_PRIVATE);
    }
}
