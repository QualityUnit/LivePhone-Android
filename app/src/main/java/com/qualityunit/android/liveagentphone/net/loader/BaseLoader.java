package com.qualityunit.android.liveagentphone.net.loader;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;

/**
 * Created by rasto
 * Base Loader to retrieve data from server asynchronously
 */
public abstract class BaseLoader<Result extends BaseLoaderResult<Type>, Type> extends Loader<Result> {

    private static final String TAG = BaseLoader.class.getSimpleName();
    private HandlerThread handlerThread;
    private Handler handler;
    private boolean isInitiated;
    private Runnable backgroundTask;
    private LoadingListener loadListener;

    /**
     * @param context
     * every time after orientation is changed. If you want to enable this re-delivering then set
     * this flag to true.
     */
    public BaseLoader(Context context) {
        super(context);
        handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void deliverResult(Result data) {
        if (isStarted()) {
            if (loadListener != null) {
                loadListener.onDeliver(this);
            }
            super.deliverResult(data);
        }
    }

    @Override
    protected void onStartLoading() {
        if (takeContentChanged() || !isInitiated) {
            isInitiated = true;
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        if (handler != null) {
            handler.removeCallbacks(backgroundTask);
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        isInitiated = false;
    }

    @Override
    protected void onForceLoad() {
        cancelLoad();
        backgroundTask = new Runnable() {

            @Override
            public void run() {
                loadInBackground();
            }

        };
        handler.post(backgroundTask);
    }

    @Override
    protected void onAbandon() {
        handlerThread.quitSafely();
        handlerThread = null;
    }

    public abstract void loadInBackground();

    public void setOnLoadListener(@Nullable LoadingListener loadListener) {
        this.loadListener = loadListener;
    }

    public interface LoadingListener<Result extends BaseLoaderResult<Type>, Type> {
        void onDeliver(Loader<Result> loader);
    }

}