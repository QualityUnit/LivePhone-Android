package com.qualityunit.android.liveagentphone.ui.common;

import com.qualityunit.android.liveagentphone.net.loader.PaginationList;

public interface Store<T> {
    void init (String basePath, String token, int initFlag);
    void reload();
    void refresh ();
    void nextPage ();
    void setListener(PaginationList.CallbackListener<T> callbackListener);
    void search (String searchTerm);
    void clear();
}
