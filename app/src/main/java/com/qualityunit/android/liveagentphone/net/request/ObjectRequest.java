package com.qualityunit.android.liveagentphone.net.request;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class ObjectRequest extends BaseRequest<JSONObject> {


    public ObjectRequest(int method, String url, @Nullable String apikey, @Nullable String requestBody, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener) {
        super(method, url, apikey, requestBody, listener, errorListener, false);
    }

    /*
     Ping request
     */
    public ObjectRequest(int method, String url, @Nullable String apikey, @Nullable String requestBody, Response.Listener<JSONObject> listener, @Nullable Response.ErrorListener errorListener, boolean noRetry) {
        super(method, url, apikey, requestBody, listener, errorListener, noRetry);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
