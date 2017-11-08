package com.qualityunit.android.liveagentphone.ui.home.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.PaginationScrollListener;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.ui.home.HomeActivity;
import com.qualityunit.android.liveagentphone.util.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rasto on 23.08.16.
 */
public class ContactFragment extends ListFragment implements AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener, PaginationList.CallbackListener<ContactItem>,
        PaginationScrollListener.OnNextPageListener {

    public static final String TAG = ContactFragment.class.getSimpleName();
    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 100;
    public static final String SORT_DIRECTION = Const.SortDir.ASCENDING;
    public static final String SORT_FIELD = "lastname";
    public static final int VISIBLE_THRESHOLD = 30;
    private HomeActivity activity;
    private ContactsListAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etSearch;
    private TextView tvEmpty;
    private ProgressBar pbLoading;
    private ContactsRetainFragment contactsRetainFragment;
    // instance variables
    private int scrollIndex;
    private int scrollTop;
    private boolean isSearchMode;
    private String searchTerm;
    private boolean isLastPage;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (HomeActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contacts_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvEmpty = (TextView) view.findViewById(R.id.tv_empty);
        tvEmpty.setVisibility(View.GONE);
        pbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.srl_list);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(this);

        // hack http://stackoverflow.com/questions/26858692/swiperefreshlayout-setrefreshing-not-showing-indicator-initially
        TypedValue typed_value = new TypedValue();
        getActivity().getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typed_value, true);
        swipeRefreshLayout.setProgressViewOffset(false, 0, getResources().getDimensionPixelSize(typed_value.resourceId));

        setListAdapter(getAdapter());
        getListView().setOnItemClickListener(this);
        getListView().setOnScrollListener(new PaginationScrollListener(this, VISIBLE_THRESHOLD));
        if (savedInstanceState != null) {
            scrollIndex = savedInstanceState.getInt("scrollIndex", 0);
            scrollTop = savedInstanceState.getInt("scrollTop", 0);
            isSearchMode = savedInstanceState.getBoolean("isSearchMode", false);
            searchTerm = savedInstanceState.getString("searchTerm", "");
            isLastPage = savedInstanceState.getBoolean("isLastPage", false);
        }
    }

    @Override
    public void onDestroyView() {
        if (contactsRetainFragment != null) {
            contactsRetainFragment.stop();
        }
        searchTerm = etSearch.getText().toString().trim();
        scrollIndex = getListView().getFirstVisiblePosition();
        View v = getListView().getChildAt(0);
        scrollTop = (v == null) ? 0 : (v.getTop() - getListView().getPaddingTop());
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        contactsRetainFragment = (ContactsRetainFragment) fragmentManager.findFragmentByTag(ContactsRetainFragment.TAG);
        if (contactsRetainFragment == null) {
            contactsRetainFragment = new ContactsRetainFragment();
            contactsRetainFragment.setTargetFragment(this, 0);
            fragmentManager.beginTransaction().add(contactsRetainFragment, ContactsRetainFragment.TAG).commit();
        }
        init(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("scrollIndex", scrollIndex);
        outState.putInt("scrollTop", scrollTop);
        outState.putBoolean("isSearchMode", isSearchMode);
        outState.putString("searchTerm", searchTerm);
        outState.putBoolean("isLastPage", isLastPage);
        super.onSaveInstanceState(outState);
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
        final ContactItem item = getAdapter().getItem(position);
        if (item == null || item.phones == null || item.phones.size() == 0) {
            Toast.makeText(activity, getString(R.string.no_number_in_contact), Toast.LENGTH_SHORT).show();
        } else if (item.phones.size() == 1){
            startActivity(new Intent(getActivity(), DialerActivity.class).putExtra("number", item.phones.get(0)));
        } else {
            ArrayAdapter<String> calleeNumberAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, item.phones);
            AlertDialog.Builder calleeNumberPicker = new AlertDialog.Builder(getContext());
            calleeNumberPicker.setTitle(getString(R.string.choose_number));
            calleeNumberPicker.setCancelable(true);
            calleeNumberPicker.setAdapter(calleeNumberAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(getActivity(), DialerActivity.class).putExtra("number", item.phones.get(which)));
                }
            });
            calleeNumberPicker.create().show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.contacts_menu, menu);
        final LinearLayout searchPanel = (LinearLayout) menu.findItem(R.id.action_search).getActionView();
        etSearch = (EditText) searchPanel.findViewById(R.id.et_search);
        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });
        ImageButton ibSearch = (ImageButton) searchPanel.findViewById(R.id.ib_search);
        ibSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });
        MenuItem menuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(menuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Tools.showKeyboard(getActivity(), etSearch);
                if (isSearchMode) {
                    // we get here in case of screen rotation after bellow line 'MenuItemCompat.expandActionView(menuItem);'
                    etSearch.setText(searchTerm);
                    return true;
                }
                swipeRefreshLayout.setRefreshing(false);
                tvEmpty.setVisibility(View.GONE);
                pbLoading.setVisibility(View.GONE);
                if (contactsRetainFragment != null) {
                    contactsRetainFragment.clear();
                }
                isSearchMode = true;
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Tools.hideKeyboard(getActivity(), etSearch);
                etSearch.setText("");
                isSearchMode = false;
                init(true);
                return true;
            }

        });
        if (isSearchMode) {
            MenuItemCompat.expandActionView(menuItem);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onGetList (final List<ContactItem> list, final PaginationList.ListState listState) {
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
        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
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

    /**
     * Get URL and token asynchronously and init contact retained fragment
     * @param reinit if you want to reset list then set this flag to true
     */
    private void init (final boolean reinit) {
        final AccountManager accountManager = AccountManager.get(getContext());
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle result = future.getResult();
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String token = result.getString(AccountManager.KEY_AUTHTOKEN);
                    contactsRetainFragment.init(ContactFragment.this, basePath, token, reinit);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    Log.e(TAG, "", e);
                    Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        }, handler);
    }

    /**
     * Get or create adapter
     * @return adapter
     */
    private ContactsListAdapter getAdapter() {
        if (adapter == null) {
            adapter = new ContactsListAdapter(getContext(), new ArrayList<ContactItem>());
        }
        return adapter;
    }

    /**
     * Refill list with new values
     * @param list
     */
    private void refillList(List<ContactItem> list) {
        if (list != null) {
            getAdapter().setNotifyOnChange(false);
            getAdapter().clear();
            getAdapter().addAll(list);
            getAdapter().notifyDataSetChanged();
            if (scrollIndex != 0 && scrollTop != 0) {
                getListView().setSelectionFromTop(scrollIndex, scrollTop);
                scrollIndex = 0;
                scrollTop = 0;
            }
        }
    }

    private void performSearch() {
        String searchTerm = etSearch.getText().toString().trim();
        if (!TextUtils.isEmpty(searchTerm)) {
            if (contactsRetainFragment != null) {
                contactsRetainFragment.search(searchTerm);
                Tools.hideKeyboard(getActivity(), etSearch);
            }
        }
    }
}
