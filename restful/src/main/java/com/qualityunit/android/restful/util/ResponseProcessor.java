package com.qualityunit.android.restful.util;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by rasto on 5.10.16.
 */

public abstract class ResponseProcessor {

    private static final String TAG = ResponseProcessor.class.getSimpleName();

    public interface ResponseCallback {
        void onSuccess(JSONObject object);
        void onFailure(Exception e);
    }

    public static void bodyToJson(final Response response, final ResponseCallback responseCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (response == null) {
                        throw new NullPointerException("Error: Response is null.");
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new NullPointerException("Error: Response does not contain body.");
                    }
                    JSONObject obj = new JSONObject(body.string());
                    responseCallback.onSuccess(obj);
                } catch (IOException | JSONException | NullPointerException e) {
                    responseCallback.onFailure(e);
                }
            }
        }).start();
    }

//    public static String errorMessage(Response response) throws IOException {
//        String message;
//        try {
//            JSONObject jsonObject = bodyToJson(response);
//            message = jsonObject.getString("message");
//        } catch (JSONException e) {
//            message = response.message();
//        }
//        return message;
//    }
}
