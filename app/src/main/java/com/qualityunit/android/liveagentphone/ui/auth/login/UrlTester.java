package com.qualityunit.android.liveagentphone.ui.auth.login;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.qualityunit.android.restful.method.RestGetBuilder;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by rasto on 11.11.16.
 */

public abstract class UrlTester implements Handler.Callback {

    private static final long LOOP_DELAY_MILLIS = 5000;
    private static final long ERROR_DELAY_MILLIS = 1500;
    private boolean isStopped;
    private String typedUrl;

    public static final class CODE {
        public static final int URL_OK = 0;
        public static final int NO_CONNECTION = 1;
        public static final int URL_NOT_VALID = 2;
        public static final int COULD_NOT_REACH_HOST = 3;
        public static final int API_ERROR = 4; // here, show resp message
        public static final int URL_BLANK = 5;
    }
    private Handler handler = new Handler(this);
    private UrlThread currentThread;

    public void test(final String typedUrl) {
        this.typedUrl = typedUrl;
        isStopped = false;
        handler.removeCallbacksAndMessages(null);
        if (currentThread != null) {
            currentThread.forget();
        }
        if (TextUtils.isEmpty(typedUrl)) {
            Message msg = new Message();
            msg.obj = new Result(CODE.URL_BLANK, "", "", "");
            handler.sendMessageDelayed(msg, 0);
            return;
        }
        currentThread = new UrlThread(handler, typedUrl);
        currentThread.start();
    }

    public void stop() {
        handler.removeCallbacksAndMessages(null);
        if (currentThread != null) {
            currentThread.forget();
        }
        isStopped = true;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (isStopped) {
            return true;
        }
        final Result result = (Result) msg.obj;
        if (result.code == CODE.NO_CONNECTION) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    test(typedUrl);
                }
            }, LOOP_DELAY_MILLIS);
        }
        onUrlCallback(result.code, result.typedUrl, result.apiUrl, result.message);
        return true;
    }

    protected abstract void onUrlCallback(int code, String typedUrl, String apiUrl, String message);

    private static class UrlThread extends Thread {

        private static final String TAG = UrlThread.class.getSimpleName();
        private volatile boolean isForget = false;
        private Handler handler;
        private final String typedUrl;

        public UrlThread(Handler handler, String typedUrl) {
            this.handler = handler;
            this.typedUrl = typedUrl;
        }

        @Override
        public void run() {
            if (!Tools.isNetworkConnected()) {
                post(CODE.NO_CONNECTION, typedUrl, 0, "No connection");
                return;
            }
            if (TextUtils.isEmpty(typedUrl) || typedUrl.equals("https://")) {
                post(CODE.URL_BLANK, typedUrl, 0, null);
                return;
            }
            String fixedUrl = typedUrl;
            if (fixedUrl.endsWith("/")) {
                fixedUrl = fixedUrl.substring(0, typedUrl.length() - 1);
            }
            if (!fixedUrl.endsWith(Const.Api.API_POSTFIX)) {
                fixedUrl = fixedUrl + Const.Api.API_POSTFIX;
            }
            if (!fixedUrl.startsWith("https://")) {
                post(CODE.API_ERROR, fixedUrl, ERROR_DELAY_MILLIS, "URL does not start with 'https://'");
            }
            startChecking(fixedUrl);
        }

        private void startChecking(String url) {
            try {
                Log.d(TAG, "Testing URL: '" + url + "'");
                // testing URL validity
                if (!Tools.isUrlValid(url)) {
                    throw new MalformedURLException("Url do not match URL pattern");
                }
                Log.d(TAG, "URL is valid");
                // testing ping request
                final Client client = Client.getInstance();
                final Request request = new RestGetBuilder(url, "/ping")
                        .addHeader("Accept", "application/json")
                        .build();
                Call call = client.newCall(request);
                Response resp = call.execute();
                if (resp.isSuccessful() && "{}".equals(resp.body().string())) {
                    post(CODE.URL_OK, url, 0, "OK");
                } else {
                    post(CODE.API_ERROR, url, ERROR_DELAY_MILLIS, resp.message());
                }
            } catch (MalformedURLException e) {
                post(CODE.URL_NOT_VALID, url, ERROR_DELAY_MILLIS, e.getMessage());
            } catch (IOException e) {
                post(CODE.COULD_NOT_REACH_HOST, url, ERROR_DELAY_MILLIS, e.getMessage());
            }

        }

        private void post(final int code, final String apiUrl, final long delayMillis, String message) {
            if (isForget) {
                return;
            }
            Message msg = new Message();
            msg.obj = new Result(code, typedUrl, apiUrl, message);
            handler.sendMessageDelayed(msg, delayMillis);
        }

        private void forget() {
            isForget = true;
        }

    }

    private static class Result {

        public Result(int code, String typedUrl, String apiUrl, String message) {
            this.code = code;
            this.typedUrl = typedUrl;
            this.apiUrl = apiUrl;
            this.message = message;
        }

        private int code;
        private String typedUrl;
        private String apiUrl;
        private String message;
    }

}
