package com.qualityunit.android.liveagentphone.ui.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.PaginationList;

import java.util.List;

public class InternalChildAdapter extends SearchChildAdapter<InternalItem> {

    public InternalChildAdapter(Context context, List<InternalItem> list, PaginationList.State state, OnClickListener<InternalItem> onClickListener) {
        super(context, context.getString(R.string.internal), list, state, onClickListener);
    }

    @Override
    public View getChildView(int position, boolean isLast, View convertView, ViewGroup parent, PaginationList.State state) {
        return InternalListAdapter.getViewStatic(context, baseUrl, getList().get(position), convertView, parent);
    }

}