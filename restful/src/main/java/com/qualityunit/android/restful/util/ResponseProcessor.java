package com.qualityunit.android.restful.util;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by rasto on 5.10.16.
 */

public abstract class ResponseProcessor {

    private static final String TAG = ResponseProcessor.class.getSimpleName();

    public static JSONObject bodyToJson(Response response) throws IOException, JSONException {
        if (response == null) {
            throw new NullPointerException("Error: Response is null.");
        }
        if (response.body() == null) {
            throw new NullPointerException("Error: Response does not contain body.");
        }
        return new JSONObject(response.body().string());
    }

    public static String errorMessage(Response response) throws IOException {
        String message;
        try {
            JSONObject jsonObject = bodyToJson(response);
            message = jsonObject.getString("message");
        } catch (JSONException e) {
            message = response.message();
        }
        return message;
    }
}
