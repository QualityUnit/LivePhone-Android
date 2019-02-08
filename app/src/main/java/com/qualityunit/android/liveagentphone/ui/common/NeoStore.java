package com.qualityunit.android.liveagentphone.ui.common;

import android.app.Activity;

import com.qualityunit.android.liveagentphone.net.PaginationList;

public interface NeoStore<T> {
    void init(Activity activity, String basePath, String token, int initFlag);
    void reload(Activity activity);
    void refresh(Activity activity);
    void nextPage(Activity activity);
    void setListener(PaginationList.CallbackListener<T> callbackListener);
    void search(Activity activity, String searchTerm);
    void clear();
}
