package com.qualityunit.android.liveagentphone.ui.home.availability;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.ui.home.HomeActivity;

import java.util.List;

/**
 * Created by rasto on 31.10.17.
 */

public class AvailabilityFragment extends BaseFragment<HomeActivity> implements AvailabilityCallbacks {

    public static String TAG = AvailabilityFragment.class.getSimpleName();
    private AvailabilityStore store;
    private Switch availabilitySwitch;
    private ListView listView;
    private View availabilitySwitchPane;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_availability, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // main switch stuff
        availabilitySwitchPane = view.findViewById(R.id.fl_mainSwitchPane);
        availabilitySwitchPane.setVisibility(View.GONE);
        View availabilitySwitchView = view.findViewById(R.id.ll_mainSwitch);
        availabilitySwitch = (Switch) availabilitySwitchView.findViewById(R.id.s_active);
        TextView availabilityLabel = (TextView) availabilitySwitchView.findViewById(R.id.tv_name);
        availabilityLabel.setText(R.string.status_available);
        // department list
        listView = (ListView) view.findViewById(R.id.lv_list);
    }

    private CompoundButton.OnCheckedChangeListener createOnCheckedAvailabilitySwitch() {
        return new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                availabilitySwitch.setOnCheckedChangeListener(null);
                store.updateDevice(isChecked);
            }
        };
    }

    @Override
    public void onDestroyView() {
        store.removeCallBacks(this);
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        store = (AvailabilityStore) fragmentManager.findFragmentByTag(AvailabilityStore.TAG);
        if (store == null) {
            store = new AvailabilityStore();
            fragmentManager.beginTransaction().add(store, AvailabilityStore.TAG).commit();
        }
        store.addCallBacks(this);
        store.getDevice();
    }

    @Override
    public void onDevice(Boolean isAvailable, Exception e) {
        availabilitySwitch.setOnCheckedChangeListener(null);
        availabilitySwitch.setChecked(isAvailable);
        availabilitySwitch.setOnCheckedChangeListener(createOnCheckedAvailabilitySwitch());
        availabilitySwitchPane.setVisibility(View.VISIBLE);
        if (e != null) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        store.getDepartments();
    }

    @Override
    public void onDepartmentList(List<DepartmentStatusItem> list, Exception e) {
        AvailabilityListAdapter adapter = new AvailabilityListAdapter(getActivity(), list);
        listView.setAdapter(adapter);
    }

}
