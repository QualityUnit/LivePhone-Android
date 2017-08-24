package com.qualityunit.android.liveagentphone.ui.common;

import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 26.01.16.
 */
public abstract class ToolbarActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = ToolbarActivity.class.getSimpleName();
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeSetContentView();
        setContentView(getContentViewRes());
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        onBackStackChanged();
    }

    protected void beforeSetContentView() {}

    public void addFragment(Fragment fragment, String tagToBackStack) {
        fragmentTransaction(false, fragment, tagToBackStack);
    }

    public void switchFragments(Fragment fragment, String tagToBackStack) {
        fragmentTransaction(true, fragment, tagToBackStack);
    }

    private void fragmentTransaction(boolean replace, Fragment fragment, String tagToBackStack) {
        if(isFinishing()) {
            return;
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        if (replace) {
            fragmentTransaction.replace(R.id.container, fragment);
            fragmentTransaction.addToBackStack(tagToBackStack);
        }
        else {
            fragmentTransaction.add(R.id.container, fragment, tagToBackStack);
        }
        fragmentTransaction.commit();
    }

    public void popToFragment(String tagOfFragment) {
        getSupportFragmentManager().popBackStack(tagOfFragment, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public Snackbar createSnackBar(@StringRes int textRes, int snackLength) {
        return createSnackBar(getString(textRes), snackLength);
    }

    public Snackbar createSnackBar(String text, int snackLength) {
        CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        if (coordinatorLayout != null) {
            Snackbar snackbar = Snackbar.make(coordinatorLayout, text, snackLength);
            return snackbar;
        }
        return null;
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0 || allwaysShowHomeAsUp()) {
            if (getSupportActionBar() != null && toolbar != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onBackPressed();
                    }
                });
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }
    }

    protected abstract boolean allwaysShowHomeAsUp();

    // METHODS TO OVERRIDE

    protected int getContentViewRes() {
        return R.layout._activity_toolbar;
    }
}
