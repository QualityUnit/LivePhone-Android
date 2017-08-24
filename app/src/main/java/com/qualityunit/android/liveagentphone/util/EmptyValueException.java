package com.qualityunit.android.liveagentphone.util;

/**
 * Created by rasto on 16.09.16.
 */
public class EmptyValueException extends Exception {

    public EmptyValueException(String key) {
        super("Value '" + key + "' not supposed to be empty.");
    }
}
