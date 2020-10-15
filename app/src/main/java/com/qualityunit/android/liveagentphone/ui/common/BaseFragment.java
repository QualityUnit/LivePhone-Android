package com.qualityunit.android.liveagentphone.ui.common;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;

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
