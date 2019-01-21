package com.qualityunit.android.liveagentphone.net;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.qualityunit.android.liveagentphone.acc.LaAccount;

import java.io.IOException;

public class Client {

    private static Client instance;
    private RequestQueue queue;
    private ImageLoader imageLoader;
    private static Context context;

    private Client(Context context) {
        Client.context = context;
        VolleyLog.DEBUG = true;
        queue = getQueue();
        imageLoader = new ImageLoader(queue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<>(20);

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    private static synchronized Client getInstance(Context context) {
        if (instance == null) {
            instance = new Client(context);
        }
        return instance;
    }

    private RequestQueue getQueue() {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }

    public <T> void addToQueue(Request<T> request, String tag) {
        getQueue().cancelAll(tag);
        request.setTag(tag);
        getQueue().add(request);
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public static void prepare(final Activity activity, final AuthDataCallback callback) {
        final AccountManager accountManager = AccountManager.get(activity.getApplicationContext());
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String apikey = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    callback.onAuthData(getInstance(activity), basePath, apikey);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    callback.onException(e);
                }

            }
        }, handler);
    }

    public interface AuthDataCallback {
        void onAuthData(Client client, String basepath, String apikey);
        void onException(Exception e);
    }

}
