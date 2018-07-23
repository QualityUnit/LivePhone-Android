package com.qualityunit.android.liveagentphone.ui.about;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;

/**
 * Created by rasto on 21.8.17.
 */

public class AboutFragment extends BaseFragment<AboutActivity> {

    public static final String TAG = AboutFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.about);
        ((TextView)view.findViewById(R.id.tv_version)).setText(Tools.getVersionName(getContext()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.about_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_licenses:
                activity.switchFragments(new LicensesFragment(), LicensesFragment.TAG);
                break;
            case R.id.item_send_logfile:
                Logger.sendLogFile(getContext());
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
