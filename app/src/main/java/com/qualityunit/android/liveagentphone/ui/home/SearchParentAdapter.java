package com.qualityunit.android.liveagentphone.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qualityunit.android.liveagentphone.R;
import com.qualityunit.android.liveagentphone.net.loader.PaginationList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchParentAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnChildClickListener {

    private final Map<Integer, SearchChildAdapter> cm = new HashMap<>();
    private final List<SearchChildAdapter> list = new ArrayList<>();
    private final Context context;

    /**
     * Create new adapter with empty lists
     * @param context
     */
    public SearchParentAdapter(Context context) {
        this.context = context;
    }

    public void setChildAdapter(int index, SearchChildAdapter searchChildAdapter) {
        SearchChildAdapter previousChildAdapter = cm.get(index);
        if (previousChildAdapter == null) {
            list.add(searchChildAdapter);
        } else {
            list.set(list.indexOf(previousChildAdapter), searchChildAdapter);
        }
        cm.put(index, searchChildAdapter);
        notifyDataSetChanged();
    }

    public void removeChildAdapter(int index) {
        list.remove(cm.remove(index));
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return list.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return list.get(groupPosition).getList().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return list.get(groupPosition).getTitle();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return list.get(groupPosition).getList().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final GroupViewHolder viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof GroupViewHolder)) {
            convertView = LayoutInflater.from(context).inflate(R.layout.search_group, parent, false);
            viewHolder = new GroupViewHolder(convertView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (GroupViewHolder) convertView.getTag();
        }
        viewHolder.tvTitle.setText(list.get(groupPosition).getTitle());
        PaginationList.State state = list.get(groupPosition).getState();
        if (state != null) {
            if (state.isRefreshing() || state.isLoading()) {
                viewHolder.pbLoading.setVisibility(View.VISIBLE);
            } else {
                viewHolder.pbLoading.setVisibility(View.GONE);
            }
        }
        ((ExpandableListView) parent).expandGroup(groupPosition);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return list.get(groupPosition).getChildView(childPosition,isLastChild, convertView, parent, list.get(groupPosition).getState());
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        SearchChildAdapter.OnClickListener onClickListener = list.get(groupPosition).getOnClickListener();
        if (onClickListener != null) {
            return onClickListener.onChildClick(list.get(groupPosition).getList().get(childPosition));
        }
        return false;
    }

    private static class GroupViewHolder {

        private TextView tvTitle;
        private ProgressBar pbLoading;

        public GroupViewHolder(View convertView) {
            tvTitle = (TextView) convertView.findViewById(R.id.tv_title);
            pbLoading = (ProgressBar) convertView.findViewById(R.id.pb_loading);
        }

    }

}
