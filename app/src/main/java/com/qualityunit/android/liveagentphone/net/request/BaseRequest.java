package com.qualityunit.android.liveagentphone.net.request;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.JsonRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseRequest<T> extends JsonRequest<T> {

    private final RetryPolicy retryPolicy;
    private final String apikey;

    public BaseRequest(int method, String url, @Nullable String apikey, @Nullable String requestBody, Response.Listener<T> listener, @Nullable Response.ErrorListener errorListener, boolean noRetry) {
        super(method, url, requestBody, listener, errorListener);
        this.apikey = apikey;
        retryPolicy = noRetry ? new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 0) : new DefaultRetryPolicy();
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
        return retryPolicy;
    }

}
