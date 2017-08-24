package com.qualityunit.android.restful.method;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rasto on 4.10.16.
 */

public abstract class RestBaseMethod {

    protected final String HEADER_USER_AGENT_VALUE = "QualityRest";
    protected final String HEADER_USER_AGENT_KEY = "User-Agent";
    protected String basePath;
    protected String path;
    protected Map<String,String> headers;
    protected Map<String,String> params;
    protected Map<String,String> encodedParams;

    public RestBaseMethod(String basePath, String path) {
        this.basePath = basePath;
        this.path = path;
        addHeader(HEADER_USER_AGENT_KEY, HEADER_USER_AGENT_VALUE);
    }

    public RestBaseMethod addHeader(String key, String value) {
        if (headers == null) { // when first header comes
            this.headers = new HashMap<>();
        }
        headers.put(key, value);
        return this;
    }

    public RestBaseMethod addEncodedParam(String key, Object value) {
        if (this.encodedParams == null) { // when first param comes
            this.encodedParams = new HashMap<>();
        }
        this.encodedParams.put(key, String.valueOf(value));
        return this;
    }

    public RestBaseMethod addParam(String key, Object value) {
        if (this.params == null) { // when first param comes
            this.params = new HashMap<>();
        }
        this.params.put(key, String.valueOf(value));
        return this;
    }

    public abstract RestBaseMethod setBody(String body);

    public Request build() {
        HttpUrl httpUrl = HttpUrl.parse(basePath + path);
        HttpUrl.Builder httpUrlbuilder = httpUrl.newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : this.params.entrySet()) {
                httpUrlbuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        if (encodedParams != null) {
            for (Map.Entry<String, String> entry : this.encodedParams.entrySet()) {
                httpUrlbuilder.addEncodedQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        httpUrl = httpUrlbuilder.build();
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(httpUrl);
        onSetMethod(requestBuilder);
        if (headers != null) {
            for (Map.Entry<String, String> entry : this.headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return requestBuilder.build();
    }

    protected abstract void onSetMethod(Request.Builder requestBuilder);
}
