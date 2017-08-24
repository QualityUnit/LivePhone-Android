package com.qualityunit.android.liveagentphone.net.rest;

/**
 * Created by rasto on 6.10.16.
 */

public class ApiException extends Exception{

    private int code;

    public ApiException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
