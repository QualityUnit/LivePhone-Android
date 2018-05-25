package com.qualityunit.android.liveagentphone.ui.status;

import android.accounts.AccountManager;
import android.app.Activity;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.Api;
import com.qualityunit.android.liveagentphone.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by rasto on 31.10.17.
 */

public class StatusStore {

    public static final String TAG = StatusStore.class.getSimpleName();
    public static final String DEVICE_TYPE_BROWSER = "W";
    public static final String DEVICE_TYPE_MOBILE = "A";
    public static final int PHONE_STATUS_NULL = 1;
    public static final int PHONE_STATUS_OUT = 2;
    public static final int PHONE_STATUS_OUT_IN = 3;
    private static final String STATUS_ONLINE_FLAG = "N";
    private static StatusStore instance;
    private Activity activity;
    private String phoneId;
    private final Map<String, JSONObject> devices = new HashMap<>();
    private final Set<StatusCallbacks> callbacksSet = new HashSet<>();

    private void notifyOnDevices(final Exception e) throws JSONException {
        for (StatusCallbacks item : callbacksSet) {
            item.onDevices(getDeviceStatus(DEVICE_TYPE_MOBILE), getDeviceStatus(DEVICE_TYPE_BROWSER), e);
        }
    }

    private int getDeviceStatus(String deviceType) throws JSONException {
        int status = PHONE_STATUS_NULL;
        JSONObject device = devices.get(deviceType);
        if (device != null) {
            String mobileStatus = device.getString("status");
            if (!TextUtils.isEmpty(mobileStatus)) {
                status = STATUS_ONLINE_FLAG.equals(mobileStatus) ? PHONE_STATUS_OUT_IN : PHONE_STATUS_OUT;
            }
        }
        return status;
    }

    public static StatusStore getInstance(Activity activity) {
        if (instance == null) {
            instance = new StatusStore(activity);
        }
        return instance;
    }

    private StatusStore(Activity activity) {
        this.activity = activity;
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
     * Fetch current device phone status using phoneId from current account
     */
    public void getDevice(boolean forceFetch, final boolean withDeparments) {
        if (!forceFetch) {
            try {
                notifyOnDevices(null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return;
        }
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDevices();
        }
        Api.getDevicesPhoneStatus(activity, phoneId, new Api.DevicesCallback() {
            @Override
            public void onResponse(JSONArray jsonArray, Exception e) {
                try {
                    if (jsonArray == null) {
                        notifyOnDevices(new Exception("Phone devices not found"));
                        return;
                    }
                    devices.clear();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject object = jsonArray.getJSONObject(i);
                        devices.put(object.getString("type"), object);
                    }
                    if (e == null && withDeparments && getDeviceStatus(DEVICE_TYPE_MOBILE) == PHONE_STATUS_OUT_IN) {
                        getDepartments(devices.get(DEVICE_TYPE_MOBILE).getString("id"));
                    }
                    notifyOnDevices(e); // phoneStatus is updated here
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Update current device phone status
     * @param requestedStatus is requested new status
     */
    public void updateDevice(final boolean requestedStatus, String deviceType) {
        if (!requestedStatus) {
            // clear department listview
            for (StatusCallbacks item : callbacksSet) {
                item.onDepartmentList(null, null);
            }
        }
        JSONObject deviceJsonObject = devices.get(deviceType);
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDevices();
        }
        Api.updateDevicePhoneStatus(activity, deviceJsonObject, requestedStatus, new Api.UpdateDeviceCallback() {
            @Override
            public void onResponse(JSONObject jsonObject, Exception e) {
                try {
                    devices.put(jsonObject.getString("type"), jsonObject);
                    if (e == null && requestedStatus && getDeviceStatus(DEVICE_TYPE_MOBILE) == PHONE_STATUS_OUT_IN) {
                        getDepartments(devices.get(DEVICE_TYPE_MOBILE).getString("id"));
                    }
                    notifyOnDevices(e);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Get phone statuses for every department. You can call this method after calling getDevice().
     * @param deviceId
     */
    public void getDepartments(String deviceId) {
        if (deviceId == null) {
            String errMsg = "Cannot call 'getDepartments() before calling 'getDevice()'";
            for (StatusCallbacks item : callbacksSet) {
                item.onDepartmentList(null, new Exception(errMsg));
            }
            Logger.e(TAG, errMsg);
            return;
        }
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDepartmentList();
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
                                    "N".equals(obj.getString("online_status")),
                                    obj.getString("preset_status")
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
