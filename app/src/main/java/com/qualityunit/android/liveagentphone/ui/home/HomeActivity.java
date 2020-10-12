package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.store.StatusStore;
import com.qualityunit.android.liveagentphone.ui.about.AboutActivity;
import com.qualityunit.android.liveagentphone.ui.common.ResumePause;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.ui.init.InitActivity;
import com.qualityunit.android.liveagentphone.ui.status.DepartmentStatusItem;
import com.qualityunit.android.liveagentphone.ui.status.StatusActivity;
import com.qualityunit.android.liveagentphone.ui.status.StatusCallbacks;

import java.util.List;


public class HomeActivity extends AppCompatActivity implements StatusCallbacks {

    private static final String TAG = HomeActivity.class.getSimpleName();
    private static final int STATUS_REQUEST_CODE = 1;
    private MenuItem statusItem;
    private RecentFragment fragmentRecents;
    private InternalFragment internalFragment;
    private int statusItemIconRes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View cardView = findViewById(R.id.cv_search);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SearchActivity.class));
                overridePendingTransition(0, R.anim.fade_out);
            }
        });
        FragmentStatePagerAdapter sectionsPagerAdapter;
        sectionsPagerAdapter = new SectionsPagerAdapter();
        ViewPager viewPager = findViewById(R.id.container);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(sectionsPagerAdapter);

        SectionsPageChangeListener sectionsPageChangeListener = new SectionsPageChangeListener(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(sectionsPageChangeListener);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home_white_24dp);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_hashtag_24);

        StatusStore.getInstance(this).addCallBacks(this);
        FloatingActionButton fab = findViewById(R.id.fab_dialpad);
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
    protected void onResume() {
        super.onResume();
        final Account[] accounts = AccountManager.get(this).getAccountsByType(LaAccount.ACCOUNT_TYPE);
        if (accounts.length == 0) {
            finish();
            return;
        }
        StatusStore.getInstance(this).getDevice(true, false);
    }

    @Override
    protected void onDestroy() {
        if (StatusStore.hasInstance()) {
            StatusStore.getInstance(this).removeCallBacks(this);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        statusItem = menu.findItem(R.id.action_status);
        updateStatusItem();
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_status:
                startActivityForResult(new Intent(this, StatusActivity.class), STATUS_REQUEST_CODE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == STATUS_REQUEST_CODE) {
            StatusStore.getInstance(this).getDevice(false, false);
        }
    }

    @Override
    public void onDevices(int mobilePhoneStatus, int browserPhoneStatus, Exception e) {
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        int phoneStatus = Math.max(mobilePhoneStatus, browserPhoneStatus);
        switch (phoneStatus) {
            case StatusStore.PHONE_STATUS_NULL:
                statusItemIconRes = R.drawable.ic_status_no_calls;
                break;
            case StatusStore.PHONE_STATUS_OUT:
                statusItemIconRes = R.drawable.ic_status_unavailable;
                break;
            case StatusStore.PHONE_STATUS_OUT_IN:
                statusItemIconRes = R.drawable.ic_status_available;
                break;
        }
        updateStatusItem();
    }

    @Override
    public void onLoadingDepartmentList() {
        // nothing here
    }

    private void logout() {
        finish();
        Intent intent = new Intent(this, InitActivity.class);
        intent.putExtra(InitActivity.PERFORM_LOGOUT, true);
        startActivity(intent);
    }

    private void updateStatusItem() {
        if (statusItem == null) {
            Log.e(TAG, "statusItem cannot be null");
            return;
        }
        if (statusItemIconRes == 0) {
            // status not fetched yet
            return;
        }
        statusItem.setIcon(statusItemIconRes);
        statusItem.setVisible(true);
        if (fragmentRecents != null) {
            fragmentRecents.setWaterSymbol(statusItemIconRes);
        }
    }

    @Override
    public void onDepartmentList(List<DepartmentStatusItem> list, Exception e) {
        // do nothing here
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final String TAG = SectionsPagerAdapter.class.getSimpleName();

        public SectionsPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            switch (position) {
                case 0:
                    fragment = fragmentRecents = new RecentFragment();
                    break;
                case 1:
                    fragment = internalFragment = new InternalFragment();
                    break;
                default:
                    throw new RuntimeException("Fragment position '" + position + "' is not defined in " + TAG);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }

    }

    private class SectionsPageChangeListener implements ViewPager.OnPageChangeListener {

        private FragmentStatePagerAdapter pagerAdapter;
        int currentPosition = 0;

        public SectionsPageChangeListener(FragmentStatePagerAdapter pagerAdapter) {
            this.pagerAdapter = pagerAdapter;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

        @Override
        public void onPageSelected(int position) {
            Fragment fragmentToShow = pagerAdapter.getItem(position);
            if (fragmentToShow instanceof ResumePause) {
                ((ResumePause) fragmentToShow).onResumeFragment();
            }
            Fragment fragmentToHide = pagerAdapter.getItem(currentPosition);
            if (fragmentToHide instanceof ResumePause) {
                ((ResumePause) fragmentToHide).onPauseFragment();
            }
            currentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {}
    }
}