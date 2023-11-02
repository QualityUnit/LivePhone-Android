package com.qualityunit.android.liveagentphone.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.qualityunit.android.liveagentphone.App;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Tools {

    /**
     * Get App version name
     *
     * @return
     */
    public static String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            String msg = "Error while retrieving data about versionName from manifest file.";
            Logger.e(Tools.class.getSimpleName(), msg);
        }
        return null;
    }

    /**
     * Get device unique ID
     *
     * @return
     */
    public static String getDeviceUniqueId() {
        String memoryKey = "com.qualityunit.android.liveagentphone.installid";
        String installId = App.getSharedPreferences().getString(memoryKey, null);
        if (TextUtils.isEmpty(installId)) {
            installId = UUID.randomUUID().toString();
            App.getSharedPreferences().edit().putString(memoryKey, installId).apply();
        }
        return installId;
    }

    /**
     * @return device name e.g. Google Pixel  -
     */
    public static String getDeviceName() {
        String name;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            name = capitalizeFirstLetter(model);
        } else {
            name = capitalizeFirstLetter(manufacturer) + " " + model;
        }
        if (TextUtils.isEmpty(name)) {
            name = "Unknown Android device";
        }
        return name;
    }

    private static String capitalizeFirstLetter(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public static int getVersionCode(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    public static void showKeyboard(final Context context, final View view, int delayMillis) {
        if (view != null) {
            view.postDelayed(() -> {
                view.requestFocus();
                InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }, delayMillis);
        }
    }

    public static void hideKeyboard(final Context context, final View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static String formatError(String message) {
        return formatError(null, message);
    }

    public static String formatError(Integer code, String message) {
        String resultMsg = "Error";
        if (code != null) {
            resultMsg += " '" + code + "'";
        }
        if (!TextUtils.isEmpty(message)) {
            resultMsg += ": " + message;
        } else {
            resultMsg += ": (no message)";
        }
        return resultMsg;
    }

    /**
     * @param jsonItem - object where field with key has value as an array
     * @param key      - key of field
     * @return
     */
    public static List<String> getStringArrayFromJson(JSONObject jsonItem, String key) {
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = jsonItem.getJSONArray(key);
            for (int i = 0; i < array.length(); i++) {
                String arrayItem = array.getString(i);
                list.add(arrayItem);
            }
        } catch (JSONException e) {
        }
        return list;
    }

    public static boolean isUrlValid(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    public static String fixDecimalsBefore(int num, int count) {
        if (num < 0) {
            return "";
        }
        String number = String.valueOf(num);
        String newNumber = "";
        for (int i = 0; i < count - number.length(); i++) {
            newNumber += "0";
        }
        newNumber += number;
        return newNumber;
    }

    public static String millisToHhMmSs(long delta) {
        delta /= 1000;
        int hours = (int) (delta / 3600); // hours from delta
        delta -= hours * 3600; // substract hours from delta
        int minutes = (int) (delta / 60); // minutes from delta
        int seconds = (int) (delta - (minutes * 60)); // substract minutes from delta
        StringBuilder stringBuilder = new StringBuilder();
        if (hours > 0) {
            stringBuilder
                    .append(Tools.fixDecimalsBefore(hours, 2))
                    .append(":");
        }
        stringBuilder
                .append(Tools.fixDecimalsBefore(minutes, 2))
                .append(":")
                .append(Tools.fixDecimalsBefore(seconds, 2));
        return stringBuilder.toString();
    }

    public static String createContactName(String firstName, String lastName, String systemName) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(firstName)) {
            sb.append(firstName.trim());
            sb.append(" ");
        }
        if (!TextUtils.isEmpty(lastName)) {
            sb.append(lastName.trim());
        }
        if (sb.length() == 0) {
            sb.append(systemName.trim());
        }
        return sb.toString().trim();
    }

    public static class MD5Util {

        public static String md5Hex(String message) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] array = md.digest(message.getBytes("CP1252"));
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < array.length; ++i) {
                    sb.append(Integer.toHexString((array[i]
                            & 0xFF) | 0x100).substring(1, 3));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
            } catch (UnsupportedEncodingException e) {
            }
            return null;
        }

    }

}