package com.qualityunit.android.liveagentphone.net.loader;

import android.os.Bundle;

/**
 * Created by rasto on 30.01.16.
 */
public class BaseLoaderResult<T> {

    private T object;
    private final Bundle bundle = new Bundle();

    public BaseLoaderResult(T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public String toString() {
        return "BaseLoaderResult{" +
                "object=" + object +
                ", bundle=" + bundle +
                '}';
    }
}