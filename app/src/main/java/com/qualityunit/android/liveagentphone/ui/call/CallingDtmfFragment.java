package com.qualityunit.android.liveagentphone.ui.call;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Tools;

public class CallingDtmfFragment extends BaseFragment<CallingActivity> implements TextWatcher {

    public static final String TAG = CallingDtmfFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.calling_dtmf_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextInputEditText tietDtmfinput = (TextInputEditText) view.findViewById(R.id.tiet_dtmfInput);
        tietDtmfinput.addTextChangedListener(this);
        Tools.showKeyboard(getContext(), tietDtmfinput);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        try {
            if (count == 1) {
                CallingCommands.sendDtmf(getContext(), s.subSequence(start, s.length()).toString());
            }
        } catch (CallingException e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
