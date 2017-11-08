package com.qualityunit.android.liveagentphone.ui.home.availability;

import java.util.List;

/**
 * Created by rasto on 31.10.17.
 */

public interface AvailabilityCallbacks {

    void onDevice(Boolean isAvailable, Exception e);
    void onDepartmentList(List<DepartmentStatusItem> list, Exception e);
}
