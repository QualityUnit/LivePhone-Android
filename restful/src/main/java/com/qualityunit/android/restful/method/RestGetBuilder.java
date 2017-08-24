package com.qualityunit.android.restful.method;

import android.util.Log;

import com.squareup.okhttp.Request;

/**
 * Created by rasto on 4.10.16.
 */

public class RestGetBuilder extends RestBaseMethod {

    private static String TAG = RestGetBuilder.class.getSimpleName();

    public RestGetBuilder(String basePath, String path) {
        super(basePath, path);
    }

    @Override
    public RestBaseMethod setBody(String body) {
        Log.e(TAG, "Cannot set body to GET request!");
        return this;
    }

    @Override
    protected void onSetMethod(Request.Builder requestBuilder) {
        requestBuilder.get();
    }

}
