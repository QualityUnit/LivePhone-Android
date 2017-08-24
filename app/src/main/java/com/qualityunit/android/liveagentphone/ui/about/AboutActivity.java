package com.qualityunit.android.liveagentphone.ui.about;

import android.os.Bundle;

import com.qualityunit.android.liveagentphone.ui.common.ToolbarActivity;

/**
 * Created by rasto on 21.8.17.
 */

public class AboutActivity extends ToolbarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            addFragment(new AboutFragment(), AboutFragment.TAG);
        }
    }

    @Override
    protected boolean allwaysShowHomeAsUp() {
        return true;
    }

}
