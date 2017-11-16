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
import android.support.v4.app.FragmentManager;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.PaginationScrollListener;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;

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
    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 100;
    public static final String SORT_DIRECTION = Const.SortDir.ASCENDING;
    public static final String SORT_FIELD = "lastname";
    public static final int VISIBLE_THRESHOLD = 30;

    private ListView listView;
    private ContactsListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar pbLoading;
    private ContactsRetainFragment contactsRetainFragment;
    private TextView tvEmpty;
    // instance variables
    private int scrollIndex;
    private int scrollTop;
    private boolean isSearchMode;
    private String searchTerm;
    private boolean isLastPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
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
            isSearchMode = savedInstanceState.getBoolean("isSearchMode", false);
            searchTerm = savedInstanceState.getString("searchTerm", "");
            isLastPage = savedInstanceState.getBoolean("isLastPage", false);
        }
        init();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        init();
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                Toast.makeText(this, "SEARCH: " + query, Toast.LENGTH_SHORT).show();
                searchTerm = query;
                if (contactsRetainFragment != null) {
                    contactsRetainFragment.search(query);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("scrollIndex", scrollIndex);
        outState.putInt("scrollTop", scrollTop);
        outState.putBoolean("isSearchMode", isSearchMode);
        outState.putString("searchTerm", searchTerm);
        outState.putBoolean("isLastPage", isLastPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (contactsRetainFragment != null) {
            contactsRetainFragment.stop();
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
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        if (!TextUtils.isEmpty(searchTerm)) {
            MenuItemCompat.expandActionView(searchItem);
            searchView.setQuery(searchTerm, false);
        }
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                SearchActivity.this.finish();
                return true;
            }

        });
        return true;
    }

    private void init () {
        if (contactsRetainFragment == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            contactsRetainFragment = (ContactsRetainFragment) fragmentManager.findFragmentByTag(ContactsRetainFragment.TAG);
            if (contactsRetainFragment == null) {
                contactsRetainFragment = new ContactsRetainFragment();
            }
            fragmentManager.beginTransaction().add(contactsRetainFragment, ContactsRetainFragment.TAG).commit();
            final AccountManager accountManager = AccountManager.get(this);
            final Account account = LaAccount.get();
            final Handler handler = new Handler(Looper.myLooper());
            accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, this, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> future) {
                    try {
                        String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                        String token = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                        contactsRetainFragment.init(SearchActivity.this, basePath, token);
                        handleIntent(SearchActivity.this.getIntent());
                    } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                        Log.e(TAG, "", e);
                        Toast.makeText(SearchActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
            }, handler);
        } else {
            handleIntent(getIntent());
        }
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
        if (contactsRetainFragment != null) {
            contactsRetainFragment.refresh();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ContactsItem item = getAdapter().getItem(position);
        if (item == null || item.phones == null || item.phones.size() == 0) {
            Toast.makeText(this, getString(R.string.no_number_in_contact), Toast.LENGTH_SHORT).show();
        } else if (item.phones.size() == 1){
            startActivity(new Intent(this, DialerActivity.class).putExtra("number", item.phones.get(0)));
        } else {
            ArrayAdapter<String> calleeNumberAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, item.phones);
            AlertDialog.Builder calleeNumberPicker = new AlertDialog.Builder(this);
            calleeNumberPicker.setTitle(getString(R.string.choose_number));
            calleeNumberPicker.setCancelable(true);
            calleeNumberPicker.setAdapter(calleeNumberAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(SearchActivity.this, DialerActivity.class).putExtra("number", item.phones.get(which)));
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
        if (contactsRetainFragment != null) {
            contactsRetainFragment.nextPage();
        }
    }

    @Override
    public void onGetList (final List<ContactsItem> list, final PaginationList.ListState listState) {
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
    public void onError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}
