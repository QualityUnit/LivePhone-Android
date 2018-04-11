package com.qualityunit.android.liveagentphone.ui.home;

import android.os.Bundle;

import com.qualityunit.android.liveagentphone.net.loader.PaginationList;
import com.qualityunit.android.liveagentphone.net.rest.ApiException;
import com.qualityunit.android.liveagentphone.net.rest.Client;
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
public class InternalStore {

    public static final int FIRST_PAGE = 1;
    public static final int ITEMS_PER_PAGE = 9999;
    public static String TAG = InternalStore.class.getSimpleName();
    private static InternalStore instance;
    private InternalPaginationList ipl = new InternalPaginationList();
    private String basePath;
    private String token;

    public static InternalStore getInstance() {
        if (instance == null) {
            instance = new InternalStore();
        }
        return instance;
    }

    private InternalStore() {
    }

    // ******** API ************

    public void init (PaginationList.CallbackListener<InternalItem> callbackListener, String basePath, String token) {
        this.basePath = basePath;
        this.token = token;
        ipl.setListener(callbackListener);
        ipl.init(createArgs(), false);
    }

    /**
     * Avoid to call any callback method
     */
    public void stop () {
        ipl.setListener(null);
    }

    public void refresh () {
        ipl.refresh(null);
    }

    public void nextPage () {
        ipl.nextPage();
    }

    public void clear () {
        ipl.clear();
    }

    // ************ private methods ************

    private Bundle createArgs() {
        Bundle bundle = new Bundle();
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
            String basePath = stringFromArg(args, "basePath");
            String token = stringFromArg(args, "token");
            return getExtensions(basePath, token, pageToLoad, ITEMS_PER_PAGE);
        }

        private String stringFromArg(Bundle args, String string) throws Exception {
            if (!args.containsKey(string)) {
                throw new Exception("Missing '" + string + "'!");
            }
            return args.getString(string);
        }

        private List<InternalItem> getExtensions(String basePath, String token, int requestedPage, int itemsPerPage) throws IOException, ApiException {
            List<InternalItem> list = new ArrayList<>();
            Client client = Client.getInstance();
            Request request = client.GET(basePath, "/extensions", token)
                    .addParam("_perPage", itemsPerPage)
                    .addParam("_page", requestedPage)
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
