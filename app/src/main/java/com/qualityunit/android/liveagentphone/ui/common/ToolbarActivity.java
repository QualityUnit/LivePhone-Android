package com.qualityunit.android.liveagentphone.ui.common;

import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 26.01.16.
 */
public abstract class ToolbarActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private static final String TAG = ToolbarActivity.class.getSimpleName();
    protected Toolbar toolbar;
    private boolean isResumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeSetContentView();
        setContentView(getContentViewRes());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        onBackStackChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    public void onPause() {
        isResumed = false;
        super.onPause();
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
        if (isResumed) {
            getSupportFragmentManager().popBackStack(tagOfFragment, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
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
                        if (isResumed) {
                            onBackPressed();
                        }
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
        return R.layout._toolbar_activity;
    }
}
