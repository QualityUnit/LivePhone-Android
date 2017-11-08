package com.qualityunit.android.liveagentphone.ui.status;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;

import java.util.List;

/**
 * Created by rasto on 8.11.17.
 */

public class StatusActivity extends AppCompatActivity implements StatusCallbacks{

    private StatusStore store;
    private Switch availabilitySwitch;
    private ListView listView;
    private View availabilitySwitchPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status_activity);
        setTitle(getString(R.string.action_availability));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // main switch stuff
        availabilitySwitchPane = findViewById(R.id.fl_mainSwitchPane);
        availabilitySwitchPane.setVisibility(View.GONE);
        View availabilitySwitchView = findViewById(R.id.ll_mainSwitch);
        availabilitySwitch = (Switch) availabilitySwitchView.findViewById(R.id.s_active);
        TextView availabilityLabel = (TextView) availabilitySwitchView.findViewById(R.id.tv_name);
        availabilityLabel.setText(R.string.status_available);

        // department list
        listView = (ListView) findViewById(R.id.lv_list);
        FragmentManager fragmentManager = getSupportFragmentManager();
        store = (StatusStore) fragmentManager.findFragmentByTag(StatusStore.TAG);
        if (store == null) {
            store = new StatusStore();
            fragmentManager.beginTransaction().add(store, StatusStore.TAG).commit();
        }
        store.addCallBacks(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        store.getAll();
    }

    @Override
    protected void onDestroy() {
        store.removeCallBacks(this);
        super.onDestroy();
    }

    @Override
    public void onDevice(Boolean isAvailable, Exception e) {
        availabilitySwitch.setOnCheckedChangeListener(null);
        availabilitySwitch.setChecked(isAvailable);
        availabilitySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                store.updateDevice(isChecked);
            }
        });
        availabilitySwitchPane.setVisibility(View.VISIBLE);
        if (e != null) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDepartmentList(List<DepartmentStatusItem> list, Exception e) {
        StatusListAdapter adapter = new StatusListAdapter(this, list);
        listView.setAdapter(adapter);
    }

}
