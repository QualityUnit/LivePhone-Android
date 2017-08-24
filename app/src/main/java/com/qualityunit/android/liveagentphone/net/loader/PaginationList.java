package com.qualityunit.android.liveagentphone.net.loader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Powerful list handler with features: load, next page, refresh.
 * Instance of this class use in retained fragment which targets to fragment containing list<br/>
 * Simply implement 'loadList' method (runs in background implicitly), which loads data from server.
 * In case of error throw an exception with message. Do not forget to implement an interface to get callbacks
 * from this list handler<br/><br/>
 * Created by rasto on 28.11.16.
 */
public abstract class PaginationList<T> implements Handler.Callback {

    private static final String TAG = PaginationList.class.getSimpleName();
    private BackgroundTaskThread thread;
    private Handler mainHandler = new Handler(this);
    private List<T> list = new ArrayList<>();
    private CallbackListener<T> listener;
    private ListState listState = new ListState();
    private int firstPage;
    private int perPage;

    /**
     * Define which page is first and how many items you can set for server call
     * @param firstPage define first page. Some server APIs consider page 0 as a first page, some of them page 1
     * @param perPage define number of items in page. How many items per page you want do get from your API
     */
    public PaginationList (int firstPage, int perPage) {
        this.firstPage = firstPage;
        this.perPage = perPage;
    }

    // *********** CONTROLS *****************

    /**
     * When list already exists return list otherwise load first page
     * @param args these arg will be available in 'loadList' params
     * @param reinit set this flag to true if you want to reset list
     */
    public void init (@NonNull Bundle args, boolean reinit) {
        if (reinit || !listState.isInitiated) {
            list.clear();
            this.listState = new ListState(true, false, true, false, false, 0, args);
            show();
            resetBackgroundTask(firstPage);
        } else {
            show();
        }
    }

    /**
     * Clear all data and load first page
     * @param args if null then use previous args
     */
    public void refresh (@Nullable Bundle args) {
        if (listState.isRefreshing) {
            return;
        }
        if (args == null) {
             args = this.listState.args;
        }
        this.listState = new ListState(true, false, false, true, false, 0, args);
        resetBackgroundTask(firstPage);
    }

    /**
     * Load next page
     */
    public void nextPage () {
        if (listState.isRefreshing) {
            Log.e(TAG, "Error: Loading next page already running. Cannot fire again.");
            return;
        }
        listState.isRefreshing = true;
        resetBackgroundTask(listState.page + 1);
    }

    /**
     * Clear list and force stop any ongoing server call
     */
    public void clear () {
        if (thread != null) {
            thread.kill();
        }
        list.clear();
        this.listState = new ListState(true, false, false, false, false, 0, listState.args);
        show();
    }

    /**
     * Get current list and state
     */
    public void show () {
        if (listener != null && listState.isInitiated) {
            listener.onGetList(list, listState);
        }
    }

    /**
     * Set listener which will get all callbacks from this list handler
     * @param listener
     */
    public void setListener (@Nullable CallbackListener<T> listener) {
        // set null if you don't want to get callback anymore
        this.listener = listener;
    }

    // *********** ABSTRACT METHODS ****************

    /**
     * Server call implementation, which returns a new list of items
     * @param pageToLoad requested page
     * @param args args you set in 'init' method and you can change them anytime in 'refresh' method
     * @return new list of items
     * @throws Exception when error has been occurred and callback onError will bring and exception message
     */
    public abstract List<T> loadList (int pageToLoad, Bundle args) throws Exception;

    // *********** PRIVATE METHODS **********

    private void resetBackgroundTask (int pageToLoad) {
        if (listState.isLastPage) {
            show();
            return;
        }
        if (thread != null) {
            thread.kill();
        }
        thread = new BackgroundTaskThread<> (mainHandler, new BackgroundTaskThread.LoadListTask<T>() {
            @Override
            public List<T> loadListInBackground (int pageToLoad, Bundle args) throws Exception {
                return loadList(pageToLoad, args);
            }
        }, pageToLoad, listState.args);
        thread.start();
    }

