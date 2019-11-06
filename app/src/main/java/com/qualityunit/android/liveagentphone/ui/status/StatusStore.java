package com.qualityunit.android.liveagentphone.ui.status;

import android.accounts.AccountManager;
import android.app.Activity;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.Client;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.qualityunit.android.liveagentphone.Const.OnlineStatus.STATUS_ONLINE_FLAG;

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
    public static final String STATUS_TYPE_PRESET = "preset_status";
    public static final String STATUS_TYPE_ONLINE = "online_status";
    private static StatusStore instance;
    private Activity activity;
    private String phoneId;
    private Map<String, JSONObject> devices = new HashMap<>();
    private final Set<StatusCallbacks> callbacksSet = new HashSet<>();

    private void notifyOnDevices(Exception e) {
        try {
            // For mobile, we're showing STATUS_TYPE_PRESET. It should be the same as STATUS_TYPE_ONLINE
            // For browser, we have to show SREAD_PHONE_STATETATUS_TYPE_ONLINE. STATUS_TYPE_PRESET can be true but STATUS_TYPE_ONLINE finally reflects opened Agent panel in browser
            int mobileStatus = getDeviceStatus(DEVICE_TYPE_MOBILE, STATUS_TYPE_PRESET);
            int browserStatus = getDeviceStatus(DEVICE_TYPE_BROWSER, STATUS_TYPE_ONLINE);
            for (StatusCallbacks item : callbacksSet) {
                item.onDevices(mobileStatus, browserStatus, e);
            }
        } catch (JSONException ex) {
            for (StatusCallbacks item : callbacksSet) {
                item.onDevices(PHONE_STATUS_NULL, PHONE_STATUS_NULL, e);
            }
        }

    }

    private int getDeviceStatus(String deviceType, String statusType) throws JSONException {
        int status = PHONE_STATUS_NULL;
        if (devices != null) {
            JSONObject device = devices.get(deviceType);
            if (device != null) {
                String mobileStatus = device.getString(statusType);
                if (!TextUtils.isEmpty(mobileStatus)) {
                    status = STATUS_ONLINE_FLAG.equals(mobileStatus) ? PHONE_STATUS_OUT_IN : PHONE_STATUS_OUT;
                }
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
     * Get browser and mobile devices statuses. If mobile devices does not exist then create it
     */
    public void getDevice(boolean forceFetch, final boolean withDeparments) {
        if (!forceFetch) {
            notifyOnDevices(null);
            return;
        }
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDevices();
        }
        Client.getDevicesPhoneStatus(activity, phoneId, new Client.Callback<Map<String, JSONObject>>() {
            @Override
            public void onSuccess(Map<String, JSONObject> object) {
                try {
                    devices = object;
                    if (withDeparments && getDeviceStatus(DEVICE_TYPE_MOBILE, STATUS_TYPE_PRESET) == PHONE_STATUS_OUT_IN) {
                        getDepartments(devices.get(DEVICE_TYPE_MOBILE).getString("id"));
                    }
                    notifyOnDevices(null); // phoneStatus is updated here
                } catch (JSONException e) {
                    notifyOnDevices(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                notifyOnDevices(e);
            }
        });
    }

    /**
     * Update mobile device phone preset and status
     * @param requestedStatus is requested new status
     */
    public void updateMobileDevice(final boolean requestedStatus) {
        if (!requestedStatus) {
            // clear department listview
            for (StatusCallbacks item : callbacksSet) {
                item.onDepartmentList(null, null);
            }
        }
        JSONObject deviceJsonObject = devices.get(DEVICE_TYPE_MOBILE);
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDevices();
        }
        Client.updateDevicePhoneStatus(activity, deviceJsonObject, requestedStatus, new Client.Callback<JSONObject>() {
            @Override
            public void onSuccess(JSONObject object) {
                try {
                    devices.put(DEVICE_TYPE_MOBILE, object);
                    if (requestedStatus && getDeviceStatus(DEVICE_TYPE_MOBILE, STATUS_TYPE_PRESET) == PHONE_STATUS_OUT_IN) {
                        getDepartments(devices.get(DEVICE_TYPE_MOBILE).getString("id"));
                    }
                    notifyOnDevices(null);
                } catch (JSONException e1) {
                    notifyOnDevices(e1);
                }
            }

            @Override
            public void onFailure(Exception e) {
                notifyOnDevices(e);
            }
        });
    }

    /**
     * Get phone statuses for every department. You can call this method after calling getDevice().
     * @param deviceId
     */
    public void getDepartments(String deviceId) {
        for (StatusCallbacks item : callbacksSet) {
            item.onLoadingDepartmentList();
        }
        Client.getDepartmentStatusList(activity, deviceId, new Client.Callback<List<DepartmentStatusItem>>() {
            @Override
            public void onSuccess(List<DepartmentStatusItem> list) {
                for (StatusCallbacks item : callbacksSet) {
                    item.onDepartmentList(list, null);
                }
            }

            @Override
            public void onFailure(Exception e) {
                for (StatusCallbacks item : callbacksSet) {
                    item.onDepartmentList(null, e);
                }
            }
        });
    }

}
