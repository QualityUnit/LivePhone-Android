package com.qualityunit.android.liveagentphone.net.loader;

/**
 * Created by rasto on 30.01.16.
 */
public class LoaderResult<T> extends BaseLoaderResult<T> {


    public static final String ARG_BUNDLE_CODE = "code";
    public static final String ARG_BUNDLE_MESSAGE = "message";

    public LoaderResult(T object, int code) {
        this(object, code, null);
    }

    public LoaderResult(T object, int code, String message) {
        super(object);
        getBundle().putInt(ARG_BUNDLE_CODE, code);
        getBundle().putString(ARG_BUNDLE_MESSAGE, message);
    }

    public int getCode () {
        return getBundle().getInt(ARG_BUNDLE_CODE);
    }

    public String getMessage () {
        return getBundle().getString(ARG_BUNDLE_MESSAGE);
    }

    @Override
    public String toString() {
        return "LoaderResult{} " + super.toString();
    }
}
