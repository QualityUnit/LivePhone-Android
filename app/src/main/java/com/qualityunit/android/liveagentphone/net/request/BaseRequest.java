package com.qualityunit.android.liveagentphone.net.request;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseRequest<T> extends JsonRequest<T> {

    private static final int SOCKET_TIMEOUT = 5000;
    private static final RetryPolicy RETRY_POLICY = new DefaultRetryPolicy(
            SOCKET_TIMEOUT,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    private String apikey;

    public BaseRequest(int method, String url, @Nullable String apikey, @Nullable String requestBody, Response.Listener<T> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        this.apikey = apikey;
    }


    @Override
    public Map<String, String> getHeaders() {
        Map<String, String>  headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
//        headers.put("Accept", "application/json");
        if (!TextUtils.isEmpty(apikey)) {
            headers.put("apikey", apikey);
        }
        return headers;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return RETRY_POLICY;
    }

}
