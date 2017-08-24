package com.qualityunit.android.liveagentphone.ui.common;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by rasto
 */
public class BaseFragment<A extends AppCompatActivity> extends Fragment {

    protected A activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (A) context;
    }

}
