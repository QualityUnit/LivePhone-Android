package com.qualityunit.android.liveagentphone.ui.call;

import android.view.View;
import android.widget.AdapterView;

import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.ui.home.InternalFragment;
import com.qualityunit.android.liveagentphone.ui.home.InternalItem;

import androidx.fragment.app.FragmentActivity;

public class TransferFragment extends InternalFragment {

    public static final String TAG = TransferFragment.class.getSimpleName();

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        InternalItem item = getAdapter().getItem(position);
        String remoteName = item.agent == null ? item.department.departmentName : item.agent.name;
        CallingCommands.transferCallExtension(getContext(), item.number, remoteName);
        FragmentActivity activity = getActivity();
        if (activity != null) activity.onBackPressed();
    }
}
