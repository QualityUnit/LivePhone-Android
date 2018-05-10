package com.qualityunit.android.liveagentphone.util;

import android.os.Bundle;
import android.util.Log;

/**
 * Created by rasto on 11.02.16.
 */
public class Logger {

    private static final String TAG = Logger.class.getSimpleName();
    private static Logger instance;

    private static Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    private Logger() {
        Log.d(TAG, "Logger successfuly initialized");
    }

    public static void e(String TAG, Throwable e) {
        getInstance().logError(TAG, null, e, null);
    }

    public static void e(String TAG, Throwable e, Bundle additionalInfo) {
        getInstance().logError(TAG, null, e, additionalInfo);
    }

    public static void e(String TAG, String errMsg) {
        getInstance().logError(TAG, errMsg, null, null);
    }

    public static void e(String TAG, String errMsg, Bundle additionalInfo) {
        getInstance().logError(TAG, errMsg, null, additionalInfo);
    }

    public static void e(String TAG, String errMsg, Throwable e, Bundle additionalInfo) {
        getInstance().logError(TAG, errMsg, e, additionalInfo);
    }

    public static void e(String TAG, String errMsg, Throwable e) {
        getInstance().logError(TAG, errMsg, e, null);
    }

    public void logError(String TAG, String errMsg, Throwable e, Bundle additionalInfo) {
        if (errMsg == null) {
            errMsg = "";
        }
        if (additionalInfo != null) {
            errMsg = errMsg + "\n" + additionalInfo.toString();
        }
        if(e != null) {
            Log.e(TAG, errMsg, e);
        }
    }
}