    @Override
    public boolean handleMessage (Message msg) {
        thread = null;
        listState.isRefreshing = false;
        listState.isLoading = false;
        listState.isEmpty = false;
        listState.isLastPage = false;
        if (msg.what == BackgroundTaskThread.SUCCESS) {
            List<T> newList = (List<T>) msg.obj;
            if (msg.arg1 == firstPage) {
                // refreshing list
                list.clear();
            }
            listState.page = msg.arg1;
            list.addAll(newList);
            if (newList.size() < perPage) {
                listState.isLastPage = true;
                if (newList.isEmpty()) {
                    listState.isEmpty = true;
                }
            }
            show();
        } else if (msg.what == BackgroundTaskThread.FAILURE) {
            String errorMsg = (String) msg.obj;
            if (listener != null && listState.isInitiated) {
                listener.onError(errorMsg);
            }
            show();
        }
        return true;
    }

    // ************* INNER CLASSES ******************

    public interface CallbackListener<T> {

        /**
         * Get current list
         * @param list if error is occurred then this is null
         * @param listState
         */
        void onGetList(@Nullable List<T> list, @NonNull ListState listState);

        /**
         * When error is occured, tihs methos is called
         * @param errorMessage details about error
         */
        void onError(String errorMessage);
    }

    public static class ListState {

        private boolean isInitiated;
        private boolean isLastPage;
        private boolean isLoading;
        private boolean isRefreshing;
        private boolean isEmpty;
        private int page = 0;
        private Bundle args;

        /**
         * Create list state instance and set all flags to false
         */
        public ListState() {
            // java sets desired values implicitly
        }

        private ListState(boolean isInitiated, boolean isLastPage, boolean isLoading, boolean isRefreshing, boolean isEmpty, int page, Bundle args) {
            this.isInitiated = isInitiated;
            this.isLastPage = isLastPage;
            this.isLoading = isLoading;
            this.isRefreshing = isRefreshing;
            this.isEmpty = isEmpty;
            this.page = page;
            this.args = args;
        }

        public boolean isInitiated() {
            return isInitiated;
        }

        public boolean isLastPage() {
            return isLastPage;
        }

        public boolean isLoading() {
            return isLoading;
        }

        public boolean isRefreshing() {
            return isRefreshing;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public int getPage() {
            return page;
        }

        public Bundle getArgs() {
            return args;
        }

        @Override
        public String toString() {
            return "ListState{" +
                    "isInitiated=" + isInitiated +
                    ", isLastPage=" + isLastPage +
                    ", isLoading=" + isLoading +
                    ", isRefreshing=" + isRefreshing +
                    ", isEmpty=" + isEmpty +
                    ", page=" + page +
                    ", args=" + args.toString() +
                    '}';
        }
    }

    private static class BackgroundTaskThread<T> extends Thread {

        private static final int SUCCESS = 0;
        private static final int FAILURE = 1;
        private volatile Handler handler;
        private LoadListTask<T> loadListTask;
        private int pageToLoad;
        private Bundle args;

        private BackgroundTaskThread(Handler handler, LoadListTask<T> loadListTask, int pageToLoad, Bundle args) {
            this.handler = handler;
            this.loadListTask = loadListTask;
            this.pageToLoad = pageToLoad;
            this.args = args;
        }

        private interface LoadListTask<T> {
            List<T> loadListInBackground(int pageToLoad, Bundle args) throws Exception;
        }

        @Override
        public void run() {
            try {
                List<T> newList = loadListTask.loadListInBackground(this.pageToLoad, this.args);
                if (handler != null) {
                    if (newList == null) {
                        throw new Exception("Empty list");
                    }
                    Message message = new Message();
                    message.what = SUCCESS;
                    message.arg1 = pageToLoad;
                    message.obj = newList;
                    handler.sendMessage(message);
                }
            } catch (Exception e) {
                if (handler != null) {
                    Message message = new Message();
                    message.what = FAILURE;
                    message.arg1 = pageToLoad;
                    message.obj = e.getMessage();
                    handler.sendMessage(message);
                }
            }
        }

        private void kill() {
            handler = null;
        }
    }

}
