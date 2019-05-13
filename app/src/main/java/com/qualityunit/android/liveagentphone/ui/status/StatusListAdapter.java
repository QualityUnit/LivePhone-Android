package com.qualityunit.android.liveagentphone.ui.status;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.Client;

import java.util.List;

import static com.qualityunit.android.liveagentphone.Const.OnlineStatus.STATUS_ONLINE_FLAG;

/**
 * Created by rasto on 15.12.15.
 */
public class StatusListAdapter extends ArrayAdapter<DepartmentStatusItem> {

    private static final int layout = R.layout.status_list_item;
    private Activity activity;

    public StatusListAdapter(Activity activity, List<DepartmentStatusItem> list) {
        super(activity, layout, list);
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final DepartmentStatusItem item = getItem(position);
        if(item != null) {
            viewHolder.nameView.setText(item.departmentName);
            if (!TextUtils.isEmpty(item.onlineStatus)) {
                viewHolder.switchView.setOnCheckedChangeListener(null);
                viewHolder.switchView.setChecked(STATUS_ONLINE_FLAG.equals(item.onlineStatus));
                viewHolder.switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                        Client.updateDepartmentStatus(activity, item, isChecked, new Client.Callback<String>() {
                            @Override
                            public void onSuccess(String statusFlag) {
                                item.onlineStatus = statusFlag;
                                // UI switch is already moved, do nothing
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                                viewHolder.switchView.setChecked(STATUS_ONLINE_FLAG.equals(item.onlineStatus));
                            }
                        });
                    }

                });
                viewHolder.switchView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.switchView.setVisibility(View.INVISIBLE);
            }
        }
        return convertView;
    }

    public static class ViewHolder {

        TextView nameView;
        Switch switchView;

        public ViewHolder(View convertView) {
            nameView = (TextView) convertView.findViewById(R.id.tv_name);
            switchView = (Switch) convertView.findViewById(R.id.s_mobileStatus);
        }
    }

}
