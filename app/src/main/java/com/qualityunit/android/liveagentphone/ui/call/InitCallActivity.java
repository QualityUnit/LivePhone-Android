package com.qualityunit.android.liveagentphone.ui.call;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;

/**
 * Created by rasto on 18.10.16.
 */

public class InitCallActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private FloatingActionButton fabStartCall;
    private Toolbar toolbar;
    private String contactName;
    private String dialString;
    private String remoteNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.init_call_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        fabStartCall = (FloatingActionButton) findViewById(R.id.fab_makeCall);
        if (fabStartCall != null) {
            fabStartCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    makeCall();
                }
            });
        }
        onBackStackChanged();
        Bundle bundle = getIntent().getExtras();
        dialString = bundle.getString("dialString");
        contactName = bundle.getString("contactName");
        remoteNumber = bundle.getString("number");
        TextView tvNumber = (TextView) findViewById(R.id.tv_number);
        tvNumber.setText(remoteNumber);
        TextView tvContact = (TextView) findViewById(R.id.tv_contact);
        if (!TextUtils.isEmpty(contactName)) {
            tvContact.setText(contactName);
            tvContact.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackStackChanged() {
        if (getSupportActionBar() != null && toolbar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String type = bundle.getString("type");
            if (Const.Push.PUSH_TYPE_CANCEL_INIT_CALL.equals(type)) {
                finish();
            }
        }
    }

    public void makeCall() {
        try {
            CallingCommands.makeCall(getApplicationContext(), dialString, "", TextUtils.isEmpty(contactName) ? remoteNumber : contactName);
        } catch (CallingException e) {
            Toast.makeText(InitCallActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
