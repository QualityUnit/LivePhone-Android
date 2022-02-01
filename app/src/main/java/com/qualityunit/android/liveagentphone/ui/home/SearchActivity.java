package com.qualityunit.android.liveagentphone.ui.home;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.PaginationList;
import com.qualityunit.android.liveagentphone.store.ContactsStore;
import com.qualityunit.android.liveagentphone.store.InternalStore;
import com.qualityunit.android.liveagentphone.ui.call.InitCallActivity;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.util.Tools;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuItemCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by rasto on 9.11.17.
 */

public class SearchActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final int INDEX_INTERNAL = 0;
    private static final int INDEX_CONTACTS = 1;
    private SearchParentAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ExpandableListView elv;
    private List<SearchSection> sections;
    private String searchTerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.srl_refreshAll);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);
        adapter = new SearchParentAdapter(this);
        sections = new ArrayList<>();
        sections.add(new InternalSection(this, InternalStore.createSearchInstance(), adapter, INDEX_INTERNAL));
        sections.add(new ContactsSection(this, ContactsStore.getInstance(), adapter, INDEX_CONTACTS));
        elv = (ExpandableListView) findViewById(R.id.elv);
        elv.setAdapter(adapter);
        elv.setOnChildClickListener(adapter);
        for (SearchSection section : sections) {
            section.init();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                searchTerm = query;
                for (SearchSection section : sections) {
                    section.search(searchTerm);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, R.anim.fade_out);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    for (SearchSection section : sections) {
                        section.clear();
                    }
                }
                return true;
            }
        });
        View v = searchView.findViewById(androidx.appcompat.R.id.search_plate);
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

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(false);
        for (SearchSection section : sections) {
            section.reload();
        }
    }

    private class InternalSection extends SearchSection<InternalItem> implements SearchChildAdapter.OnClickListener<InternalItem> {

        public InternalSection(SearchActivity searchActivity, InternalStore instance, SearchParentAdapter adapter, int indexInParentAdapter) {
            super(searchActivity, instance, adapter, indexInParentAdapter);
        }

        @Override
        protected void onRefillList(List<InternalItem> list, PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter) {
            if (listState.isLoaded()) {
                adapter.setChildAdapter(indexInParentAdapter, new InternalChildAdapter(getApplicationContext(), list, listState, this));
                swipeRefreshLayout.setEnabled(true);
            } else {
                adapter.removeChildAdapter(indexInParentAdapter);
            }
        }

        @Override
        protected void onShowError(List<InternalItem> list, PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter) {
            adapter.setChildAdapter(indexInParentAdapter, new InternalChildAdapter(getApplicationContext(), list, listState, this));
        }

        @Override
        public boolean onChildClick(InternalItem item) {
            InitCallActivity.initInternalCall(getApplicationContext(), item);
            return true;
        }
    }

    private class ContactsSection extends SearchSection<ContactsItem> implements SearchChildAdapter.OnClickListener<ContactsItem> {


        public ContactsSection(SearchActivity searchActivity, ContactsStore instance, SearchParentAdapter adapter, int indexInParentAdapter) {
            super(searchActivity, instance, adapter, indexInParentAdapter);
        }

        @Override
        protected void onRefillList(List<ContactsItem> list, PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter) {
            if (listState.isLoaded()) {
                adapter.setChildAdapter(indexInParentAdapter, new ContactsChildAdapter(getApplicationContext(), list, listState, this));
                swipeRefreshLayout.setEnabled(true);
            } else {
                adapter.removeChildAdapter(indexInParentAdapter);
            }
        }

        @Override
        protected void onShowError(List<ContactsItem> list, PaginationList.State listState, SearchParentAdapter adapter, int indexInParentAdapter) {
            adapter.setChildAdapter(indexInParentAdapter, new ContactsChildAdapter(getApplicationContext(), list, listState, this));
        }

        @Override
        public boolean onChildClick(final ContactsItem item) {
            final Intent intent = new Intent(SearchActivity.this, DialerActivity.class);
            String contactName = Tools.createContactName(item.getFirstName(), item.getLastName(), item.getSystemName());
            if (!TextUtils.isEmpty(contactName)) {
                intent.putExtra("contactName", contactName);
            }
            if (item.getPhones() == null || item.getPhones().size() == 0) {
                Toast.makeText(SearchActivity.this, getString(R.string.no_number_in_contact), Toast.LENGTH_SHORT).show();
            } else if (item.getPhones().size() == 1){
                startActivityForResult(intent.putExtra("number", item.getPhones().get(0)), 0);
                overridePendingTransition(0, R.anim.fade_out);
            } else {
                ArrayAdapter<String> calleeNumberAdapter = new ArrayAdapter<>(SearchActivity.this, android.R.layout.simple_list_item_1, item.getPhones());
                AlertDialog.Builder calleeNumberPicker = new AlertDialog.Builder(SearchActivity.this);
                calleeNumberPicker.setTitle(getString(R.string.choose_number));
                calleeNumberPicker.setCancelable(true);
                calleeNumberPicker.setAdapter(calleeNumberAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(intent.putExtra("number", item.getPhones().get(which)), 0);
                        overridePendingTransition(0, R.anim.fade_out);
                    }
                });
                calleeNumberPicker.create().show();
            }
            return true;
        }

    }
}
