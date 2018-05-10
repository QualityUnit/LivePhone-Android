package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.Store;

import java.io.IOException;
import java.util.List;

public abstract class SearchSection<T> implements PaginationList.CallbackListener<T> {

    private static final String TAG = SearchSection.class.getSimpleName();
    private Activity activity;
    private Store<T> store;
    private SearchParentAdapter adapter;
    private int indexInParentAdapter;

    public SearchSection(Activity activity, Store<T> store, SearchParentAdapter adapter, int indexInParentAdapter) {
        this.activity = activity;
        this.store = store;
        this.adapter = adapter;
        this.indexInParentAdapter = indexInParentAdapter;
        store.setListener(this);
    }

    /**
     * Get fresh list from store with new list state - now update the UI
     * @param list
     * @param listState
     * @param adapter ExpandableListView adapter
     * @param indexInParentAdapter
     */
    protected abstract void onRefillList(final List<T> list, final PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter);

    /**
     * Get list with error list state - show it to user
     * @param list
     * @param listState
     * @param adapter
     * @param indexInParentAdapter
     */
    protected abstract void onShowError(List<T> list, PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter);

    public void search(String searchTerm) {
        store.search(searchTerm);
    }

    @Override
    public void onGetList (@NonNull final List<T> list, @NonNull final PaginationList.State listState) {
        if (activity == null || activity.isDestroyed()) {
            return;
        }
        onRefillList(list, listState, adapter, indexInParentAdapter);
    }

    @Override
    public void onError(@NonNull final List<T> list, String errorMessage, final PaginationList.State listState) {
        if (activity == null || activity.isDestroyed()) {
            return;
        }
        onShowError(list, listState, adapter, indexInParentAdapter);
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
    }

    public void init() {
        final AccountManager accountManager = AccountManager.get(activity);
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    store.init(basePath, token, PaginationList.InitFlag.INSTANCE);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    Log.e(TAG, "", e);
                    Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, handler);
    }

    public void reload() {
        store.reload();
    }

    public void clear() {
        store.clear();
    }
}
