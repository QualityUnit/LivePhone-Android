package com.qualityunit.android.liveagentphone.ui.home;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.net.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.Store;

import java.util.List;

public abstract class SearchSection<T> implements PaginationList.CallbackListener<T> {

    private static final String TAG = SearchSection.class.getSimpleName();
    protected Activity activity;
    protected Store<T> store;
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
        store.search(activity, searchTerm);
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
        store.init(activity, PaginationList.InitFlag.INSTANCE);
    }

    public void reload() {
        store.reload(activity);
    }

    public void clear() {
        store.clear();
    }
}
