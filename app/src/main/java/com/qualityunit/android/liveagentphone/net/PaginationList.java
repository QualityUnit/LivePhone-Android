package com.qualityunit.android.liveagentphone.net;

import android.app.Activity;
import android.os.Bundle;

import com.qualityunit.android.liveagentphone.util.Logger;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.qualityunit.android.liveagentphone.net.PaginationList.InitFlag.LOAD;
import static com.qualityunit.android.liveagentphone.net.PaginationList.InitFlag.RELOAD;

/**
 * Powerful list handler with features: load, next page, refresh.
 * Instance of this class use in retained fragment which targets to fragment containing list<br/>
 * Simply implement 'loadList' method (runs in background implicitly), which loads data from server.
 * In case of error throw an exception with message. Do not forget to implement an interface to get callbacks
 * from this list handler<br/><br/>
 * Created by rasto on 28.11.16.
 */
public abstract class PaginationList<T> {

    private static final String TAG = PaginationList.class.getSimpleName();
    private List<T> list = new ArrayList<>();
    private CallbackListener<T> listener;
    private State currentState;
    private int firstPage;
    private int perPage;
    private String requestTag;

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
     * @param requestTag unique string tag to be used to cancel requests
     */
    public PaginationList (int firstPage, int perPage, String requestTag) {
        this.firstPage = firstPage;
        this.perPage = perPage;
        this.currentState = new State(firstPage, null);
        this.requestTag = requestTag;
    }

    // *********** CONTROLS *****************

    /**
     * When list already exists return list otherwise load first page
     * @param initFlag @see {@link InitFlag}
     * @param args optional arguments that they are available in loadList() method
     */
    public void init (Activity activity, int initFlag, @Nullable Bundle args) {
        State newState = currentState.createCopy().setInitiated(true);
        if ((initFlag == LOAD  && !currentState.isInitiated()) || initFlag == RELOAD) {
            list.clear();
            newState = new State(firstPage,  args);
            newState.setInitiated(true)
                    .setLoaded(true)
                    .setLoading(true);
            makeRequest(activity, firstPage, args);
        }
        show(newState, list);
    }

    /**
     * Clear all data and load fresh first page (to change args, you must call init() with RELOAD flag)
     */
    public void refresh (Activity activity) {
        if (currentState.isRefreshing()) {
            return;
        }
        State newState = currentState.createCopy()
                .setRefreshing(true)
                .setLoaded(true);
        makeRequest(activity, firstPage, currentState.getArgs());
        show(newState, list);
    }

    /**
     * Load next page
     */
    public void nextPage (Activity activity) {
        if (currentState.isRefreshing()) {
            Logger.d(TAG, "Error: Loading next page already running. Cannot fire again.");
            return;
        }
        State newState = currentState.createCopy()
                .setRefreshing(true)
                .setLoaded(true);
        makeRequest(activity, currentState.getPage() + 1, currentState.getArgs());
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

    /**
     * Get error message and state
     * @param newState
     * @param errorMsg
     */
    private void showError(State newState, String errorMsg) {
        currentState = newState;
        if (listener != null) {
            listener.onError(list, errorMsg, currentState);
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

    /**
     * Clear the list (set state as before first load)
     */
    public void clear() {
        list.clear();
        State newState = new State(firstPage,  null)
                .setInitiated(true);
        show(newState, list);
    }

    public State getCurrentState() {
        return currentState;
    }

    // *********** ABSTRACT METHODS ****************

    /**
     * Server call implementation, which sends just request
     * @param activity
     * @param pageToLoad
     * @param args
     * @throws Exception when error has been occurred and callback onError will bring and exception message
     */
    public abstract void requestPage(Activity activity, String requestTag, int pageToLoad, Bundle args, Client.Callback<List<T>> callback);

    // *********** PRIVATE METHODS **********

    private void makeRequest(Activity activity, final int pageToLoad, Bundle args) {
        Client.cancel(activity, requestTag);
        requestPage(activity, requestTag, pageToLoad, args, new Client.Callback<List<T>>() {

            @Override
            public void onSuccess(List<T> newList) {
                State newState = currentState.createCopy()
                        .setRefreshing(false)
                        .setLoading(false);
                if (pageToLoad == firstPage) {
                    // refreshing list
                    list.clear();
                }
                newState.setPage(pageToLoad);
                list.addAll(newList);
                if (list.size() < perPage) {
                    newState.setLastPage(true);
                    if (list.isEmpty()) {
                        newState.setEmpty(true);
                    }
                }
                show(newState, list);
            }

            @Override
            public void onFailure(Exception e) {
                State newState = currentState.createCopy()
                        .setRefreshing(false)
                        .setLoading(false);
                showError(newState, e.getMessage());
            }

        });
    }

    // ************* INNER CLASSES ******************

    public interface CallbackListener<T> {

        /**
         * Get current list
         * @param list
         * @param listState state of pagination list
         */
        void onGetList(@NonNull List<T> list, @NonNull State listState);

        /**
         * When error is occured, tihs methos is called
         * @param list
         * @param errorMessage details about error
         * @param listState state of pagination list
         */
        void onError(@NonNull List<T> list, @Nullable String errorMessage, @NonNull State listState);
    }

    public static class State {

        private boolean isInitiated; // true after created an instance of list with given flag
        private boolean isLoaded; // true after first load started
        private boolean isLastPage; // true when last page is loaded
        private boolean isLoading; // true while loading first page
        private boolean isRefreshing; // true while refreshing entire list or loading next page
        private boolean isEmpty; // true after empty response comes
        private int page; // current page loaded
        private Bundle args;

        public State(int page, @Nullable Bundle args) {
            this.page = page;
            this.args = args;
        }

        public State(boolean isInitiated, boolean isLoaded, boolean isLastPage, boolean isLoading, boolean isRefreshing, boolean isEmpty, int page, @Nullable Bundle args) {
            this.isInitiated = isInitiated;
            this.isLoaded = isLoaded;
            this.isLastPage = isLastPage;
            this.isLoading = isLoading;
            this.isRefreshing = isRefreshing;
            this.isEmpty = isEmpty;
            this.page = page;
            this.args = args;
        }

        public State createCopy() {
            return new State(isInitiated, isLoaded, isLastPage, isLoading, isRefreshing, isEmpty, page, args == null ? null : new Bundle(args));
        }

        @Override
        public String toString() {
            return "State{" +
                    "isInitiated=" + isInitiated +
                    ", isLoaded=" + isLoaded +
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

        public State setLoaded(boolean loaded) {
            isLoaded = loaded;
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

        public boolean isLoaded() {
            return isLoaded;
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

}
