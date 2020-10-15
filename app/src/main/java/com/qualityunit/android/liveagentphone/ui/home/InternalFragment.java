package com.qualityunit.android.liveagentphone.ui.home;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.PaginationList;
import com.qualityunit.android.liveagentphone.store.InternalStore;
import com.qualityunit.android.liveagentphone.ui.call.InitCallActivity;
import com.qualityunit.android.liveagentphone.ui.common.PaginationScrollListener;

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
    private TextView tvEmpty;
    // instance variables
    private int scrollIndex;
    private int scrollTop;
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
        InternalStore.getInstance().setListener(this);
        init();
    }

    @Override
    public void onDestroy() {
        if (InternalStore.hasInstance()) {
            InternalStore.getInstance().setListener(null);
        }
        scrollIndex = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        scrollTop = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
        super.onDestroy();
    }

    private void init () {
        InternalStore.getInstance().init(getActivity(), PaginationList.InitFlag.LOAD);
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
        InternalStore.getInstance().refresh(getActivity());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        InitCallActivity.initInternalCall(getContext(), getAdapter().getItem(position));
    }

    @Override
    public void onNextPage() {
        if (isLastPage) {
            return;
        }
        swipeRefreshLayout.setRefreshing(true);
        pbLoading.setVisibility(View.GONE);
        InternalStore.getInstance().nextPage(getActivity());
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
        swipeRefreshLayout.setEnabled(true);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
    }
}
