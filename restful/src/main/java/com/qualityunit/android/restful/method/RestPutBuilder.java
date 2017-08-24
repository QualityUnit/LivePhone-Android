package com.qualityunit.android.restful.method;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

/**
 * Created by rasto on 4.10.16.
 */

public class RestPutBuilder extends RestBaseMethod {

    private static String TAG = RestPutBuilder.class.getSimpleName();
    private String body;

    public RestPutBuilder(String basePath, String path) {
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
        requestBuilder.put(RequestBody.create(MediaType.parse("application/json"), body));
    }

}
