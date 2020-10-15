package com.qualityunit.android.liveagentphone.ui.call;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.ui.home.InternalItem;

import static com.qualityunit.android.liveagentphone.Const.NotificationId.INIT_CALL_NOTIFICATION_ID;

/**
 * Created by rasto on 18.10.16.
 */

public class InitCallActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    public static final String INTENT_FILTER_INITCALL_DISMISS = "com.qualityunit.android.liveagentphone.INITCALL_DISMISS";
    public static final String EXTRA_DIAL_STRING = "dialString";
    public static final String EXTRA_CONTACT_NAME = "contactName";
    public static final String EXTRA_REMOTE_NUMBER = "number";
    private Toolbar toolbar;
    private String contactName;
    private String dialString;
    private String remoteNumber;
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver initCallDissmissReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.init_call_activity);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
            wakeLock.acquire(10*60*1000L /*10 minutes*/);
        }
        registerReceiver();
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        FloatingActionButton fabMakeCall = (FloatingActionButton) findViewById(R.id.fab_makeCall);
        if (fabMakeCall != null) {
            fabMakeCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                    CallingCommands.makeCall(getApplicationContext(), dialString, "", TextUtils.isEmpty(contactName) ? remoteNumber : contactName);
                }
            });
        }
        onBackStackChanged();
        fillUi();
        dismissNotification();
    }

    private void dismissNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(INIT_CALL_NOTIFICATION_ID);
    }

    private void fillUi() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }
        dialString = bundle.getString(EXTRA_DIAL_STRING);
        contactName = bundle.getString(EXTRA_CONTACT_NAME);
        remoteNumber = bundle.getString(EXTRA_REMOTE_NUMBER);
        TextView tvPrimary = (TextView) findViewById(R.id.tv_primary);
        TextView tvSecondary = (TextView) findViewById(R.id.tv_secondary);
        if (!TextUtils.isEmpty(contactName)) {
            tvPrimary.setText(contactName);
            tvSecondary.setText(remoteNumber);
            tvSecondary.setVisibility(View.VISIBLE);
        } else {
            tvSecondary.setVisibility(View.GONE);
            tvPrimary.setText(remoteNumber);
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
        setIntent(intent);
        fillUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private void registerReceiver() {
        if (initCallDissmissReceiver == null) {
            initCallDissmissReceiver = new InitCallDismissReceiver();
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(initCallDissmissReceiver, new IntentFilter(INTENT_FILTER_INITCALL_DISMISS));
        }
    }

    private void unregisterReceiver() {
        if (initCallDissmissReceiver != null) {
            initCallDissmissReceiver = null;
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(initCallDissmissReceiver);
        }
    }

    public static void initInternalCall(Context context, InternalItem item) {
        String remoteName = item.agent == null ? item.department.departmentName : item.agent.name;
        Intent intent = new Intent(context, InitCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle bundle = new Bundle();
        bundle.putString(InitCallActivity.EXTRA_DIAL_STRING, item.number);
        bundle.putString(InitCallActivity.EXTRA_REMOTE_NUMBER, item.number);
        bundle.putString(InitCallActivity.EXTRA_CONTACT_NAME, remoteName);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    private class InitCallDismissReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }

    }
}
