package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.store.InternalStore;
import com.qualityunit.android.liveagentphone.ui.call.InitCallActivity;
import com.qualityunit.android.liveagentphone.ui.common.PaginationScrollListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * Created by rasto on 8.11.17.
 */

public class InternalFragment extends Fragment implements AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener, PaginationList.CallbackListener<InternalItem>,
        PaginationScrollListener.OnNextPageListener {

    public static final String TAG = InternalFragment.class.getSimpleName();
    public static final int VISIBLE_THRESHOLD = 30;

    private ListView listView;
    private InternalListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar pbLoading;
    private InternalStore store;
    private TextView tvEmpty;
    // instance variables
    private int scrollIndex;
    private int scrollTop;
    private String searchTerm;
    private boolean isLastPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.internal_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEmpty = (TextView) view.findViewById(R.id.tv_empty);
        tvEmpty.setVisibility(GONE);
        pbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_list);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);
        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setAdapter(getAdapter());
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(new PaginationScrollListener(this, VISIBLE_THRESHOLD));
        if (savedInstanceState != null) {
            scrollIndex = savedInstanceState.getInt("scrollIndex", 0);
            scrollTop = savedInstanceState.getInt("scrollTop", 0);
            searchTerm = savedInstanceState.getString("searchTerm", "");
            isLastPage = savedInstanceState.getBoolean("isLastPage", false);
        }
        store = InternalStore.getInstance();
        store.setListener(this);
        init();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("scrollIndex", scrollIndex);
        outState.putInt("scrollTop", scrollTop);
        outState.putString("searchTerm", searchTerm);
        outState.putBoolean("isLastPage", isLastPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (store != null) {
            store.setListener(null);
        }
        scrollIndex = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        scrollTop = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
        super.onDestroy();
    }

    private void init () {
        final AccountManager accountManager = AccountManager.get(getContext());
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, getActivity(), new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    store.init(basePath, token, PaginationList.InitFlag.LOAD);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    Log.e(TAG, "", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, handler);
    }

    private InternalListAdapter getAdapter() {
        if (adapter == null) {
            adapter = new InternalListAdapter(getContext(), new ArrayList<InternalItem>());
        }
        return adapter;
    }

    private void refillList(List<InternalItem> list) {
        if (list != null) {
            getAdapter().setNotifyOnChange(false);
            getAdapter().clear();
            getAdapter().addAll(list);
            getAdapter().notifyDataSetChanged();
            if (scrollIndex != 0 && scrollTop != 0) {
                listView.setSelectionFromTop(scrollIndex, scrollTop);
                scrollIndex = 0;
                scrollTop = 0;
            }
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        pbLoading.setVisibility(View.GONE);
        if (store != null) {
            store.refresh();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        InitCallActivity.initInternalCall(getContext(), getAdapter().getItem(position));
//        CallingCommands.makeCall(getContext(), item.number, "", remoteName);
    }

    @Override
    public void onNextPage() {
        if (isLastPage) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        pbLoading.setVisibility(View.GONE);
        if (store != null) {
            store.nextPage();
        }
    }

    @Override
    public void onGetList (final List<InternalItem> list, final PaginationList.State listState) {
        isLastPage = listState.isLastPage();
        tvEmpty.setVisibility(listState.isEmpty() ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(listState.isRefreshing());
        pbLoading.setVisibility(listState.isLoading() ? View.VISIBLE : View.GONE);
        if (!listState.isLoading() && !listState.isRefreshing()) {
            swipeRefreshLayout.setEnabled(true);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }
        refillList(list);
    }

    @Override
    public void onError(@NonNull List<InternalItem> list, @Nullable String errorMessage, @NonNull PaginationList.State listState) {
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
    }
}
