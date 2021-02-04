package com.qualityunit.android.liveagentphone.store;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.net.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.Store;
import com.qualityunit.android.liveagentphone.ui.home.InternalItem;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Retained fragment for handling extensions list
 * Created by rasto on 28.11.16.
 */
public class InternalStore implements Store<InternalItem>{

    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 9999;
    public static String TAG = InternalStore.class.getSimpleName();
    private static InternalStore instance;
    private InternalPaginationList ipl = new InternalPaginationList();

    public static InternalStore getInstance() {
        if (instance == null) {
            instance = new InternalStore();
        }
        return instance;
    }

    public static InternalStore createSearchInstance() {
        return new InternalStore();
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    public static void destroyInstances() {
        if (instance != null) {
            instance.setListener(null);
        }
        instance = null;
    }

    private InternalStore() {
    }

    // ******** API ************

    @Override
    public void init (Activity activity, int initFlag) {
        ipl.init(activity, initFlag, createArgs(null));
    }

    @Override
    public void reload(Activity activity) {
        ipl.init(activity, PaginationList.InitFlag.RELOAD, ipl.getCurrentState().getArgs());
    }

    @Override
    public void refresh (Activity activity) {
        ipl.refresh(activity);
    }

    @Override
    public void nextPage (Activity activity) {
        ipl.nextPage(activity);
    }

    @Override
    public void setListener(PaginationList.CallbackListener<InternalItem> callbackListener) {
        ipl.setListener(callbackListener);
    }

    @Override
    public void search(Activity activity, String searchTerm) {
        ipl.init(activity, PaginationList.InitFlag.RELOAD, createArgs(searchTerm));
    }

    @Override
    public void clear() {
        ipl.clear();
    }

    // ************ private methods ************

    private Bundle createArgs(@Nullable String searchTerm) {
        Bundle bundle = new Bundle();
        if (!TextUtils.isEmpty(searchTerm)) {
            bundle.putString("searchTerm", searchTerm);
        }
        return bundle;
    }

    // ********** INNER CLASSES ************

    private class InternalPaginationList extends PaginationList<InternalItem> {

        public InternalPaginationList() {
            super(FIRST_PAGE, ITEMS_PER_PAGE, "GET /extensions");
        }

        @Override
        public void requestPage(Activity activity, String requestTag, int pageToLoad, Bundle args, Client.Callback<List<InternalItem>> callback) {
            String searchTerm = args.getString("searchTerm");
            Client.getExtensions(activity, requestTag, searchTerm, pageToLoad, ITEMS_PER_PAGE, false, callback);
        }
    }
}
