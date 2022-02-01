package com.qualityunit.android.liveagentphone.ui.dialer;

import android.os.Bundle;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.ui.common.ToolbarActivity;

/**
 * Created by rasto on 31.08.16.
 */
public class DialerActivity extends ToolbarActivity {

    @Override
    protected boolean allwaysShowHomeAsUp() {
        return true;
    }

    @Override
    protected int getContentViewRes() {
        return R.layout.dialer_activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            addFragment(new DialerFragment(), DialerFragment.TAG);
        }

    }

    @Override
    public void onPause() {
        overridePendingTransition(0, R.anim.fade_out);
        super.onPause();
    }

}
