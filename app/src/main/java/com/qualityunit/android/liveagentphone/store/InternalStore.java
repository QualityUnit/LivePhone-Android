package com.qualityunit.android.liveagentphone.store;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.net.rest.ApiException;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.ui.common.Store;
import com.qualityunit.android.liveagentphone.ui.home.InternalItem;
import com.qualityunit.android.liveagentphone.util.Logger;
import com.qualityunit.android.liveagentphone.util.Tools;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Retained fragment for handling extensions list
 * Created by rasto on 28.11.16.
 */
public class InternalStore implements Store<InternalItem>{

    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 9999;
    public static String TAG = InternalStore.class.getSimpleName();
    private static InternalStore instance;
    private static InternalStore searchInstance;
    private InternalPaginationList ipl = new InternalPaginationList();
    private String basePath;
    private String token;

    public static InternalStore getInstance() {
        if (instance == null) {
            instance = new InternalStore();
        }
        return instance;
    }

    public static InternalStore getSearchInstance() {
        if (searchInstance == null) {
            searchInstance = new InternalStore();
        }
        return searchInstance;
    }

    private InternalStore() {
    }

    // ******** API ************

    /**
     *
     * @param basePath
     * @param token
     * @param initFlag @see {@link PaginationList.InitFlag}
     */
    @Override
    public void init (String basePath, String token, int initFlag) {
        this.basePath = basePath;
        this.token = token;
        ipl.init(initFlag, createArgs(null));
    }

    @Override
    public void reload() {
        ipl.init(PaginationList.InitFlag.RELOAD, ipl.getCurrentState().getArgs());
    }

    @Override
    public void refresh () {
        ipl.refresh();
    }

    @Override
    public void nextPage () {
        ipl.nextPage();
    }

    @Override
    public void setListener(PaginationList.CallbackListener<InternalItem> callbackListener) {
        ipl.setListener(callbackListener);
    }

    @Override
    public void search(String searchTerm) {
        ipl.init(PaginationList.InitFlag.RELOAD, createArgs(searchTerm));
    }

    @Override
    public void clear() {
        ipl.clear();
    }


    // ************ private methods ************

    private Bundle createArgs(@Nullable String searchTerm) {
        Bundle bundle = new Bundle();
        if (!TextUtils.isEmpty(searchTerm)) {
            bundle.putString("searchTerm", searchTerm);
        }
        bundle.putString("basePath", basePath);
        bundle.putString("token", token);
        return bundle;
    }

    // ********** INNER CLASSES ************

    private class InternalPaginationList extends PaginationList<InternalItem> {

        private final String TAG = InternalPaginationList.class.getSimpleName();

        public InternalPaginationList() {
            super(FIRST_PAGE, ITEMS_PER_PAGE);
        }

        @Override
        public List<InternalItem> loadList(int pageToLoad, Bundle args) throws Exception {
            JSONObject filters = new JSONObject();
            filters.put("computed_status", "A,E"); // default: show only Active and Enabled internals
            if (args.containsKey("searchTerm")) {
                filters.put("q", args.getString("searchTerm"));
            }
            String basePath = stringFromArg(args, "basePath");
            String token = stringFromArg(args, "token");
            return getExtensions(basePath, token, pageToLoad, ITEMS_PER_PAGE, filters.toString());
        }

        private String stringFromArg(Bundle args, String string) throws Exception {
            if (!args.containsKey(string)) {
                throw new Exception("Missing '" + string + "'!");
            }
            return args.getString(string);
        }

        private List<InternalItem> getExtensions(String basePath, String token, int requestedPage, int itemsPerPage, String filters) throws IOException, ApiException {
            List<InternalItem> list = new ArrayList<>();
            Client client = Client.getInstance();
            Request request = client.GET(basePath, "/extensions", token)
                    .addParam("_perPage", itemsPerPage)
                    .addParam("_page", requestedPage)
                    .addParam("_filters", filters)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    Logger.e(TAG, Tools.formatError("Missing response body"));
                    return list;
                }
                try {
                    JSONArray array = new JSONArray(response.body().string());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jsonItem = array.getJSONObject(i);
                        InternalItem.Agent agent = null;
                        JSONObject jsonAgent = null;
                        try {
                            jsonAgent = jsonItem.getJSONObject("agent");
                        } catch (JSONException ex) {
                            // do nothing, just keep agent null
                        }
                        if (jsonAgent != null) {
                            agent = new InternalItem.Agent(
                                    Tools.getStringFromJson(jsonAgent, "id"),
                                    Tools.getStringFromJson(jsonAgent, "name"),
                                    Tools.getStringFromJson(jsonAgent, "avatar_url")
                            );
                        }
                        InternalItem.Department department = null;
                        JSONObject jsonDepartment = jsonItem.getJSONObject("department");
                        if (jsonDepartment.length() != 0) {
                            department = new InternalItem.Department(
                                    Tools.getStringFromJson(jsonDepartment, "department_id"),
                                    Tools.getStringFromJson(jsonDepartment, "name"),
                                    Tools.getStringArrayFromJson(jsonDepartment, "agent_ids")
                            );
                        }
                        InternalItem internalItem = new InternalItem(
                                Tools.getStringFromJson(jsonItem, "id"),
                                Tools.getStringFromJson(jsonItem, "number"),
                                Tools.getStringFromJson(jsonItem, "status"),
                                agent,
                                department
                        );
                        list.add(internalItem);
                    }

                } catch (IOException | JSONException e) {
                    Logger.e(TAG, e.getMessage());
                }
            }
            else {
                throw new ApiException(response.message(), response.code());
            }
            return list;
        }

    }
}
