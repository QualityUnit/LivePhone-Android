package com.qualityunit.android.liveagentphone.ui.auth.login;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.App;
import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.ErrorCode;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.ui.auth.AuthActivity;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.qualityunit.android.restful.method.RestGetBuilder;
import com.qualityunit.android.restful.util.ResponseProcessor;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;

import java.io.IOException;

/**
 * Created by rasto on 19.12.15.
 */
public class LoginFragment extends BaseFragment<AuthActivity> {

    public static final String TAG = LoginFragment.class.getSimpleName();
    private UrlEditText etUrl;
    private AutoCompleteTextView etEmail;
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
        ((TextView)view.findViewById(R.id.tv_version)).setText(Tools.getVersionName());
        etUrl = (UrlEditText) view.findViewById(R.id.url);
        etUrl.setThreshold(1);
        etUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                setErrorUrl(null);
                apiUrl = null;
                etUrl.getBackground().clearColorFilter();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (urlTester != null) {
                    urlTester.test(s.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etUrl.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                urlTester.test(etUrl.getText().toString().trim());
            }
        });
        etEmail = (AutoCompleteTextView) view.findViewById(R.id.email);
        etEmail.setThreshold(1);
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

    @Override
    public void onResume() {
        super.onResume();
        if (urlTester == null) {
            urlTester = new ApiUrlTester();
        }
        urlTester.test(etUrl.getText().toString().trim());
        preFillFields();
    }

    @Override
    public void onPause() {
        if (urlTester != null) {
            urlTester.stop();
        }
        saveFilledFields();
        super.onPause();
    }

    private void setErrorLogin(@Nullable final String errorMessage) {
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
            login();
        }

    }

    private void login() {
        if (!Tools.isNetworkConnected()) {
            setErrorLogin(getString(R.string.no_connection));
            return;
        }
        showProgress(true);
        final String typedUrl = etUrl.getText().toString().trim();
        final String userName = etEmail.getText().toString().trim();
        final String userPass = etPassword.getText().toString().trim();
        new AsyncTask<Void, Void, GetTokenResult>() {
            @Override
            protected GetTokenResult doInBackground(Void... params) {
                final Client client = Client.getInstance();
                final Request request = new RestGetBuilder(apiUrl, "/token")
                        .addHeader("Accept", "application/json")
                        .addParam("username", userName)
                        .addParam("password", userPass)
                        .build();
                try {
                    Call call = client.newCall(request);
                    Response response = call.execute();
                    if (response.isSuccessful()) {
                        final Intent res = new Intent();
                        res.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
                        res.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
                        res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, LaAccount.ACCOUNT_TYPE);
                        res.putExtra(AccountManager.KEY_AUTHTOKEN, (ResponseProcessor.bodyToJson(response)).getString("key"));
                        res.putExtra(LaAccount.USERDATA_URL_API, apiUrl);
                        res.putExtra(LaAccount.USERDATA_URL_TYPED, typedUrl);
                        res.putExtra(AuthActivity.PARAM_USER_PASS, userPass);
                        return new GetTokenResult(res);
                    }
                    else {
                        String errorMessage = ResponseProcessor.errorMessage(response);
                        Log.e(TAG, errorMessage);
                        return new GetTokenResult(errorMessage, response.code());
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "", e);
                    return new GetTokenResult(e.getMessage(), ErrorCode.CANNOT_PARSE_RESPONSE);
                }
            }

            @Override
            protected void onPostExecute(GetTokenResult result) {
                if (result.intent != null) {
                    finishLogin(result.intent);
                }
                else {
                    showProgress(false);
                    setErrorLogin(result.errorMessage);
                }
            }
        }.execute();
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

        @Override
        protected void onUrlCallback(int code, String typedUrl, String apiUrl, String message) {
            setErrorUrl(null);
            setErrorLogin(null);
            switch (code) {
                case CODE.URL_BLANK:
                    setErrorUrl(null);
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
                case CODE.NO_CONNECTION:
                    setErrorLogin(getString(R.string.no_connection));
                    break;
                case CODE.API_ERROR:
                    setErrorUrl(message);
                    break;
                default:
                    Logger.e(TAG, "Unknown URL tester code: " + code);
            }
        }

        @Override
        protected String onGetTypedUrl() {
            return etUrl.getText().toString().trim();
        }
    }

    private class GetTokenResult {

        protected Intent intent;
        protected String errorMessage;
        protected int errorCode;

        public GetTokenResult(String errorMessage, int errorCode) {
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public GetTokenResult(Intent intent) {
            this.intent = intent;
        }
    }

}
