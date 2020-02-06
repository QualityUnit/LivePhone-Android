package com.qualityunit.android.liveagentphone.store;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.net.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.Store;
import com.qualityunit.android.liveagentphone.ui.home.ContactsItem;

import java.util.List;

/**
 * Retained fragment for handling contact list
 * Created by rasto on 28.11.16.
 */
public class ContactsStore implements Store<ContactsItem> {

    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 100;
    public static final String SORT_DIRECTION = Const.SortDir.ASCENDING;
    public static final String SORT_FIELD = "lastname";
    public static String TAG = ContactsStore.class.getSimpleName();
    private static ContactsStore instance;
    private ContactPaginationList cpl = new ContactPaginationList();

    public static ContactsStore getInstance() {
        if (instance == null) {
            instance = new ContactsStore();
        }
        return instance;
    }

    public static boolean hasInstance() {
        return instance != null;
    }

    public static void destroyInstance() {
        if (instance != null) {
            instance.setListener(null);
        }
        instance = null;
    }

    private ContactsStore() {
    }

    // ******** API ************

    @Override
    public void setListener(@Nullable PaginationList.CallbackListener<ContactsItem> callbackListener) {
        cpl.setListener(callbackListener);
    }

    @Override
    public void clear() {
        cpl.clear();
    }

    @Override
    public void init(Activity activity, int initFlag) {
        cpl.init(activity, initFlag, createArgs(null));
    }

    @Override
    public void reload(Activity activity) {
        cpl.init(activity, PaginationList.InitFlag.RELOAD, cpl.getCurrentState().getArgs());
    }

    @Override
    public void refresh(Activity activity) {
        cpl.refresh(activity);
    }

    @Override
    public void nextPage(Activity activity) {
        cpl.nextPage(activity);
    }

    @Override
    public void search(Activity activity, String searchTerm) {
        cpl.init(activity, PaginationList.InitFlag.RELOAD, createArgs(searchTerm));
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

    private class ContactPaginationList extends PaginationList<ContactsItem> {

        public ContactPaginationList() {
            super(FIRST_PAGE, ITEMS_PER_PAGE, "GET /contacts");
        }

        @Override
        public void requestPage(Activity activity, String requestTag, int pageToLoad, Bundle args, Client.Callback<List<ContactsItem>> callback) {
            String searchTerm = args.getString("searchTerm");
            Client.getContacts(activity, requestTag, searchTerm, pageToLoad, ITEMS_PER_PAGE, SORT_DIRECTION, SORT_FIELD, callback);
        }
    }
}
