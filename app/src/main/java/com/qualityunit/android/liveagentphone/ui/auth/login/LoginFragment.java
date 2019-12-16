package com.qualityunit.android.liveagentphone.ui.auth.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.App;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.ui.auth.AuthActivity;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Tools;

/**
 * Created by rasto on 19.12.15.
 */
public class LoginFragment extends BaseFragment<AuthActivity> {

    public static final String TAG = LoginFragment.class.getSimpleName();
    private UrlEditText etUrl;
    private EditText etEmail;
    private EditText etPassword;
    private View vProgress;
    private View rlForm;
    private Button bLogin;
    private TextInputLayout tilUrl;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextView tvError;
    private ApiUrlTester urlTester;
    // instance variables
    private String apiUrl;
    private String urlError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            apiUrl = savedInstanceState.getString("apiUrl");
            urlError = savedInstanceState.getString("urlError");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("apiUrl", apiUrl);
        outState.putString("urlError", urlError);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.login_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView)view.findViewById(R.id.tv_version)).setText(Tools.getVersionName(getContext()));
        etUrl = (UrlEditText) view.findViewById(R.id.url);
        etUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                setErrorUrl(null);
                apiUrl = null;
                etUrl.getBackground().clearColorFilter();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                testUrl(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                testUrl(((EditText)v).getText().toString().trim());
            }

        });
        etEmail = (EditText) view.findViewById(R.id.email);
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                setErrorEmail(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etPassword = (EditText) view.findViewById(R.id.password);
        etPassword.setTransformationMethod(new PasswordTransformationMethod());
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                setErrorPassword(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etPassword.setImeOptions(EditorInfo.IME_ACTION_GO);
        etPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    attemptLogin();
                }
                return true;
            }
        });
        bLogin = (Button) view.findViewById(R.id.email_sign_in_button);
        bLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
        rlForm = view.findViewById(R.id.login_form);
        vProgress = view.findViewById(R.id.login_progress);
        tilUrl = (TextInputLayout) view.findViewById(R.id.input_layout_url);
        setErrorUrl(urlError);
        tilEmail = (TextInputLayout) view.findViewById(R.id.input_layout_email);
        tilPassword = (TextInputLayout) view.findViewById(R.id.input_layout_password);
        tvError = (TextView) view.findViewById(R.id.tv_error);
    }

    private void testUrl(String urlToTest) {
        if (urlTester != null) {
            urlTester.test(urlToTest);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (urlTester == null) {
            urlTester = new ApiUrlTester(getActivity());
        }
        testUrl(etUrl.getText().toString().trim());
        preFillFields();
    }

    @Override
    public void onPause() {
        if (urlTester != null) {
            urlTester.stop();
            urlTester = null;
        }
        saveFilledFields();
        super.onPause();
    }

    private void setErrorLogin(@Nullable final String errorMessage) {
        showProgress(false);
        if (TextUtils.isEmpty(errorMessage)) {
            tvError.setVisibility(View.GONE);
            tvError.setText("");
        } else {
            tvError.setText(errorMessage);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void setErrorUrl(@Nullable final String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            tilUrl.setErrorEnabled(false);
            tilUrl.setError(null);
        } else {
            tilUrl.setErrorEnabled(true);
            tilUrl.setError(errorMessage);
        }
    }

    private void setErrorEmail(@Nullable final String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            tilEmail.setErrorEnabled(false);
            tilEmail.setError(null);
        } else {
            tilEmail.setErrorEnabled(true);
            tilEmail.setError(errorMessage);
        }
    }

    private void setErrorPassword(@Nullable final String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            tilPassword.setErrorEnabled(false);
            tilPassword.setError(null);
        } else {
            tilPassword.setErrorEnabled(true);
            tilPassword.setError(errorMessage);
        }
    }

    private void preFillFields() {
        if (TextUtils.isEmpty(etUrl.getText().toString())) {
            etUrl.setText(App.getSharedPreferences().getString(Const.MemoryKeys.LOGIN_ET_URL, ""));
        }
        if (TextUtils.isEmpty(etEmail.getText().toString())) {
            etEmail.setText(App.getSharedPreferences().getString(Const.MemoryKeys.LOGIN_ET_EMAIL, ""));
        }
    }

    private void saveFilledFields() {
        App.getSharedPreferences().edit().putString(Const.MemoryKeys.LOGIN_ET_URL, etUrl.getText().toString()).apply();
        App.getSharedPreferences().edit().putString(Const.MemoryKeys.LOGIN_ET_EMAIL, etEmail.getText().toString()).apply();
    }

    private void attemptLogin() {
        setErrorLogin(null);
        boolean isError = false;
        Tools.hideKeyboard(getContext(), etPassword);
        if (TextUtils.isEmpty(apiUrl)) {
            setErrorUrl(getString(R.string.please_correct_url));
            isError = true;
        }
        if (TextUtils.isEmpty(etUrl.getText().toString().trim())) {
            setErrorUrl(getString(R.string.error_field_required));
            isError = true;
        }
        if (TextUtils.isEmpty(etEmail.getText().toString().trim())) {
            setErrorEmail(getString(R.string.error_field_required));
            isError = true;
        }
        if (TextUtils.isEmpty(etPassword.getText().toString().trim())) {
            setErrorPassword(getString(R.string.error_field_required));
            isError = true;
        }
        if (!isError) {
            login(null);
        }
    }

    private void login(@Nullable final String verificationCode) {
        if (!Tools.isNetworkConnected(getContext())) {
            setErrorLogin(getString(R.string.no_connection));
            return;
        }
        showProgress(true);
        final String typedUrl = etUrl.getText().toString().trim();
        final String userName = etEmail.getText().toString().trim();
        final String userPass = etPassword.getText().toString().trim();
        Client.login(activity, apiUrl, userName, userPass, verificationCode, new Client.LoginCallback() {

            @Override
            public void onSuccess(final String apikey) {
                final Intent res = new Intent();
                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
                res.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
                res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, LaAccount.ACCOUNT_TYPE);
                res.putExtra(AccountManager.KEY_AUTHTOKEN, apikey);
                res.putExtra(LaAccount.USERDATA_URL_API, apiUrl);
                res.putExtra(LaAccount.USERDATA_URL_TYPED, typedUrl);
                res.putExtra(AuthActivity.PARAM_USER_PASS, userPass);
                finishLogin(res);
            }

            @Override
            public void onInvalidPassword() {
                showProgress(false);
                setErrorLogin(getString(R.string.invalid_credentials));
            }

            @Override
            public void onFailure(Exception e) {
                showProgress(false);
                setErrorLogin(e.getMessage());
            }

            @Override
            public void onVerificationCodeRequired() {
                requestVerificationCode();
            }

            @Override
            public void onVerificationCodeFailure() {
                setErrorLogin(getString(R.string.invalid_verification_code));
            }

            @Override
            public void onTooManyLogins() {
                setErrorLogin(getString(R.string.too_many_logins));
            }
        });

    }

    public void requestVerificationCode() {
        showProgress(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.verification_code);
        View viewInflated = LayoutInflater.from(getActivity()).inflate(R.layout.input_two_factor, null);
        final EditText input = (EditText) viewInflated.findViewById(R.id.et_twofactortoken);
        builder.setView(viewInflated);
        builder.setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress(true);
                String twofactortoken = input.getText().toString().trim();
                if (TextUtils.isEmpty(twofactortoken)) {
                    requestVerificationCode();
                    return;
                }
                login(twofactortoken);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress(false);
            }
        });
        builder.show();
    }

    private void finishLogin(Intent intent) {
        final AccountManager accountManager = AccountManager.get(getActivity());
        String typedEmail = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        final Account account = LaAccount.setAccount(typedEmail, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));
        accountManager.addAccountExplicitly(account, intent.getStringExtra(AuthActivity.PARAM_USER_PASS), null);
        accountManager.setAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, intent.getStringExtra(AccountManager.KEY_AUTHTOKEN));
        accountManager.setUserData(account, LaAccount.USERDATA_URL_API, intent.getStringExtra(LaAccount.USERDATA_URL_API));
        String typedUrl = intent.getStringExtra(LaAccount.USERDATA_URL_TYPED);
        accountManager.setUserData(account, LaAccount.USERDATA_URL_TYPED, typedUrl);
        activity.setAccountAuthenticatorResult(intent.getExtras());
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
        rlForm.setVisibility(show ? View.GONE : View.VISIBLE);
        rlForm.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rlForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });
        vProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        vProgress.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                vProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private class ApiUrlTester extends UrlTester {

        public ApiUrlTester(Activity activity) {
            super(activity);
        }

        @Override
        protected void onUrlCallback(int code, String typedUrl, String apiUrl, String message) {
            setErrorUrl(null);
            switch (code) {
                case CODE.URL_BLANK:
                    setErrorUrl(null);
                    break;
                case CODE.NO_CONNECTION:
                    setErrorUrl(getString(R.string.no_connection));
                    break;
                case CODE.URL_OK:
                    LoginFragment.this.apiUrl = apiUrl;
                    etUrl.getBackground().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.ok), PorterDuff.Mode.SRC_IN);
                    break;
                case CODE.URL_NOT_VALID:
                    setErrorUrl(getString(R.string.url_not_valid));
                    break;
                case CODE.COULD_NOT_REACH_HOST:
                    setErrorUrl(getString(R.string.cant_reach_host));
                    break;
                case CODE.API_ERROR:
                    setErrorUrl(message);
                    break;
                default:
                    Log.e(TAG, "Unknown URL tester code: " + code);
            }
        }

        @Override
        protected String onGetTypedUrl() {
            return etUrl.getText().toString().trim();
        }
    }

}
