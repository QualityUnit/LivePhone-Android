package com.qualityunit.android.liveagentphone.net;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;
import com.qualityunit.android.liveagentphone.BuildConfig;
import com.qualityunit.android.liveagentphone.acc.LaAccount;
import com.qualityunit.android.liveagentphone.net.request.ArrayRequest;
import com.qualityunit.android.liveagentphone.net.request.ObjectRequest;
import com.qualityunit.android.liveagentphone.ui.dialer.DialerFragment;
import com.qualityunit.android.liveagentphone.ui.home.ContactsItem;
import com.qualityunit.android.liveagentphone.ui.home.InternalItem;
import com.qualityunit.android.liveagentphone.ui.status.DepartmentStatusItem;
import com.qualityunit.android.liveagentphone.util.Tools;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static com.android.volley.Request.Method.DELETE;
import static com.android.volley.Request.Method.GET;
import static com.android.volley.Request.Method.POST;
import static com.android.volley.Request.Method.PUT;
import static com.qualityunit.android.liveagentphone.Const.OnlineStatus.STATUS_OFFLINE_FLAG;
import static com.qualityunit.android.liveagentphone.Const.OnlineStatus.STATUS_ONLINE_FLAG;

public class Client {

    private static Client instance;
    private RequestQueue queue;
    //    private ImageLoader imageLoader;
    private static Context context;

    private Client(Context context) {
        Client.context = context;
        VolleyLog.DEBUG = BuildConfig.DEBUG;
        queue = getQueue();
//        imageLoader = new ImageLoader(queue,
//                new ImageLoader.ImageCache() {
//                    private final LruCache<String, Bitmap>
//                            cache = new LruCache<>(20);
//
//                    @Override
//                    public Bitmap getBitmap(String url) {
//                        return cache.get(url);
//                    }
//
//                    @Override
//                    public void putBitmap(String url, Bitmap bitmap) {
//                        cache.put(url, bitmap);
//                    }
//                });
    }

    private static synchronized Client getInstance(Context context) {
        if (instance == null) {
            instance = new Client(context);
        }
        return instance;
    }

    private RequestQueue getQueue() {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }

    private <T> void addToQueue(Request<T> request, String tag) {
        getQueue().cancelAll(tag);
        request.setTag(tag);
        getQueue().add(request);
    }

//    public ImageLoader getImageLoader() {
//        return imageLoader;
//    }

