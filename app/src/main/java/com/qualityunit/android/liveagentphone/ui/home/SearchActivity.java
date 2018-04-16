package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.PaginationScrollListener;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.util.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * Created by rasto on 9.11.17.
 */

public class SearchActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener, PaginationList.CallbackListener<ContactsItem>,
        PaginationScrollListener.OnNextPageListener {

    private static final String TAG = SearchActivity.class.getSimpleName();
    public static final int VISIBLE_THRESHOLD = 30;

    private ListView listView;
    private ContactsListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar pbLoading;
    private ContactsStore store;
    private TextView tvEmpty;
    // instance variables
    private int scrollIndex;
    private int scrollTop;
    private String searchTerm;
    private boolean isLastPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        tvEmpty = (TextView) findViewById(R.id.tv_empty);
        tvEmpty.setVisibility(GONE);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_list);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);
        listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(getAdapter());
        listView.setOnItemClickListener(this);
        listView.setOnScrollListener(new PaginationScrollListener(this, VISIBLE_THRESHOLD));
        if (savedInstanceState != null) {
            scrollIndex = savedInstanceState.getInt("scrollIndex", 0);
            scrollTop = savedInstanceState.getInt("scrollTop", 0);
            searchTerm = savedInstanceState.getString("searchTerm", "");
            isLastPage = savedInstanceState.getBoolean("isLastPage", false);
        }
        store = ContactsStore.getInstance();
        store.setListener(this);
        init(PaginationList.InitFlag.INSTANCE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                searchTerm = query;
                if (store != null) {
                    store.search(query);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("scrollIndex", scrollIndex);
        outState.putInt("scrollTop", scrollTop);
        outState.putString("searchTerm", searchTerm);
        outState.putBoolean("isLastPage", isLastPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        if (store != null) {
            store.setListener(null);
        }
        scrollIndex = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        scrollTop = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundResource(R.color.background);
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setQuery(searchTerm, false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                SearchActivity.this.finish();
                return true;
            }

        });
        MenuItemCompat.expandActionView(searchItem);
        return true;
    }

    private void init (final int initFlag) {
        final AccountManager accountManager = AccountManager.get(this);
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, this, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    store.init(basePath, token, initFlag);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    Log.e(TAG, "", e);
                    Toast.makeText(SearchActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, handler);
    }

    private ContactsListAdapter getAdapter() {
        if (adapter == null) {
            adapter = new ContactsListAdapter(this, new ArrayList<ContactsItem>());
        }
        return adapter;
    }

    private void refillList(List<ContactsItem> list) {
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
        final ContactsItem item = getAdapter().getItem(position);
        final Intent intent = new Intent(this, DialerActivity.class);
        String contactName = Tools.createContactName(item.firstname, item.lastname, item.system_name);
        if (!TextUtils.isEmpty(contactName)) {
            intent.putExtra("contactName", contactName);
        }
        if (item.phones == null || item.phones.size() == 0) {
            Toast.makeText(this, getString(R.string.no_number_in_contact), Toast.LENGTH_SHORT).show();
        } else if (item.phones.size() == 1){
            startActivity(intent.putExtra("number", item.phones.get(0)));
        } else {
            ArrayAdapter<String> calleeNumberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, item.phones);
            AlertDialog.Builder calleeNumberPicker = new AlertDialog.Builder(this);
            calleeNumberPicker.setTitle(getString(R.string.choose_number));
            calleeNumberPicker.setCancelable(true);
            calleeNumberPicker.setAdapter(calleeNumberAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(intent.putExtra("number", item.phones.get(which)));
                }
            });
            calleeNumberPicker.create().show();
        }
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
    public void onGetList (final List<ContactsItem> list, final PaginationList.State listState) {
        isLastPage = listState.isLastPage();
        tvEmpty.setVisibility(listState.isEmpty() ? View.VISIBLE : View.GONE);
        pbLoading.setVisibility(listState.isLoading() ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(listState.isRefreshing());
        if (!listState.isLoading()) {
            swipeRefreshLayout.setEnabled(true);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }
        refillList(list);
    }

    @Override
    public void onError(String errorMessage, final PaginationList.State listState) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
