package com.qualityunit.android.liveagentphone.ui.home.availability;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.Api;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.ui.home.HomeActivity;
import com.qualityunit.android.liveagentphone.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rasto on 31.10.17.
 */

public class AvailabilityStore extends BaseFragment<HomeActivity> {

    public static final String TAG = AvailabilityStore.class.getSimpleName();
    private Integer deviceId;
    private String phoneId;
    private String agentId;
    private Set<AvailabilityCallbacks> callbacksSet = new HashSet<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    /**
     * Set callbacks for retrieving device phone status and phone statuses for every department
     * @param callBacks
     */
    public void addCallBacks(AvailabilityCallbacks callBacks) {
        this.callbacksSet.add(callBacks);
    }

    /**
     * Remove callbacks for retrieving device phone status and phone statuses for every department
     * @param callBacks
     */
    public void removeCallBacks(AvailabilityCallbacks callBacks) {
        this.callbacksSet.remove(callBacks);
    }

    /**
     * Get current device phone status using phoneId from SharedPreferences
     */
    public void getDevice() {
        final Account account = LaAccount.get();
        final AccountManager accountManager = AccountManager.get(getActivity());
        phoneId = accountManager.getUserData(account, LaAccount.USERDATA_PHONE_ID);
        Api.getDevicePhoneStatus(getActivity(), phoneId, new Api.DeviceCallback() {

            @Override
            public void onResponse(Boolean isAvailable, Integer deviceId, String agentId, Exception e) {
                AvailabilityStore.this.agentId = agentId;
                AvailabilityStore.this.deviceId = deviceId;
                for (AvailabilityCallbacks item : callbacksSet) {
                    item.onDevice(isAvailable, e);
                }
            }

        });
    }

    /**
     * Update current device phone status
     * @param requestedStatus is requested new status
     */
    public void updateDevice(boolean requestedStatus) {
        Api.updateDevicePhoneStatus(getActivity(), deviceId, agentId, requestedStatus, new Api.DeviceCallback() {
            @Override
            public void onResponse(Boolean status, Integer deviceId, String agentId, Exception e) {
                if (e != null) {
                    e.printStackTrace();
                }
                for (AvailabilityCallbacks item : callbacksSet) {
                    item.onDevice(status, e);
                }
            }
        });
    }

    /**
     * Get phone statuses for every department. You can call this method after calling getDevice().
     */
    public void getDepartments() {
        if (deviceId == null) {
            String errMsg = "Cannot call 'getDepartments() before calling 'getDevice()'";
            for (AvailabilityCallbacks item : callbacksSet) {
                item.onDepartmentList(null, new Exception(errMsg));
            }
            Logger.e(TAG, errMsg);
            return;
        }
        Api.getDepartmentStatusList(getActivity(), deviceId, new Api.DepartmentStatusListCallback() {

            @Override
            public void onResponse(JSONArray array, Exception e) {
                List<DepartmentStatusItem> list = null;
                if (e == null) {
                    int arrayLength = array.length();
                    list = new ArrayList<>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        try {
                            JSONObject obj = array.getJSONObject(i);
                            DepartmentStatusItem item = new DepartmentStatusItem(
                                    obj.getInt("device_id"),
                                    obj.getString("department_id"),
                                    obj.getString("user_id"),
                                    obj.getString("department_name"),
                                    "N".equals(obj.getString("online_status"))
                            );
                            list.add(item);
                        } catch (JSONException e1) {
                            e = e1;
                            e.printStackTrace();
                            break;
                        }
                    }
                }
                for (AvailabilityCallbacks item : callbacksSet) {
                    item.onDepartmentList(list, e);
                }
            }

        });
    }


}
