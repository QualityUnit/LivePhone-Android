package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.ui.about.AboutActivity;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.ui.home.availability.AvailabilityCallbacks;
import com.qualityunit.android.liveagentphone.ui.home.availability.AvailabilityFragment;
import com.qualityunit.android.liveagentphone.ui.home.availability.AvailabilityStore;
import com.qualityunit.android.liveagentphone.ui.home.availability.DepartmentStatusItem;
import com.qualityunit.android.liveagentphone.ui.home.contacts.ContactFragment;
import com.qualityunit.android.liveagentphone.ui.init.InitActivity;
import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.List;


public class HomeActivity extends AppCompatActivity implements AvailabilityCallbacks, FragmentManager.OnBackStackChangedListener {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private final int statusIdentifier = 1000000;
    private Menu actionItems;
    private AvailabilityStore store;
    private boolean isFinishing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle(getString(R.string.title_contacts));
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        onBackStackChanged();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            fragmentTransaction.add(R.id.container, new ContactFragment(), ContactFragment.TAG);
            fragmentTransaction.commit();
        }
        store = (AvailabilityStore) fragmentManager.findFragmentByTag(AvailabilityStore.TAG);
        if (store == null) {
            store = new AvailabilityStore();
            fragmentManager.beginTransaction().add(store, AvailabilityStore.TAG).commit();
        }
        store.addCallBacks(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_dialpad);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(HomeActivity.this, DialerActivity.class).putExtra("number", ""));
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        store.removeCallBacks(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        actionItems = menu;
        store.getDevice();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_logout:
                isFinishing = true;
                store.updateDevice(false);
                return true;
            case statusIdentifier:
                FragmentManager fragmentManager = getSupportFragmentManager();
                AvailabilityFragment availabilityFragment = (AvailabilityFragment) fragmentManager.findFragmentByTag(AvailabilityFragment.TAG);
                if (availabilityFragment == null) {
                    availabilityFragment = new AvailabilityFragment();
                }
                fragmentManager.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .replace(R.id.container, availabilityFragment)
                        .addToBackStack(AvailabilityFragment.TAG)
                        .commit();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackStackChanged() {
        final ActionBar actionBar = getSupportActionBar();
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            if (toolbar == null) {
                throw new NullPointerException("Home toolbar is null");
            }
            if (actionBar == null) {
                throw new NullPointerException("Home actionBar is null");
            }
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
            } else {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
        } catch (Exception e) {
            Logger.e(TAG, e);
        }
    }

    @Override
    public void onDevice(Boolean isAvailable, Exception e) {
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (isFinishing) {
            isFinishing = false;
            final AccountManager accountManager = AccountManager.get(this);
            final Handler handler = new Handler(Looper.myLooper());
            final AccountManagerCallback accountManagerCallback = new AccountManagerCallback() {
                @Override
                public void run(AccountManagerFuture future) {
                    finish();
                    startActivity(new Intent(HomeActivity.this, InitActivity.class));
                }
            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                accountManager.removeAccount(LaAccount.get(), this, accountManagerCallback, handler);
            } else {
                accountManager.removeAccount(LaAccount.get(), accountManagerCallback, handler);
            }
            return;
        }
        int itemTitleRes = isAvailable ? R.string.status_available : R.string.status_unavailable;
        int itemIconRes = isAvailable ? R.drawable.ic_status_available : R.drawable.ic_status_unavailable;
        MenuItem item = actionItems.findItem(statusIdentifier);
        if (item == null) {
            actionItems.add(Menu.NONE, statusIdentifier, 100, itemTitleRes)
                    .setIcon(itemIconRes)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            item.setIcon(itemIconRes);
            item.setTitle(itemTitleRes);
        }
    }

    @Override
    public void onDepartmentList(List<DepartmentStatusItem> list, Exception e) {
        // no need here
    }
}