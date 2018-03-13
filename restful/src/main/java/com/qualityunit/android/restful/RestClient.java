package com.qualityunit.android.restful;

import com.qualityunit.common.android.restful.BuildConfig;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * RestClient</br>
 */

public class RestClient extends OkHttpClient {

    public RestClient(long connectTimeoutSeconds, long readWriteTimeoutSecond, boolean verifyingSsl) {
        super();
        if (BuildConfig.DEBUG) {
            RestfulLoggingInterceptor interceptor = new RestfulLoggingInterceptor();
            interceptor.setLevel(RestfulLoggingInterceptor.Level.BODY);
            this.interceptors().add(interceptor);
        }
        this.setConnectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS);
        this.setReadTimeout(readWriteTimeoutSecond, TimeUnit.SECONDS);
        this.setWriteTimeout(readWriteTimeoutSecond, TimeUnit.SECONDS);
        this.setFollowRedirects(true);
        this.setFollowSslRedirects(true);
        this.setProtocols(Arrays.asList(Protocol.HTTP_1_1));
    }

}