    private static void prepare(final Activity activity, final AuthCallback callback) {
        final AccountManager accountManager = AccountManager.get(activity.getApplicationContext());
        final Account account = LaAccount.get();
        final Handler handler = new Handler(Looper.myLooper());
        accountManager.getAuthToken(account, LaAccount.AUTH_TOKEN_TYPE, null, activity, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    String basePath = accountManager.getUserData(account, LaAccount.USERDATA_URL_API);
                    String apikey = future.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                    callback.onAuthData(getInstance(activity.getApplicationContext()), basePath, apikey);
                } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                    callback.onException(e);
                }

            }
        }, handler);
    }

    private static String createUrl (@NonNull String basepath, @NonNull String path) {
        return createUrl(basepath, path, null);
    }

    private static String createUrl (@NonNull String basepath, @NonNull String path, @Nullable Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(basepath);
        sb.append(path);
        if (params != null) {
            sb.append("?");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(String.valueOf(entry.getValue()));
                sb.append("&");
            }
            sb.deleteCharAt(sb.lastIndexOf("&"));
        }
        return sb.toString();
    }

    private static Exception processFailure(@Nullable Context context, final String token, @NonNull VolleyError e, @NonNull String tag, @Nullable AuthFallback fallback) {
        if (e.networkResponse != null) {
            int code = e.networkResponse.statusCode;
            if ((code == 401 || code == 403) && context != null) {
                AccountManager accountManager = AccountManager.get(context);
                Account account = LaAccount.get();
                accountManager.invalidateAuthToken(account.type, token);
                if (fallback != null) {
                    fallback.onFallback();
                }
            }
            return new Exception(parseMessage(e, tag), e.getCause());
        }
        return new Exception("Unknown error: " + tag, e.getCause());
    }

    private static String parseMessage(Exception e, String tag) {
        String message = e.getMessage();
        if (e instanceof VolleyError) {
            VolleyError volleyError = (VolleyError) e;
            if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                try {
                    String bodyString = new String(volleyError.networkResponse.data, "UTF-8");
                    JSONObject body = new JSONObject(bodyString);
                    message = body.getString("message");
                    return message;
                } catch (UnsupportedEncodingException | JSONException ex) {
                    message = "Network error: '" + volleyError.networkResponse.statusCode + "' while " + tag;
                }
            }
        }
        return message;
    }

    public static void cancel(Activity activity, String tag) {
        if (!TextUtils.isEmpty(tag)) {
            getInstance(activity.getApplicationContext()).getQueue().cancelAll(tag);
        }
    }

    private interface AuthCallback {
        void onAuthData(Client client, String basepath, String apikey);
        void onException(Exception e);
    }

    private interface AuthFallback {
        void onFallback();
    }

    public interface Callback<T> {
        void onSuccess(T object);
        void onFailure(Exception e);
    }

    public interface LoginCallback {
        void onInvalidPassword();
        void onVerificationCodeRequired();
        void onVerificationCodeFailure();
        void onTooManyLogins();
        void onFailure(Exception e);
        void onSuccess(String apikey, String apikeyId);
    }

    /////////////////////// API ///////////////////////

    public static void ping(final Activity activity, @NonNull String basepath, final Callback<JSONObject> callback) {
        if (TextUtils.isEmpty(basepath)) {
            return;
        }
        String url = createUrl(basepath, "/ping");
        final String tag = "GET " + url;
        ObjectRequest request = new ObjectRequest(GET, url, null, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject object) {
                callback.onSuccess(object);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                callback.onFailure(new Exception(parseMessage(e, tag)));
            }
        });
        getInstance(activity.getApplicationContext()).addToQueue(request, tag);
    }

    public static void login(final Context context, final String basepath, final String username, final String password, @Nullable final String verificationCode, final LoginCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("login", username);
            body.put("password", password);
            body.put("type", "P");
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'");
            df.setTimeZone(tz);
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.YEAR, 1);
            String validToDate = df.format(cal.getTime());
            body.put("valid_to_date", validToDate);
            body.put("name", Tools.getDeviceName());
            body.put("installid", Tools.getDeviceUniqueId());
            if (!TextUtils.isEmpty(verificationCode)) {
                body.put("two_factor_token", verificationCode);
            }
            String bodyString = body.toString();
            String url = createUrl(basepath, "/apikeys/_login");
            final String tag = "POST " + url;
            ObjectRequest request = new ObjectRequest(POST, url, null, bodyString, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject object) {
                    // Bad server implementation - temporary FIX
                    final String message = object.optString("message");
                    if (!TextUtils.isEmpty(message)) {
                        if ("Two-factor authentication required.".equals(message)) {
                            callback.onVerificationCodeRequired();
                        } else if ("Invalid two-factor verification code.".equals(message)) {
                            callback.onVerificationCodeFailure();
                        }
                        return;
                    }
                    String apikey = object.optString("key");
                    String apikeyId = object.optString("id");
                    if (TextUtils.isEmpty(apikey)) {
                        callback.onFailure(new Exception("Error: API token not found in login response"));
                        return;
                    }
                    callback.onSuccess(apikey, apikeyId);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError e) {
                    if (e.networkResponse == null) {
                        callback.onFailure(new Exception(parseMessage(e, tag)));
                        return;
                    }
                    int code = e.networkResponse.statusCode;
                    switch (code) {
                        case 401:
                        case 403:
                            callback.onInvalidPassword();
                            return;
                        case 424:
                            callback.onVerificationCodeRequired();
                            return;
                        case 425:
                            callback.onVerificationCodeFailure();
                            return;
                        case 429:
                            callback.onTooManyLogins();
                            return;
                    }
                    callback.onFailure(new Exception(parseMessage(e, tag)));
                }
            }) {
                @Override
                public RetryPolicy getRetryPolicy() {
                    return new DefaultRetryPolicy(30000, 0, 1);
                }
            };
            getInstance(context.getApplicationContext()).addToQueue(request, tag);
        } catch (JSONException e) {
            callback.onFailure(e);
        }
    }

    public static void logout(final Activity activity, final Callback<JSONObject> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(final Client client, final String basepath, final String apikey) {
                final AccountManager accountManager = AccountManager.get(context);
                final String deviceId = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_DEVICE_ID);
                final String url = createUrl(basepath, "/devices/" + deviceId);
                final String deleteDeviceTag = "DELETE " + url;
                ObjectRequest deleteDeviceRequest = new ObjectRequest(DELETE, url, apikey, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response == null) {
                            callback.onFailure(new Exception("Empty response body from '" + url + "'"));
                            return;
                        }
                        // continue logout - delete apikey
                        deleteApikey(activity, client, basepath, apikey, callback);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onFailure(processFailure(activity, apikey, error, deleteDeviceTag, new AuthFallback() {
                            @Override
                            public void onFallback() {
                                logout(activity, callback);
                            }
                        }));
                    }
                });
                client.addToQueue(deleteDeviceRequest, deleteDeviceTag);
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private static void deleteApikey(final Activity activity, final Client client, final String basepath, final String apikey, final Callback<JSONObject> callback) {
        final AccountManager accountManager = AccountManager.get(context);
        final String apikeyId = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_APIKEY_ID);
        final String url = createUrl(basepath, "/apikeys/" + apikeyId);
        final String deleteApikeyTag = "DELETE " + url;
        ObjectRequest deleteApikeyRequest = new ObjectRequest(DELETE, url, apikey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response == null) {
                    callback.onFailure(new Exception("Empty response body from '" + url + "'"));
                    return;
                }
                accountManager.removeAccountExplicitly(LaAccount.get());
                callback.onSuccess(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onFailure(processFailure(activity, apikey, error, deleteApikeyTag, new AuthFallback() {
                    @Override
                    public void onFallback() {
                        deleteApikey(activity, client, basepath, apikey, callback);
                    }
                }));
            }
        });
        client.addToQueue(deleteApikeyRequest, deleteApikeyTag);
    }

    /**
     * Get SIP credentials and LA 'phoneId'
     * @param activity
     * @param callback
     */
    public static void getPhone(final Activity activity, final Callback<JSONObject> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                final String url = createUrl(basepath, "/phones/_app_");
                final String tag = "GET " + url;
                ObjectRequest request = new ObjectRequest(GET, url, apikey, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject object) {
                        callback.onSuccess(object);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        callback.onFailure(processFailure(context, apikey, e, tag, new AuthFallback() {
                            @Override
                            public void onFallback() {
                                getPhone(activity, callback);
                            }
                        }));
                    }
                });
                client.addToQueue(request, tag);
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Update phone params - subscribe to push notifications
     * @param context
     * @param basepath
     * @param phoneId
     * @param apikey
     * @param params
     * @param callback
     */
    public static void updatePhoneParams(final Context context, final String basepath, final String phoneId, final String apikey, final Map<String,Object> params, final Callback<JSONObject> callback) {
        String path = "/phones/" + phoneId + "/_updateParams";
        String url = createUrl(basepath, path, params);
        final String tag = "PUT " + url;
        ObjectRequest request = new ObjectRequest(PUT, url, apikey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject object) {
                callback.onSuccess(object);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                callback.onFailure(processFailure(context, apikey, e, tag, new AuthFallback() {
                    @Override
                    public void onFallback() {
                        updatePhoneParams(context, basepath, phoneId, apikey, params, callback);
                    }
                }));
            }
        });
        getInstance(context.getApplicationContext()).addToQueue(request, tag);
    }

    /**
     * Get LA phone numbers
     * @param activity
     * @param callback
     */
    public static void getPhoneNumbers(final Activity activity, final Callback<List<DialerFragment.OutgoingNumberItem>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final JSONObject filters = new JSONObject() {{
                        put("type", "S");
                    }};
                    final String url = createUrl(basepath, "/phone_numbers", new HashMap<String, Object>() {{
                        put("_filters", filters.toString());
                    }});
                    final String tag = "GET " + url;
                    ArrayRequest request = new ArrayRequest(GET, url, apikey, null, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray array) {
                            try {
                                int arrayLength = array.length();
                                List<DialerFragment.OutgoingNumberItem> list = new ArrayList<>(arrayLength);
                                for (int i = 0; i < arrayLength; i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    list.add(new DialerFragment.OutgoingNumberItem(
                                            obj.optString("id"),
                                            obj.optString("number"),
                                            obj.optString("name"),
                                            obj.optString("departmentid"),
                                            obj.has("dial_out_prefix_formatted") ? obj.getString("dial_out_prefix_formatted") : obj.getString("dial_out_prefix") // TODO remove 'dial_out_prefix' later
                                    ));
                                }
                                callback.onSuccess(list);
                            } catch (JSONException e) {
                                callback.onFailure(e);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError e) {
                            callback.onFailure(processFailure(activity, apikey, e, tag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    getPhoneNumbers(activity, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(request, tag);
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Get devices presets and status (both devices browser and mobile). If mobile device does not exist (while first time running the app) then create it.
     * @param activity
     * @param phoneId
     * @param callback
     */
    public static void getDevicesPhoneStatus(final Activity activity, final String phoneId, final Callback<Map<String, JSONObject>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(final Client client, final String basepath, final String apikey) {
                try {
                    final JSONObject filters = new JSONObject();
                    filters.put("phone_id", phoneId);
                    filters.put("service_type", "P");
                    final String url = createUrl(basepath, "/devices", new HashMap<String, Object>(){{
                        put("_filters", filters.toString());
                    }});
                    final String getDevicesTag = "GET " + url;
                    final ArrayRequest getDevicesRequest = new ArrayRequest(GET, url, apikey, null, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            try {
                                final AccountManager accountManager = AccountManager.get(context);
                                final Map<String, JSONObject> devices = new HashMap<>();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    String type = object.getString("type");
                                    devices.put(type, object);
                                    if ("A".equals(type)) {
                                        // mobile device already exists - return statuses
                                        String deviceId = object.getString("id");
                                        accountManager.setUserData(LaAccount.get(), LaAccount.USERDATA_DEVICE_ID, deviceId);
                                        callback.onSuccess(devices);
                                        return;
                                    }
                                }
                                // does not contain mobile device, let's create mobile device
                                createMobileDevice(activity, phoneId, devices, callback);
                            } catch (JSONException e) {
                                callback.onFailure(e);
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError e) {
                            callback.onFailure(processFailure(activity, apikey, e, getDevicesTag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    getDevicesPhoneStatus(activity, phoneId, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(getDevicesRequest, getDevicesTag);
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    private static void createMobileDevice(final Activity activity, final String phoneId, final Map<String, JSONObject> devices, final Callback<Map<String, JSONObject>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final AccountManager accountManager = AccountManager.get(context);
                    final String agentId = accountManager.getUserData(LaAccount.get(), LaAccount.USERDATA_AGENT_ID);
                    final JSONObject body = new JSONObject();
                    body.put("phone_id", phoneId);
                    body.put("agent_id", agentId);
                    body.put("type", "A");
                    body.put("service_type", "P");
                    body.put("online_status", STATUS_ONLINE_FLAG);
                    body.put("preset_status", STATUS_ONLINE_FLAG);
                    final String url = createUrl(basepath, "/devices");
                    final String postDeviceTag = "POST " + url;
                    ObjectRequest postDeviceRequest = new ObjectRequest(POST, url, apikey, body.toString(), new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (response == null) {
                                callback.onFailure(new Exception("Empty response body from '" + url + "'"));
                                return;
                            }
                            // add deviceId into account manager
                            try {
                                String deviceId = response.getString("id");
                                accountManager.setUserData(LaAccount.get(), LaAccount.USERDATA_DEVICE_ID, deviceId);
                            } catch (Exception e) {
                                callback.onFailure(e);
                                return;
                            }
                            devices.put("A", response);
                            callback.onSuccess(devices);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onFailure(processFailure(activity, apikey, error, postDeviceTag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    createMobileDevice(activity, phoneId, devices, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(postDeviceRequest, postDeviceTag);
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Update device preset and status
     * @param activity
     * @param deviceJsonObject
     * @param requestedStatus
     * @param callback
     */
    public static void updateDevicePhoneStatus(final Activity activity, final JSONObject deviceJsonObject, final boolean requestedStatus, final Callback<JSONObject> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final String deviceId = deviceJsonObject.getString("id");
                    deviceJsonObject.put("device_status", requestedStatus ? STATUS_ONLINE_FLAG : STATUS_OFFLINE_FLAG);
                    deviceJsonObject.put("preset_status", requestedStatus ? STATUS_ONLINE_FLAG : STATUS_OFFLINE_FLAG);
                    final String url = createUrl(basepath, "/devices/" + deviceId);
                    final String tag = "PUT " + url;
                    ObjectRequest request = new ObjectRequest(PUT, url, apikey, deviceJsonObject.toString(), new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject object) {
                            callback.onSuccess(object);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onFailure(processFailure(activity, apikey, error, tag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    updateDevicePhoneStatus(activity, deviceJsonObject, requestedStatus, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(request, tag);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Get presets and statuses of all departments
     * @param activity
     * @param deviceId
     * @param callback
     */
    public static void getDepartmentStatusList(final Activity activity, final String deviceId, final Callback<List<DepartmentStatusItem>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                final String url = createUrl(basepath, "/devices/" + deviceId + "/departments", new HashMap<String, Object>(){{
                    put("_page", 0);
                    put("_perPage", 999);
                }});
                final String tag = "GET " + url;
                ArrayRequest request = new ArrayRequest(GET, url, apikey, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray array) {
                        int arrayLength = array.length();
                        List<DepartmentStatusItem> list = new ArrayList<>(arrayLength);
                        for (int i = 0; i < arrayLength; i++) {
                            try {
                                JSONObject obj = array.optJSONObject(i);
                                list.add(new DepartmentStatusItem(
                                        obj.getInt("device_id"),
                                        obj.getString("department_id"),
                                        obj.optString("user_id"),
                                        obj.optString("department_name"),
                                        obj.optString("online_status")
                                ));
                            } catch (JSONException e) {
                                callback.onFailure(e);
                                return;
                            }
                        }
                        callback.onSuccess(list);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        callback.onFailure(processFailure(activity, apikey, error, tag, new AuthFallback() {
                            @Override
                            public void onFallback() {
                                getDepartmentStatusList(activity, deviceId, callback);
                            }
                        }));
                    }
                });
                client.addToQueue(request, tag);
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Change department preset and online status
     * @param activity
     * @param item
     * @param requestedStatus
     * @param callback
     */
    public static void updateDepartmentStatus(final Activity activity, final DepartmentStatusItem item, final boolean requestedStatus, final Callback<String> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final String onlineFlag = requestedStatus ? STATUS_ONLINE_FLAG : STATUS_OFFLINE_FLAG;
                    JSONObject body = new JSONObject();
                    body.put("user_id", item.userId);
                    body.put("department_id", item.departmentId);
                    body.put("department_name", item.departmentName);
                    body.put("user_id", item.userId);
                    body.put("online_status", onlineFlag);
                    final String url = createUrl(basepath, "/devices/" + item.deviceId + "/departments/" + item.departmentId);
                    final String tag = "PUT " + url;
                    ObjectRequest request = new ObjectRequest(PUT, url, apikey, body.toString(), new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            callback.onSuccess(onlineFlag);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onFailure(processFailure(activity, apikey, error, tag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    updateDepartmentStatus(activity, item, requestedStatus, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(request, tag);
                } catch (JSONException e) {
                    callback.onFailure(e);
                }
            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public static void getExtensions(final Activity activity, final String requestTag, @Nullable final String searchTerm, final int page, final int perPage, final Callback<List<InternalItem>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final JSONObject filters = new JSONObject() {{
                        put("computed_status", "A,E"); // default: show only Active and Enabled internals
                        if (!TextUtils.isEmpty(searchTerm)) {
                            put("q", searchTerm);
                        }
                    }};
                    final String url = createUrl(basepath, "/extensions", new HashMap<String, Object>(){{
                        put("_page", page);
                        put("_perPage", perPage);
                        put("_filters", filters);
                    }});
                    ArrayRequest request = new ArrayRequest(GET, url, apikey, null, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray array) {
                            int arrayLength = array.length();
                            List<InternalItem> list = new ArrayList<>(arrayLength);
                            for (int i = 0; i < arrayLength; i++) {
                                JSONObject jsonItem = array.optJSONObject(i);
                                InternalItem.Agent agent = null;
                                JSONObject jsonAgent = jsonItem.optJSONObject("agent");
                                if (jsonAgent != null) {
                                    agent = new InternalItem.Agent(
                                            jsonAgent.optString("id"),
                                            jsonAgent.optString("name"),
                                            jsonAgent.optString("avatar_url")
                                    );
                                }
                                InternalItem.Department department = null;
                                JSONObject jsonDepartment = jsonItem.optJSONObject("department");
                                if (jsonDepartment != null) {
                                    department = new InternalItem.Department(
                                            jsonDepartment.optString("department_id"),
                                            jsonDepartment.optString("name"),
                                            Tools.getStringArrayFromJson(jsonDepartment, "agent_ids")
                                    );
                                }
                                InternalItem internalItem = new InternalItem(
                                        jsonItem.optString("id"),
                                        jsonItem.optString("number"),
                                        jsonItem.optString("status"),
                                        agent,
                                        department
                                );
                                list.add(internalItem);
                            }
                            callback.onSuccess(list);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onFailure(processFailure(activity, apikey, error, requestTag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    getExtensions(activity, requestTag, searchTerm, page, perPage, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(request, requestTag);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public static void getContacts(final Activity activity, final String requestTag, @Nullable final String searchTerm, final int page, final int perPage, final String sortDirection, final String sortField, final Callback<List<ContactsItem>> callback) {
        prepare(activity, new AuthCallback() {
            @Override
            public void onAuthData(Client client, String basepath, final String apikey) {
                try {
                    final JSONObject filters = new JSONObject() {{
                        put("type", "V"); // default
                        put("hasPhone", "Y"); // default
                        if (!TextUtils.isEmpty(searchTerm)) {
                            put("q", searchTerm);
                        }
                    }};
                    final String url = createUrl(basepath, "/contacts", new HashMap<String, Object>(){{
                        put("_page", page);
                        put("_perPage", perPage);
                        put("_sortField", sortField);
                        put("_sortDir", sortDirection);
                        put("_filters", filters);
                    }});
                    ArrayRequest request = new ArrayRequest(GET, url, apikey, null, new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray array) {
                            int arrayLength = array.length();
                            List<ContactsItem> list = new ArrayList<>(arrayLength);
                            for (int i = 0; i < arrayLength; i++) {
                                JSONObject obj = array.optJSONObject(i);
                                ContactsItem contactsItem = new ContactsItem(
                                        obj.optString("id"),
                                        obj.optString("firstname"),
                                        obj.optString("lastname"),
                                        obj.optString("system_name"),
                                        obj.optString("avatar_url"),
                                        obj.optString("type")
                                );
                                contactsItem.setEmails(Tools.getStringArrayFromJson(obj, "emails"));
                                contactsItem.setPhones(Tools.getStringArrayFromJson(obj, "phones"));
                                list.add(contactsItem);
                            }
                            callback.onSuccess(list);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            callback.onFailure(processFailure(activity, apikey, error, requestTag, new AuthFallback() {
                                @Override
                                public void onFallback() {
                                    getContacts(activity, requestTag, searchTerm, page, perPage, sortDirection, sortField, callback);
                                }
                            }));
                        }
                    });
                    client.addToQueue(request, requestTag);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onException(Exception e) {
                callback.onFailure(e);
            }
        });
    }

}
