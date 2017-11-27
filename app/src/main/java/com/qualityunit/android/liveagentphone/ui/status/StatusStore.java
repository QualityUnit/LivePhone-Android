package com.qualityunit.android.liveagentphone.ui.status;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.Api;
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

public class StatusStore extends Fragment {

    public static final String TAG = StatusStore.class.getSimpleName();
    private Activity activity;
    private Integer deviceId;
    private String phoneId;
    private String agentId;
    private Boolean isAvailable;
    private boolean isGettingAll;
    private final Set<StatusCallbacks> callbacksSet = new HashSet<>();
    private final Api.DeviceCallback deviceCallbacks = new Api.DeviceCallback() {

        @Override
        public void onResponse(Boolean isAvailable, Integer deviceId, String agentId, Exception e) {
            StatusStore.this.isAvailable = isAvailable;
            StatusStore.this.agentId = agentId;
            StatusStore.this.deviceId = deviceId;
            for (StatusCallbacks item : callbacksSet) {
                item.onDevice(isAvailable, e);
            }
            if (isGettingAll) {
                isGettingAll = false;
                if (e == null) {
                    getDepartments();
                }
            }
        }

    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (Activity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        phoneId = AccountManager.get(activity).getUserData(LaAccount.get(), LaAccount.USERDATA_PHONE_ID);
    }

    /**
     * Set callbacks for retrieving device phone status and phone statuses for every department
     * @param callBacks
     */
    public void addCallBacks(StatusCallbacks callBacks) {
        this.callbacksSet.add(callBacks);
    }

    /**
     * Remove callbacks for retrieving device phone status and phone statuses for every department
     * @param callBacks
     */
    public void removeCallBacks(StatusCallbacks callBacks) {
        this.callbacksSet.remove(callBacks);
    }

    /**
     * Fetch fresh status and department list
     */
    public void getAll() {
        isGettingAll = true;
        getDevice(true);
    }

    /**
     * Fetch current device phone status using phoneId from current account
     */
    public void getDevice(boolean forceFetch) {
        if (isAvailable == null || forceFetch) {
            Api.getDevicePhoneStatus(activity, phoneId, deviceCallbacks);
        } else {
            for (StatusCallbacks item : callbacksSet) {
                item.onDevice(isAvailable, null);
            }
        }
    }

    /**
     * Update current device phone status
     * @param requestedStatus is requested new status
     */
    public void updateDevice(boolean requestedStatus) {
        if (deviceId != null) {
            Api.updateDevicePhoneStatus(activity, deviceId, agentId, requestedStatus, deviceCallbacks);
        }
    }

    /**
     * Get phone statuses for every department. You can call this method after calling getDevice().
     */
    public void getDepartments() {
        if (deviceId == null) {
            String errMsg = "Cannot call 'getDepartments() before calling 'getDevice()'";
            for (StatusCallbacks item : callbacksSet) {
                item.onDepartmentList(null, new Exception(errMsg));
            }
            Logger.e(TAG, errMsg);
            return;
        }
        Api.getDepartmentStatusList(activity, deviceId, new Api.DepartmentStatusListCallback() {

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
                for (StatusCallbacks item : callbacksSet) {
                    item.onDepartmentList(list, e);
                }
            }

        });
    }


}
