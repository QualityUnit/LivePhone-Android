package com.qualityunit.android.liveagentphone.net.loader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static com.qualityunit.android.liveagentphone.net.loader.PaginationList.InitFlag.INSTANCE;
import static com.qualityunit.android.liveagentphone.net.loader.PaginationList.InitFlag.RELOAD;

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
    private State currentState;
    private int firstPage;
    private int perPage;

    /**
     * <li>{@link #INSTANCE}</li>
     * <li>{@link #LOAD}</li>
     * <li>{@link #RELOAD}</li>
     */
    public static class InitFlag {
        /**
         * Only create instance, do not make any loads
         */
        public static int INSTANCE = 1;
        /**
         * Create instance and fire first load, do not fire another load
         */
        public static int LOAD = 2;
        /**
         * Create instance and fire first load, fire load always when init is called
         */
        public static int RELOAD = 3;
    }

    /**
     * Define which page is first and how many items you can set for server call
     * @param firstPage define first page. Some server APIs consider page 0 as a first page, some of them page 1
     * @param perPage define number of items in page. How many items per page you want do get from your API
     */
    public PaginationList (int firstPage, int perPage) {
        this.firstPage = firstPage;
        this.perPage = perPage;
        this.currentState = new State(firstPage, null);
    }

    // *********** CONTROLS *****************

    /**
     * When list already exists return list otherwise load first page
     * @param initFlag @see {@link InitFlag}
     * @param args optional arguments that they are available in loadList() method
     */
    public void init (int initFlag, @Nullable Bundle args) {
        State newState;
        if (initFlag == INSTANCE) {
            newState = currentState.createCopy().setInitiated(true);
        } else {
            newState = new State(firstPage,  args);
            if (!currentState.isInitiated() || initFlag == RELOAD) {
                list.clear();
                newState.setLoading(true);
                resetBackgroundTask(firstPage, args);
            }
        }
        show(newState, list);
    }

    /**
     * Clear all data and load fresh first page (to change args, you must call init() with RELOAD flag)
     */
    public void refresh () {
        if (currentState.isRefreshing()) {
            return;
        }
        State newState = currentState.createCopy()
                .setRefreshing(true);
        resetBackgroundTask(firstPage, currentState.getArgs());
        show(newState, list);
    }

    /**
     * Load next page
     */
    public void nextPage () {
        if (currentState.isRefreshing()) {
            Log.d(TAG, "Error: Loading next page already running. Cannot fire again.");
            return;
        }
        State newState = currentState.createCopy()
                .setRefreshing(true);
        resetBackgroundTask(currentState.getPage() + 1, currentState.getArgs());
        show(newState, list);
    }

    /**
     * Get current list and state
     * @param newState
     * @param list
     */
    public void show(State newState, List<T> list) {
        currentState = newState;
        if (listener != null) {
            listener.onGetList(list, currentState);
        }
    }

//    /**
//     * Clear list and cancel loading thread
//     */
//    private void clean () {
//        if (thread != null) {
//            thread.forget();
//            thread = null;
//        }
//        list.clear();
//    }

    /**
     * Get error message and state
     * @param newState
     * @param errorMsg
     */
    private void showError(State newState, String errorMsg) {
        currentState = newState;
        if (listener != null) {
            listener.onError(errorMsg, currentState);
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

    private void resetBackgroundTask (int pageToLoad, Bundle args) {
        if (thread != null) {
            thread.forget();
        }
        thread = new BackgroundTaskThread<> (mainHandler, new BackgroundTaskThread.LoadListTask<T>() {
            @Override
            public List<T> loadListInBackground (int pageToLoad, Bundle args) throws Exception {
                return loadList(pageToLoad, args);
            }
        }, pageToLoad, args);
        thread.start();
    }

    @Override
    public boolean handleMessage (Message msg) {
        thread = null;
        State newState = currentState.createCopy()
                .setRefreshing(false)
                .setLoading(false);
        if (msg.what == BackgroundTaskThread.SUCCESS) {
            if (msg.arg1 == firstPage) {
                // refreshing list
                list.clear();
            }
            newState.setPage(msg.arg1);
            list.addAll((List<T>) msg.obj);
            if (list.size() < perPage) {
                newState.setLastPage(true);
                if (list.isEmpty()) {
                    newState.setEmpty(true);
                }
            }
            show(newState, list);
        } else if (msg.what == BackgroundTaskThread.FAILURE) {
            String errorMsg = (String) msg.obj;
            showError(newState, errorMsg);
        }
        return true;
    }

    // ************* INNER CLASSES ******************

    public interface CallbackListener<T> {

        /**
         * Get current list
         * @param list if error is occurred then this is null
         * @param listState state of pagination list
         */
        void onGetList(@NonNull List<T> list, @NonNull State listState);

        /**
         * When error is occured, tihs methos is called
         * @param errorMessage details about error
         * @param listState state of pagination list
         */
        void onError(@Nullable String errorMessage, @NonNull State listState);
    }

    public static class State {

        private boolean isInitiated;
        private boolean isLastPage;
        private boolean isLoading;
        private boolean isRefreshing;
        private boolean isEmpty;
        private int page;
        private Bundle args;

        public State(int page, @Nullable Bundle args) {
            this.page = page;
            this.args = args;
        }

        public State(boolean isInitiated, boolean isLastPage, boolean isLoading, boolean isRefreshing, boolean isEmpty, int page, @Nullable Bundle args) {
            this.isInitiated = isInitiated;
            this.isLastPage = isLastPage;
            this.isLoading = isLoading;
            this.isRefreshing = isRefreshing;
            this.isEmpty = isEmpty;
            this.page = page;
            this.args = args;
        }

        public State createCopy() {
            return new State(isInitiated, isLastPage, isLoading, isRefreshing, isEmpty, page, args == null ? null : new Bundle(args));
        }

        @Override
        public String toString() {
            return "State{" +
                    "isInitiated=" + isInitiated +
                    ", isLastPage=" + isLastPage +
                    ", isLoading=" + isLoading +
                    ", isRefreshing=" + isRefreshing +
                    ", isEmpty=" + isEmpty +
                    ", page=" + page +
                    ", args=" + args +
                    '}';
        }

        public State setInitiated(boolean initiated) {
            isInitiated = initiated;
            return this;
        }

        public State setLastPage(boolean lastPage) {
            isLastPage = lastPage;
            return this;
        }

        public State setLoading(boolean loading) {
            isLoading = loading;
            return this;
        }

        public State setRefreshing(boolean refreshing) {
            isRefreshing = refreshing;
            return this;
        }

        public State setEmpty(boolean empty) {
            isEmpty = empty;
            return this;
        }

        public State setPage(int page) {
            this.page = page;
            return this;
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

        private void forget() {
            handler = null;
        }
    }

}
