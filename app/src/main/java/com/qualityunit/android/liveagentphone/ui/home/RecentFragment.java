package com.qualityunit.android.liveagentphone.ui.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 8.11.17.
 */

public class RecentFragment extends Fragment {

    public static final String TAG = RecentFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.recent_fragment, container, false);
    }

}
