package com.qualityunit.android.restful.method;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

/**
 * Created by rasto on 4.10.16.
 */

public class RestPostBuilder extends RestBaseMethod {

    private static String TAG = RestPostBuilder.class.getSimpleName();
    private String body;

    public RestPostBuilder(String basePath, String path) {
        super(basePath, path);
    }

    @Override
    public RestBaseMethod setBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    protected void onSetMethod(Request.Builder requestBuilder) {
        if (body == null) {
            body = "";
        }
        requestBuilder.post(RequestBody.create(MediaType.parse("application/json"), body));
    }

}
