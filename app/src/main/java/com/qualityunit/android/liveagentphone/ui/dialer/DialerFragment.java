package com.qualityunit.android.liveagentphone.ui.dialer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.loader.GenericLoader;
import com.qualityunit.android.liveagentphone.net.loader.LoaderResult;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.service.CallingCommands;
import com.qualityunit.android.liveagentphone.service.CallingException;
import com.qualityunit.android.liveagentphone.ui.common.BaseFragment;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class DialerFragment extends BaseFragment<DialerActivity> {

    public static final String TAG = DialerFragment.class.getSimpleName();
    private static final int LOADER_ID_PHONES = 0;
    private PhonesLoaderCallbacks phonesLoaderCallbacks;
    private TextView tvOutgoingnumber;
    private AlertDialog.Builder outgoingNumberPicker;
    private OutGoingItem outGoingNumber;
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
        if (phonesLoaderCallbacks == null) {
            phonesLoaderCallbacks = new PhonesLoaderCallbacks();
        }
        activity.getSupportLoaderManager().initLoader(LOADER_ID_PHONES, null, phonesLoaderCallbacks);
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
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loader for populating outgoing phones
     */
    private static class PhonesLoader extends GenericLoader<Response> {

        public PhonesLoader(Activity activity) {
            super(activity);
        }

        @Override
        protected Response requestData(String basePath, String token) throws IOException {
            JSONObject filters = new JSONObject();
            try {
                filters.put("type", "S"); // default
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
            Client client = Client.getInstance();
            Request request = client
                    .GET(basePath, "/phone_numbers", token)
                    .addEncodedParam("_filters", URLEncoder.encode(filters.toString(), "utf-8"))
                    .build();
            return client.newCall(request).execute();
        }

    }

    /**
     * LoaderCallbacks for PhonesGetLoader
     */
    private class PhonesLoaderCallbacks implements LoaderManager.LoaderCallbacks<LoaderResult<Response>> {


        @Override
        public Loader<LoaderResult<Response>> onCreateLoader(int id, Bundle args) {
            return new PhonesLoader(activity);
        }

        @Override
        public void onLoadFinished(Loader<LoaderResult<Response>> loader, LoaderResult<Response> data) {
            if (data.getObject().body() == null) {
                String errMsg = "Missing response body";
                Logger.e(TAG, Tools.formatError(errMsg));
                Toast.makeText(getContext(), errMsg, Toast.LENGTH_SHORT).show();
                return;
            }
            final List<OutGoingItem> list = new ArrayList<>();
            try {
                JSONArray array = new JSONArray(data.getObject().body().string()); // WARNING - string() can be performed only once
                for (int i = 0; i < array.length(); i++) {
                    JSONObject jsonItem = array.getJSONObject(i);
                    list.add(new OutGoingItem(
                            Tools.getStringFromJson(jsonItem, "id"),
                            Tools.getStringFromJson(jsonItem, "number"),
                            Tools.getStringFromJson(jsonItem, "name"),
                            Tools.getStringFromJson(jsonItem, "departmentid"),
                            Tools.fixDecimalsBefore(Tools.getIntFromJson(jsonItem, "dial_out_prefix"), 2)
                    ));
                }
            } catch (IOException | JSONException e) {
                Toast.makeText(activity, "Could not load phone numbers", Toast.LENGTH_LONG).show();
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (list.isEmpty()) {
                        tvOutgoingnumber.setText(getString(R.string.no_outgoing_numbers));
                    }
                    else {
                        if (list.size() == 1) {
                            outGoingNumber = list.get(0);
                            tvOutgoingnumber.setText(createSpinText(outGoingNumber));
                        }
                        else {
                            final OutGoingAdapter outGoingAdapter = new OutGoingAdapter(getActivity());
                            for (OutGoingItem outGoingItem : list) {
                                if (TextUtils.isEmpty(outGoingItem.number)) {
                                    Toast.makeText(getActivity(), "Error: phoneNumber cannot be null or empty", Toast.LENGTH_LONG).show();
                                }
                                else {
                                    outGoingAdapter.add(outGoingItem);
                                }
                            }
                            if (outGoingAdapter.isEmpty()) {
                                tvOutgoingnumber.setText(getString(R.string.no_outgoing_numbers));
                            }
                            else {
                                outgoingNumberPicker = new AlertDialog.Builder(getContext());
                                outgoingNumberPicker.setTitle(getString(R.string.choose_number));
                                outgoingNumberPicker.setAdapter(outGoingAdapter, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pickOutgoingNumber(which, outGoingAdapter);
                                    }
                                });
                                pickOutgoingNumber(pickedOutgoingNumberIndex, outGoingAdapter);
                            }
                        }
                    }
                }

                private void pickOutgoingNumber(int index, OutGoingAdapter outGoingAdapter) {
                    if (index < 0) {
                        tvOutgoingnumber.setText("");
                    }
                    else {
                        pickedOutgoingNumberIndex = index;
                        outGoingNumber = outGoingAdapter.getItem(pickedOutgoingNumberIndex);
                        tvOutgoingnumber.setText(createSpinText(outGoingNumber));
                    }
                }
            });

        }

        @Override
        public void onLoaderReset(Loader<LoaderResult<Response>> loader) {

        }
    }

    private String createSpinText(OutGoingItem outGoingItem) {
        String name = outGoingItem.name.trim();
        String number = outGoingItem.number.trim();
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
    private static class OutGoingAdapter extends ArrayAdapter<OutGoingItem> {

        private static final int layout = R.layout.dialer_outgoing_item;

        public OutGoingAdapter(Context context, List<OutGoingItem> list) {
            super(context, layout, list);
        }

        public OutGoingAdapter(Context context) {
            super(context, layout, new ArrayList<OutGoingItem>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            OutGoingAdapter.ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                viewHolder = new OutGoingAdapter.ViewHolder(convertView);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (OutGoingAdapter.ViewHolder) convertView.getTag();
            }
            OutGoingItem item = getItem(position);
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

    private static class OutGoingItem {

        public OutGoingItem(String id, String number, String name, String departmentid, String dial_out_preffix) {
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
