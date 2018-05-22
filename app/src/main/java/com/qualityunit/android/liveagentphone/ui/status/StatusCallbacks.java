package com.qualityunit.android.liveagentphone.ui.status;

import java.util.List;

/**
 * Created by rasto on 31.10.17.
 */

public interface StatusCallbacks {

    void onDevices(int mobilePhoneStatus, int browserPhoneStatus, Exception e);
    void onDepartmentList(List<DepartmentStatusItem> list, Exception e);
}
