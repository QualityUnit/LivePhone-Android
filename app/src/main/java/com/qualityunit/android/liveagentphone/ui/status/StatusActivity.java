package com.qualityunit.android.liveagentphone.ui.status;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.store.StatusStore;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by rasto on 8.11.17.
 */

public class StatusActivity extends AppCompatActivity implements StatusCallbacks, SwipeRefreshLayout.OnRefreshListener {

    private Switch mobileAvailabilitySwitch;
    private ListView listView;
    private TextView listMessage;
    private ProgressBar listLoading;
    private ProgressBar mobileStatusLoading;
    private LinearLayout llStatusWeb;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_activity);
        setTitle(R.string.status_available);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // mobile status stuff
        mobileAvailabilitySwitch = (Switch) toolbar.findViewById(R.id.s_mobileStatus);
        mobileAvailabilitySwitch.setVisibility(View.INVISIBLE);

        // department list
        swipeRefreshLayout = findViewById(R.id.srl_list);
        swipeRefreshLayout.setOnRefreshListener(this);
        listView = (ListView) findViewById(R.id.lv_list);
        listMessage = (TextView) findViewById(R.id.tv_list_message);
        listLoading = (ProgressBar) findViewById(R.id.pb_list_loading);
        mobileStatusLoading = (ProgressBar) findViewById(R.id.s_mobileStatus_loading);
        StatusStore.getInstance(this).addCallBacks(this);

        // browser status stuff
        llStatusWeb = (LinearLayout) findViewById(R.id.ll_status_web);
        llStatusWeb.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusStore.getInstance(this).getDevice(true, true);
    }

    @Override
    protected void onPause() {
        overridePendingTransition(0, R.anim.fade_out);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        StatusStore.getInstance(this).removeCallBacks(this);
        super.onDestroy();
    }

    @Override
    public void onDevices(int mobilePhoneStatus, int browserPhoneStatus, Exception e) {
        mobileAvailabilitySwitch.setVisibility(View.VISIBLE);
        mobileStatusLoading.setVisibility(View.INVISIBLE);
        if (browserPhoneStatus > StatusStore.PHONE_STATUS_NULL) {
            if (browserPhoneStatus == StatusStore.PHONE_STATUS_OUT_IN) {
                llStatusWeb.setVisibility(View.VISIBLE);
            }
        } else {
            llStatusWeb.setVisibility(View.GONE);
        }
        if (mobilePhoneStatus > StatusStore.PHONE_STATUS_NULL) {
            mobileAvailabilitySwitch.setOnCheckedChangeListener(null);
            mobileAvailabilitySwitch.setChecked(mobilePhoneStatus == StatusStore.PHONE_STATUS_OUT_IN);
            mobileAvailabilitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    StatusStore.getInstance(StatusActivity.this).updateMobileDevice(isChecked);
                }
            });
            mobileAvailabilitySwitch.setVisibility(View.VISIBLE);
        } else {
            mobileAvailabilitySwitch.setVisibility(View.GONE);
        }
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoadingDepartmentList() {
        listMessage.setVisibility(View.INVISIBLE);
        listView.setVisibility(View.INVISIBLE);
        listLoading.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDepartmentList(List<DepartmentStatusItem> list, Exception e) {
        listView.setVisibility(View.INVISIBLE);
        listMessage.setVisibility(View.INVISIBLE);
        listLoading.setVisibility(View.INVISIBLE);
        if (list == null) {
            listView.setAdapter(null);
        } else if (list.size() == 0) {
            listMessage.setText(getString(R.string.empty));
            listView.setVisibility(View.INVISIBLE);
            listMessage.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            StatusListAdapter adapter = new StatusListAdapter(this, list);
            listView.setAdapter(adapter);
        }
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRefresh() {
        onLoadingDepartmentList();
        llStatusWeb.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
        mobileAvailabilitySwitch.setVisibility(View.INVISIBLE);
        mobileStatusLoading.setVisibility(View.VISIBLE);
        StatusStore.getInstance(this).getDevice(true, true);
    }
}
