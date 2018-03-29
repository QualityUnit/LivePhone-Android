package com.qualityunit.android.liveagentphone.ui.home;

import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
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
import com.qualityunit.android.liveagentphone.util.Tools;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by rasto on 15.12.15.
 */
public class ContactsListAdapter extends ArrayAdapter<ContactsItem> {

    private static final String TAG = ContactsListAdapter.class.getSimpleName();
    private static final int layout = R.layout.contacts_list_item;
    private final String baseUrl;
    private final Drawable defaultAvatar;
    private Context context;

    public ContactsListAdapter(Context context, List<ContactsItem> list) {
        super(context, layout, list);
        this.context = context;
        AccountManager accountManager = AccountManager.get(context);
        baseUrl = accountManager
                .getUserData(LaAccount.get(), LaAccount.USERDATA_URL_API)
                .replace(Const.Api.API_POSTFIX, "");
        defaultAvatar = ContextCompat.getDrawable(context, R.drawable.ll_avatar);
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
        final ContactsItem item = getItem(position);
        if(item != null) {
            String contactName = Tools.createContactName(item.firstname, item.lastname, item.system_name);
            viewHolder.avatar.setImageDrawable(defaultAvatar);
            if (!item.emails.isEmpty()) {
                String gravatarUrl = "https://www.gravatar.com/avatar/" + Tools.MD5Util.md5Hex(item.emails.get(0)) + "?d=404";
                Picasso.with(context).load(gravatarUrl).placeholder(defaultAvatar).transform(new CircleTransform()).into(viewHolder.avatar, new Callback() {

                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        loadServerAvatar(baseUrl, item, viewHolder);
                    }
                });
            } else if (!TextUtils.isEmpty(item.avatar_url)) {
                loadServerAvatar(baseUrl, item, viewHolder);
            }
            viewHolder.text.setText(contactName);
        }
        return convertView;
    }

    private void loadServerAvatar(String baseUrl, ContactsItem item, ViewHolder viewHolder) {
        String url;
        if (item.avatar_url.contains("__BASE_URL__")) {
            url = baseUrl + item.avatar_url.replace("__BASE_URL__", "/");
        } else {
            url = item.avatar_url;
        }
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(context).load(url).placeholder(defaultAvatar).transform(new CircleTransform()).into(viewHolder.avatar);
        }
    }

    public static class ViewHolder {

        TextView text;
        ImageView avatar;

        public ViewHolder(View convertView) {
            text = (TextView) convertView.findViewById(R.id.tv_text);
            avatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
        }
    }

}
