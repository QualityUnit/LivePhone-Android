package com.qualityunit.android.liveagentphone.ui.dialer;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.Client;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class DialerFragment extends BaseFragment<DialerActivity> {

    public static final String TAG = DialerFragment.class.getSimpleName();
    private TextView tvOutgoingnumber;
    private AlertDialog.Builder outgoingNumberPicker;
    private OutgoingNumberItem outGoingNumber;
    private EditText etDialInput;
    // instance variables
    private int pickedOutgoingNumberIndex = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialer_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab_makeCall);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                makeCall();
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etDialInput = (EditText) view.findViewById(R.id.et_dialInput);
        tvOutgoingnumber = (TextView) view.findViewById(R.id.tv_outgoingNumber);
        tvOutgoingnumber.setHint(getString(R.string.choose_number));
        tvOutgoingnumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (outgoingNumberPicker != null) {
                    outgoingNumberPicker.show();
                }
            }
        });
        if (savedInstanceState != null) {
            pickedOutgoingNumberIndex = savedInstanceState.getInt("pickedOutgoingNumberIndex", -1);
        } else {
            // first start
            etDialInput.setText(getActivity().getIntent().getStringExtra("number"));
        }
        startInit();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("pickedOutgoingNumberIndex", pickedOutgoingNumberIndex);
        super.onSaveInstanceState(outState);
    }

    /**
     * Start to initialize view
     */
    private void startInit() {
        Client.getPhoneNumbers(activity, new Client.Callback<List<OutgoingNumberItem>>() {
            @Override
            public void onSuccess(List<OutgoingNumberItem> list) {
                if (list.isEmpty()) {
                    tvOutgoingnumber.setText(getString(R.string.no_outgoing_numbers));
                }
                else {
                    if (list.size() == 1) {
                        outGoingNumber = list.get(0);
                        tvOutgoingnumber.setText(createSpinText(outGoingNumber));
                    }
                    else {
                        final OutgoingAdapter outgoingAdapter = new OutgoingAdapter(getActivity());
                        for (OutgoingNumberItem outgoingNumberItem : list) {
                            if (TextUtils.isEmpty(outgoingNumberItem.number)) {
                                Toast.makeText(getActivity(), "Error: phoneNumber cannot be null or empty", Toast.LENGTH_LONG).show();
                            }
                            else {
                                outgoingAdapter.add(outgoingNumberItem);
                            }
                        }
                        if (outgoingAdapter.isEmpty()) {
                            tvOutgoingnumber.setText(getString(R.string.no_outgoing_numbers));
                        }
                        else {
                            outgoingNumberPicker = new AlertDialog.Builder(getContext());
                            outgoingNumberPicker.setTitle(getString(R.string.choose_number));
                            outgoingNumberPicker.setAdapter(outgoingAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    pickOutgoingNumber(which, outgoingAdapter);
                                }
                            });
                            pickOutgoingNumber(pickedOutgoingNumberIndex, outgoingAdapter);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void pickOutgoingNumber(int index, OutgoingAdapter outgoingAdapter) {
        if (index < 0) {
            tvOutgoingnumber.setText("");
        }
        else {
            pickedOutgoingNumberIndex = index;
            outGoingNumber = outgoingAdapter.getItem(pickedOutgoingNumberIndex);
            tvOutgoingnumber.setText(createSpinText(outGoingNumber));
        }
    }

    /**
     * Make call
     */
    public void makeCall() {
        try {
            if (outGoingNumber == null) throw new CallingException(getString(R.string.cannot_call) + " " + getString(R.string.no_outgoing_numbers));
            String phoneNumber = etDialInput.getText().toString().trim();
            if (TextUtils.isEmpty(phoneNumber)) throw new CallingException(getString(R.string.callee_empty));
            String contactName = getActivity().getIntent().getStringExtra("contactName");
            CallingCommands.makeCall(activity, phoneNumber, outGoingNumber.dial_out_preffix, contactName);
        } catch (CallingException e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private String createSpinText(OutgoingNumberItem outgoingNumberItem) {
        String name = outgoingNumberItem.name.trim();
        String number = outgoingNumberItem.number.trim();
        if (TextUtils.isEmpty(name) || number.equals(name)) {
            return number;
        }
        else {
            return name + " (" + number + ")";
        }
    }

    /**
     * Adapter for spinner 'outgoing number'
     */
    private static class OutgoingAdapter extends ArrayAdapter<OutgoingNumberItem> {

        private static final int layout = R.layout.dialer_outgoing_item;

        public OutgoingAdapter(Context context) {
            super(context, layout, new ArrayList<OutgoingNumberItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            OutgoingAdapter.ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                viewHolder = new OutgoingAdapter.ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (OutgoingAdapter.ViewHolder) convertView.getTag();
            }
            OutgoingNumberItem item = getItem(position);
            if(item != null) {
                fillTextView(viewHolder.name, item.name);
                fillTextView(viewHolder.number, item.number);
            }
            return convertView;
        }

        private void fillTextView(TextView textView, String value) {
            if (!TextUtils.isEmpty(value)) {
                value = value.trim();
                textView.setText(value);
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setText("");
                textView.setVisibility(View.GONE);
            }
        }

        public static class ViewHolder {

            TextView name;
            TextView number;

            public ViewHolder(View convertView) {
                name = (TextView) convertView.findViewById(R.id.tv_name);
                number = (TextView) convertView.findViewById(R.id.tv_number);
            }
        }
    }

    public static class OutgoingNumberItem {

        public OutgoingNumberItem(String id, String number, String name, String departmentid, String dial_out_preffix) {
            this.id = id;
            this.number = number;
            this.name = name;
            this.departmentid = departmentid;
            this.dial_out_preffix = dial_out_preffix;
        }

        protected String id;
        protected String number;
        protected String name;
        protected String departmentid;
        protected String dial_out_preffix;
    }

}
