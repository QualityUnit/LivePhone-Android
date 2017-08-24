package com.qualityunit.android.liveagentphone;

/**
 * Created by rasto on 30.01.16.
 */
public class ErrorCode {
    public static final int OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int CANNOT_PARSE_RESPONSE = -5;
    public static final int MISSING_VALUE_IN_RESPONSE = -6;
    public static final int UNKNOWN_ERROR = -4;
    public static final int AUTHENTICATOR_ERROR = -3;
    public static final int NETWORK_ERROR = -2;
    public static final int NO_CONNECTION = -1;
}
