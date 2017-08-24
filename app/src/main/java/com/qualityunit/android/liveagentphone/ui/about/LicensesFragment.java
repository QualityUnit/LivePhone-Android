package com.qualityunit.android.liveagentphone.ui.about;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 21.8.17.
 */

public class LicensesFragment extends Fragment {

    public static final String TAG = LicensesFragment.class.getSimpleName();
    private AboutActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AboutActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_licenses, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity.setTitle(R.string.licenses);
        ((WebView) view.findViewById(R.id.wv_content)).loadUrl("file:///android_asset/licenses.html");
    }
}
