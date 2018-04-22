package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.Const;
import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.ui.common.CircleTransform;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by rasto on 15.12.15.
 */
public class InternalListAdapter extends ArrayAdapter<InternalItem> {

    private static final String TAG = InternalListAdapter.class.getSimpleName();

    private static final String STATUS_ENABLED = "E";
    private static final String STATUS_DISABLED = "D";
    private static final String STATUS_ACTIVE = "A";
    private static final int layout = R.layout.internal_list_item;

    private final String baseUrl;
    private Context context;

    public InternalListAdapter(Context context, List<InternalItem> list) {
        super(context, layout, list);
        this.context = context;
        AccountManager accountManager = AccountManager.get(context);
        baseUrl = accountManager
                .getUserData(LaAccount.get(), LaAccount.USERDATA_URL_API)
                .replace(Const.Api.API_POSTFIX, "");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewStatic(context, baseUrl, getItem(position), convertView, parent);
    }

    public static View getViewStatic(Context context, String baseUrl, InternalItem item, View convertView, ViewGroup parent) {
        Drawable defaultAvatarAgent = ContextCompat.getDrawable(context, R.drawable.ll_avatar);
        Drawable defaultAvatarDepartment = ContextCompat.getDrawable(context, R.drawable.ll_department);
        final ViewHolder viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        if(item != null) {
            if (item.agent == null) {
                // deparment extension
                viewHolder.avatar.setImageDrawable(defaultAvatarDepartment);
                viewHolder.name.setText(item.department.departmentName);
                viewHolder.department.setText("");
                viewHolder.department.setVisibility(View.GONE);
            } else {
                // agent extension
                viewHolder.avatar.setImageDrawable(defaultAvatarAgent);
                viewHolder.name.setText(item.agent.name);
                viewHolder.department.setText(item.department.departmentName);
                viewHolder.department.setVisibility(View.VISIBLE);
                String avatarUrl = item.agent.avatarUrl;
                if (avatarUrl.contains("www.gravatar.com")) {
                    if (avatarUrl.startsWith("//")) {
                        avatarUrl = "https:" + avatarUrl;
                    }
                    Picasso.with(context).load(avatarUrl).placeholder(defaultAvatarAgent).transform(new CircleTransform()).into(viewHolder.avatar);
                } else if (!TextUtils.isEmpty(avatarUrl)) {
                    loadServerAvatar(context, defaultAvatarAgent, baseUrl, item, viewHolder);
                }
            }
            resolveStatus(item, viewHolder);
        }
        return convertView;
    }

    public static void resolveStatus(InternalItem item, ViewHolder viewHolder) {
        switch (item.status) {
            case STATUS_ACTIVE:
                viewHolder.status.setBackgroundResource(R.drawable.bg_extension_active);
                break;
            case STATUS_DISABLED:
                viewHolder.status.setBackgroundResource(R.drawable.bg_extension_disabled);
                break;
            case STATUS_ENABLED:
                viewHolder.status.setBackgroundResource(R.drawable.bg_extension_enabled);
                break;
            default:
                Log.d(TAG, "resolveStatus: Unknown status:'" + item.status + "'");
                viewHolder.status.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public static void loadServerAvatar(Context context, Drawable defaultAvatarAgent, String baseUrl, InternalItem item, ViewHolder viewHolder) {
        String url;
        url = baseUrl + item.agent.avatarUrl.replace("__BASE_URL__", "/");
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(context).load(url).placeholder(defaultAvatarAgent).transform(new CircleTransform()).into(viewHolder.avatar);
        }
    }

    public static class ViewHolder {

        ImageView avatar;
        TextView name;
        TextView department;
        View status;

        public ViewHolder(View convertView) {
            avatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
            name = (TextView) convertView.findViewById(R.id.tv_name);
            department = (TextView) convertView.findViewById(R.id.tv_department);
            status = convertView.findViewById(R.id.v_status);
        }
    }

}
