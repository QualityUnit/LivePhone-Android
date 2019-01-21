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
    private String apikey;

    public BaseRequest(int method, String url, String apikey, @Nullable String requestBody, Response.Listener<T> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        this.apikey = apikey;
    }


    @Override
    public Map<String, String> getHeaders() {
        Map<String, String>  headers = new HashMap<>();
        headers.put("Accept", "application/json");
        if (!TextUtils.isEmpty(apikey)) {
            headers.put("apikey", apikey);
        }
        return headers;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        DefaultRetryPolicy retryPolicy = new DefaultRetryPolicy(SOCKET_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        return retryPolicy;
    }

}
