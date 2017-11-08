package com.qualityunit.android.liveagentphone.ui.status;

/**
 * Created by rasto on 2.11.17.
 */

public class DepartmentStatusItem {

    public int deviceId;
    public String departmentId;
    public String userId;
    public String departmentName;
    public boolean onlineStatus;

    public DepartmentStatusItem(int deviceId, String departmentId, String userId, String departmentName, boolean onlineStatus) {
        this.deviceId = deviceId;
        this.departmentId = departmentId;
        this.userId = userId;
        this.departmentName = departmentName;
        this.onlineStatus = onlineStatus;
    }

    @Override
    public String toString() {
        return "DepartmentStatusItem{" +
                "deviceId=" + deviceId +
                ", departmentId='" + departmentId + '\'' +
                ", userId='" + userId + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", onlineStatus=" + onlineStatus +
                '}';
    }
}
