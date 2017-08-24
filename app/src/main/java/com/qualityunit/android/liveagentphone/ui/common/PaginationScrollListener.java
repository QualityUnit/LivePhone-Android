package com.qualityunit.android.liveagentphone.ui.common;

import android.widget.AbsListView;

/**
 * Created by rasto on 30.11.16.
 */

public class PaginationScrollListener implements AbsListView.OnScrollListener {

    private boolean isLoading;
    private OnNextPageListener onNextPageListener;
    private int threshold;
    private int previousTotalItemCount;

    public PaginationScrollListener(OnNextPageListener onNextPageListener, int threshold) {
        this.onNextPageListener = onNextPageListener;
        this.threshold = threshold;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Don't take any action on changed
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (totalItemCount == 0) {
            previousTotalItemCount = totalItemCount;
            return;
        }
        int lastVisibleItem = firstVisibleItem + visibleItemCount;
        int triggerItem = totalItemCount - threshold;
        if (isLoading && previousTotalItemCount != totalItemCount) {
            isLoading = false;
        }
        if (!isLoading && lastVisibleItem >= triggerItem) {
            isLoading = true;
            previousTotalItemCount = totalItemCount;
            onNextPageListener.onNextPage();
        }
    }

    public interface OnNextPageListener {
        void onNextPage();
    }

}
