package com.qualityunit.android.liveagentphone.ui.auth.login;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.qualityunit.android.liveagentphone.App;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.util.Tools;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by rasto on 11.11.16.
 */

public abstract class UrlTester implements Handler.Callback {

    private static final long LOOP_DELAY_MILLIS = 5000;
    private static final long ERROR_DELAY_MILLIS = 1500;
    private boolean isStopped;
    private Timer timer = new Timer("urlLoop");
    private TimerTask timerTask;
    private Activity activity;

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

    public UrlTester(Activity activity) {
        this.activity = activity;
    }

    public void test(final String typedUrl) {
        cancelTimerTask();
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
        currentThread = new UrlThread(handler, typedUrl, activity);
        currentThread.start();
    }

    private void loop() {
        cancelTimerTask();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                test(onGetTypedUrl());
            }
        };
        timer.schedule(timerTask, LOOP_DELAY_MILLIS);
    }

    private void cancelTimerTask () {
        if (timerTask != null) {
            timerTask.cancel();
        }
        timerTask = null;
    }

    public void stop() {
        cancelTimerTask();
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
        onUrlCallback(result.code, result.typedUrl, result.apiUrl, result.message);
        loop();
        return true;
    }

    protected abstract void onUrlCallback(int code, String typedUrl, String apiUrl, String message);

    protected abstract String onGetTypedUrl();

    private static class UrlThread extends Thread {

        private static final String TAG = UrlThread.class.getSimpleName();
        private volatile boolean isForget = false;
        private Handler handler;
        private final String typedUrl;
        private Activity activity;

        public UrlThread(Handler handler, String typedUrl, Activity activity) {
            this.handler = handler;
            this.typedUrl = typedUrl;
            this.activity = activity;
        }

        @Override
        public void run() {
            if (!Tools.isNetworkConnected(activity)) {
                post(CODE.NO_CONNECTION, typedUrl, 0, "No connection");
                return;
            }
            if (TextUtils.isEmpty(typedUrl) || typedUrl.equals("https://")) {
                post(CODE.URL_BLANK, typedUrl, 0, null);
                return;
            }
            if (!App.ALLOW_HTTP && !typedUrl.startsWith("https://")) {
                post(CODE.API_ERROR, typedUrl, ERROR_DELAY_MILLIS, "URL does not loop with 'https://'");
                return;
            }
            // do not do this while debug
            String fixedUrl = typedUrl;
            if (fixedUrl.endsWith("/")) {
                fixedUrl = fixedUrl.substring(0, fixedUrl.length() - 1);
            }
            if (fixedUrl.endsWith("/index.php")) {
                fixedUrl = fixedUrl.substring(0, fixedUrl.length() - "/index.php".length());
            }
            if (fixedUrl.endsWith("/agent")) {
                fixedUrl = fixedUrl.substring(0, fixedUrl.length() - "/agent".length());
            }
            if (!fixedUrl.endsWith(Const.Api.API_POSTFIX)) {
                fixedUrl = fixedUrl + Const.Api.API_POSTFIX;
            }
            startChecking(fixedUrl);
        }

        private void startChecking(final String basePath) {
            try {
                Log.d(TAG, "Testing URL: '" + basePath + "'");
                // testing URL validity
                if (!Tools.isUrlValid(basePath) || (!basePath.startsWith("http://") && !basePath.startsWith("https://"))) {
                    throw new MalformedURLException();
                }
                // testing ping request
                Client.ping(activity, basePath, new Client.Callback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject object) {
                        if (object != null && object.length() == 0) {
                            post(CODE.URL_OK, basePath, 0, "OK");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        post(CODE.URL_NOT_VALID, basePath, ERROR_DELAY_MILLIS, e.getMessage());
                    }
                });
            } catch (MalformedURLException e) {
                post(CODE.URL_NOT_VALID, basePath, ERROR_DELAY_MILLIS, e.getMessage());
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
