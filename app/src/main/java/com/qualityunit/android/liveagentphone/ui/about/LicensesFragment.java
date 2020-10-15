package com.qualityunit.android.liveagentphone.ui.about;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;

/**
 * Created by rasto on 21.8.17.
 */

public class LicensesFragment extends BaseFragment<AboutActivity> {

    public static final String TAG = LicensesFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.licenses_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.licenses);
        ((WebView) view.findViewById(R.id.wv_content)).loadUrl("file:///android_asset/licenses.html");
    }
}
