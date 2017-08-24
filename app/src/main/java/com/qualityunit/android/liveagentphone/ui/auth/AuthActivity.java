package com.qualityunit.android.liveagentphone.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.AccountAuthenticatorActivity;
import com.qualityunit.android.liveagentphone.ui.about.AboutActivity;
import com.qualityunit.android.liveagentphone.ui.auth.login.LoginFragment;

/**
 * Created by rasto on 19.12.15.
 *
 * This activity is for result. If login is successful then result is RESULT_OK otherwise RESULT_CANCELED
 */
public class AuthActivity extends AccountAuthenticatorActivity {

    public static final String PARAM_USER_PASS = "PARAM_USER_PASS";
    public static final String ARG_ACCOUNT_TYPE = "ARG_ACCOUNT_TYPE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            addFragment(new LoginFragment(), LoginFragment.TAG);
        }
        setResult(RESULT_CANCELED);
    }

    @Override
    protected boolean allwaysShowHomeAsUp() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.auth_context_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
