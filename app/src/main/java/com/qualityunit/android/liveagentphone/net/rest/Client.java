package com.qualityunit.android.liveagentphone.net.rest;

import com.qualityunit.android.restful.RestClient;
import com.qualityunit.android.restful.method.RestGetBuilder;
import com.qualityunit.android.restful.method.RestPutBuilder;

/**
 * Created by rasto on 4.10.16.
 */

public class Client extends RestClient {

    public static final boolean VERIFY_SSL_CERTIFICATES = true;
    public static final int CONNECTION_TIMEOUT_SEC = 5;
    public static final int READ_WRITE_TIMEOUT_SEC = 20;
    private static Client instance;

    private Client() {
        super(CONNECTION_TIMEOUT_SEC, READ_WRITE_TIMEOUT_SEC, VERIFY_SSL_CERTIFICATES);
    }

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public RestGetBuilder GET (final String basePath, final String path, final String  token) {
        RestGetBuilder builder = new RestGetBuilder(basePath, path);
        builder.addHeader("apikey", token);
        builder.addHeader("Accept", "application/json");
        return builder;
    }

    public RestPutBuilder PUT (final String basePath, final String path, final String  token) {
        RestPutBuilder builder = new RestPutBuilder(basePath, path);
        builder.addHeader("apikey", token);
        builder.addHeader("Accept", "application/json");
        return builder;
    }

}
