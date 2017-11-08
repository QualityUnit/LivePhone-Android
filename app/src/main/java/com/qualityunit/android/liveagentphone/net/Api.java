package com.qualityunit.android.liveagentphone.net;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.rest.ApiException;
import com.qualityunit.android.liveagentphone.net.rest.Client;
import com.qualityunit.android.liveagentphone.ui.status.DepartmentStatusItem;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;

/**
 * Created by rasto on 31.10.17.
 */

public class Api {

    private interface ApiPrepare {
        void onApiReady(String apiBasePath, String apiKey, Exception e);
    }

    private static void prepare(final Handler handler, final Activity activity, final ApiPrepare apiPrepare) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                final Account account = LaAccount.get();
                final AccountManager accountManager = AccountManager.get(activity);
                accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Bundle result = future.getResult();
                            String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                            String token = result.getString(AccountManager.KEY_AUTHTOKEN);
                            apiPrepare.onApiReady(basePath, token, null);
                        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                            apiPrepare.onApiReady(null, null, e);
                        }
                    }
                }, handler);
            }
        });
    }

    public interface DeviceCallback {
        void onResponse(Boolean isAvailable, Integer deviceId, String agentId, Exception e);
    }

    public static void getDevicePhoneStatus(final Activity activity, final String phoneId, final DeviceCallback deviceCallback) {
        final Handler handler = new Handler(Looper.myLooper());
        prepare(handler, activity, new ApiPrepare() {
            @Override
            public void onApiReady(final String apiBasePath, final String apiKey, Exception e) {
                if (e != null) {
                    deviceCallback.onResponse(null, null, null, e);
                    return;
                }
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject filters = new JSONObject();
                            filters.put("phoneId", phoneId);
                            filters.put("service_type", "P");
                            final Client client = Client.getInstance();
                            final Request request = client
                                    .GET(apiBasePath, "/devices", apiKey)
                                    .addEncodedParam("_filters", URLEncoder.encode(filters.toString(), "utf-8"))
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (!response.isSuccessful()) {
                                throw new ApiException(response.message(), response.code());
                            }
                            JSONArray array = new JSONArray(response.body().string());
                            if (array.length() < 1) {
                                throw new ApiException("Phone service not found", response.code());
                            }
                            JSONObject object = array.getJSONObject(0);
                            final int newDeviceId = object.getInt("id");
                            final String newAgentId = object.getString("agent_id");
                            final boolean newIsAvailable = "N".equals(object.getString("status"));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    deviceCallback.onResponse(newIsAvailable, newDeviceId, newAgentId, null);
                                }
                            });
                        } catch (JSONException | IOException | ApiException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    deviceCallback.onResponse(null, null, null, e);
                                }
                            });
                        }
                    }
                })).start();
            }
        });
    }

    public static void updateDevicePhoneStatus(final Activity activity, final int deviceId, final String agentId, final boolean requestedStatus, final DeviceCallback deviceCallback) {
        final Handler handler = new Handler(Looper.myLooper());
        prepare(handler, activity, new ApiPrepare() {
            @Override
            public void onApiReady(final String apiBasePath, final String apiKey, Exception e) {
                if (e != null) {
                    deviceCallback.onResponse(!requestedStatus, deviceId, agentId, e);
                    return;
                }
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("agent_id", agentId);
                            body.put("service_type", "P");
                            body.put("status", requestedStatus ? "N" : "F");
                            final Client client = Client.getInstance();
                            final Request request = client
                                    .PUT(apiBasePath, "/devices/" + deviceId, apiKey)
                                    .setBody(body.toString())
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (!response.isSuccessful()) {
                                throw new ApiException(response.message(), response.code());
                            }
                            JSONObject object = new JSONObject(response.body().string());
                            final boolean newStatus = "N".equals(object.getString("status"));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    deviceCallback.onResponse(newStatus, deviceId, agentId, null);
                                }
                            });
                        } catch (JSONException | IOException | ApiException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    deviceCallback.onResponse(!requestedStatus, deviceId, agentId, e);
                                }
                            });
                        }
                    }
                })).start();
            }
        });
    }

    public interface DepartmentStatusListCallback {
        void onResponse(JSONArray array, Exception e);
    }

    public static void getDepartmentStatusList(final Activity activity, final int deviceId, final DepartmentStatusListCallback departmetsCallback) {
        final Handler handler = new Handler(Looper.myLooper());
        prepare(handler, activity, new ApiPrepare() {
            @Override
            public void onApiReady(final String apiBasePath, final String apiKey, Exception e) {
                if (e != null) {
                    departmetsCallback.onResponse(null, e);
                    return;
                }
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final Client client = Client.getInstance();
                            final Request request = client
                                    .GET(apiBasePath, "/devices/" + deviceId + "/departments", apiKey)
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (!response.isSuccessful()) {
                                throw new ApiException(response.message(), response.code());
                            }
                            final JSONArray array = new JSONArray(response.body().string());
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    departmetsCallback.onResponse(array, null);
                                }
                            });
                        } catch (JSONException | IOException | ApiException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    departmetsCallback.onResponse(null, e);
                                }
                            });
                        }
                    }
                })).start();
            }
        });
    }

    public interface DepartmetStatusCallback {
        void onResponse(DepartmentStatusItem departmentStatusItem, Exception e);
    }

    public static void updateDepartmentStatus(final Activity activity, final DepartmentStatusItem item, final boolean requestedStatus, final DepartmetStatusCallback departmetStatusCallback) {
        final Handler handler = new Handler(Looper.myLooper());
        prepare(handler, activity, new ApiPrepare() {
            @Override
            public void onApiReady(final String apiBasePath, final String apiKey, Exception e) {
                if (e != null) {
                    departmetStatusCallback.onResponse(null, e);
                    return;
                }
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject body = new JSONObject();
                            body.put("user_id", item.userId);
                            body.put("department_name", item.departmentId);
                            body.put("online_status", requestedStatus ? "N" : "F");
                            final Client client = Client.getInstance();
                            final Request request = client
                                    .PUT(apiBasePath, "/devices/" + item.deviceId + "/departments/" + item.departmentId, apiKey)
                                    .setBody(body.toString())
                                    .build();
                            Response response = client.newCall(request).execute();
                            if (!response.isSuccessful()) {
                                throw new ApiException(response.message(), response.code());
                            }
                            JSONObject object = new JSONObject(response.body().string());
                            item.onlineStatus = "N".equals(object.getString("online_status"));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    departmetStatusCallback.onResponse(item, null);
                                }
                            });
                        } catch (JSONException | IOException | ApiException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    departmetStatusCallback.onResponse(item, e);
                                }
                            });
                        }
                    }
                })).start();
            }
        });
    }

}
