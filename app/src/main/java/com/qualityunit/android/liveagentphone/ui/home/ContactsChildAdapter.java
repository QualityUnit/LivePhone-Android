package com.qualityunit.android.liveagentphone.ui.home;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.ui.common.CircleTransform;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ContactsChildAdapter extends SearchChildAdapter<ContactsItem> {

    private static final int layout = R.layout.contacts_list_item;
    private final Drawable defaultAvatar;

    public ContactsChildAdapter(Context context, List<ContactsItem> list, PaginationList.State state, OnClickListener<ContactsItem> onClickListener) {
        super(context, context.getString(R.string.contacts), list, state, onClickListener);
        defaultAvatar = ContextCompat.getDrawable(context, R.drawable.ll_avatar);
    }

    @Override
    public View getChildView(int position, boolean isLast, View convertView, ViewGroup parent, PaginationList.State state) {
        final ContactViewHolder viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof ContactViewHolder)) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ContactViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ContactViewHolder) convertView.getTag();
        }
        final ContactsItem item = getList().get(position);
        if(item != null) {
            String contactName = Tools.createContactName(item.getFirstName(), item.getLastName(), item.getSystemName());
            viewHolder.avatar.setImageDrawable(defaultAvatar);
            if (item.getEmails() != null && !item.getEmails().isEmpty()) {
                String gravatarUrl = "https://www.gravatar.com/avatar/" + Tools.MD5Util.md5Hex(item.getEmails().get(0)) + "?d=404";
                Picasso.with(context).load(gravatarUrl).placeholder(defaultAvatar).transform(new CircleTransform()).into(viewHolder.avatar, new Callback() {

                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError() {
                        loadServerAvatar(baseUrl, item, viewHolder);
                    }
                });
            } else if (!TextUtils.isEmpty(item.getAvatarUrl())) {
                loadServerAvatar(baseUrl, item, viewHolder);
            }
            viewHolder.textPrimary.setText(contactName);
            if (item.getPhones() != null && !item.getPhones().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String phone : item.getPhones()) {
                    if (sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(phone);
                }
                viewHolder.textSecodary.setVisibility(View.VISIBLE);
                viewHolder.textSecodary.setText(sb.toString());
            } else {
                viewHolder.textSecodary.setVisibility(View.GONE);
                viewHolder.textSecodary.setText("");
            }
        }
        return convertView;
    }

    private void loadServerAvatar(String baseUrl, ContactsItem item, ContactViewHolder viewHolder) {
        String url;
        if (item.getAvatarUrl().contains("__BASE_URL__")) {
            url = baseUrl + item.getAvatarUrl().replace("__BASE_URL__", "/");
        } else {
            url = item.getAvatarUrl();
        }
        if (!TextUtils.isEmpty(url)) {
            Picasso.with(context).load(url).placeholder(defaultAvatar).transform(new CircleTransform()).into(viewHolder.avatar);
        }
    }

    private static class ContactViewHolder {

        TextView textPrimary;
        TextView textSecodary;
        ImageView avatar;

        public ContactViewHolder(View convertView) {
            textPrimary = (TextView) convertView.findViewById(R.id.tv_text_primary);
            textSecodary = (TextView) convertView.findViewById(R.id.tv_text_secondary);
            avatar = (ImageView) convertView.findViewById(R.id.iv_avatar);
        }
    }
}