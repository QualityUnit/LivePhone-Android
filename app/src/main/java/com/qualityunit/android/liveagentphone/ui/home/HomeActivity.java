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
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.ui.about.AboutActivity;
import com.qualityunit.android.liveagentphone.ui.common.ResumePause;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerActivity;
import com.qualityunit.android.liveagentphone.ui.home.contacts.ContactFragment;
import com.qualityunit.android.liveagentphone.ui.init.InitActivity;


public class HomeActivity extends AppCompatActivity {

    private static final String TAG = HomeActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.title_contacts));
        setSupportActionBar(toolbar);

        FragmentStatePagerAdapter sectionsPagerAdapter;
        sectionsPagerAdapter = new SectionsPagerAdapter();
        if (sectionsPagerAdapter.getCount() <= 1) {
            findViewById(R.id.tabs).setVisibility(View.GONE);
        }
        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(sectionsPagerAdapter);

        SectionsPageChangeListener sectionsPageChangeListener = new SectionsPageChangeListener(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(sectionsPageChangeListener);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_dialpad);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDialer("");
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_logout:
                final AccountManager accountManager = AccountManager.get(this);
                final Handler handler = new Handler(Looper.myLooper());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    accountManager.removeAccount(LaAccount.get(), this, new AccountManagerCallback<Bundle>() {
                        @Override
                        public void run(AccountManagerFuture<Bundle> future) {
                            runInit();
                        }
                    }, handler);
                } else {
                    accountManager.removeAccount(LaAccount.get(), new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> future) {
                            runInit();
                        }
                    }, handler);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void runInit() {
        finish();
        startActivity(new Intent(HomeActivity.this, InitActivity.class));
        Toast.makeText(HomeActivity.this, "Warning... Temporary implementation", Toast.LENGTH_SHORT).show();
    }

    public void openDialer(String number) {
        startActivity(new Intent(this, DialerActivity.class).putExtra("number", number));
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
                    fragment = new ContactFragment();
                    break;
                default:
                    throw new RuntimeException("Fragment position '" + position + "' is not defined in " + TAG);
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_contacts);
                default:
                    throw new RuntimeException("String position '" + position + "' is not defined in " + TAG);
            }
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