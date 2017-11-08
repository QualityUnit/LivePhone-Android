package com.qualityunit.android.liveagentphone.net.loader;

import android.app.Activity;
import android.content.Context;

import com.qualityunit.android.liveagentphone.net.ApiCall;
import com.qualityunit.android.liveagentphone.net.rest.ApiException;

import java.io.IOException;

/**
 * Created by rasto on 28.01.16.
 */
public abstract class GenericLoader<T> extends BaseLoader<LoaderResult<T>, T> {

    private Activity activity;

    /**
     * @param activity from which is possible to run login screen
     * every time after orientation is changed. If you want to enable this re-delivering then set
     * this flag to true.
     */
    public GenericLoader(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public void loadInBackground() {
        ApiCall apiCall = new ApiCall(activity);
        apiCall.executeAuthorized(createApiCall());
    }

    protected ApiCall.PingPong createApiCall() {
        return new ApiCall.PingPong<T>() {
            @Override
            public T onRequest(String basePath, String token) throws IOException, ApiException {
                return requestData(basePath, token);
            }

            @Override
            public void onResponse(LoaderResult<T> response) {
                deliverResult(response);
            }
        };
    }

    protected abstract T requestData(String basePath, String token) throws IOException, ApiException;
}
