package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;

import java.util.List;

public abstract class SearchChildAdapter<T> {

    protected Context context;
    private final String title;
    private final List<T> list;
    protected final String baseUrl;
    protected final LayoutInflater inflater;
    protected final PaginationList.State state;
    private OnClickListener<T> onClickListener;

    public SearchChildAdapter(@NonNull Context context, @NonNull String title, @NonNull List<T> list, @Nullable PaginationList.State state, OnClickListener<T> onClickListener) {
        this.context = context;
        this.title = title;
        this.list = list;
        this.inflater = LayoutInflater.from(context);
        this.state = state;
        this.onClickListener = onClickListener;
        AccountManager accountManager = AccountManager.get(context);
        this.baseUrl = accountManager
                .getUserData(LaAccount.get(), LaAccount.USERDATA_URL_API)
                .replace(Const.Api.API_POSTFIX, "");
    }

    public abstract View getChildView(int position, boolean isLast, View convertView, ViewGroup parent, PaginationList.State state);

    public PaginationList.State getState() {
        return state;
    }

    public String getTitle() {
        if (getState() == null || getState().isLoading() || getState().isRefreshing()) {
            return title;
        }
        int size = getList().size();
        return title + " (" + size + (size == 0 || getState().isLastPage() ? "" : "+") + ")";
    }

    public List<T> getList() {
        return list;
    }

    public OnClickListener<T> getOnClickListener() {
        return onClickListener;
    }

    public interface OnClickListener<T> {
        boolean onChildClick(T item);
    }
}
