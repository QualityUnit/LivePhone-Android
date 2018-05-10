package com.qualityunit.android.liveagentphone.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.qualityunit.android.liveagentphone.App;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public abstract class Tools {

	/**
	 * Get App version name
	 * 
	 * @return
	 */
	public static String getVersionName() {
		try {
			return App.getAppContext().getPackageManager().getPackageInfo(App.getAppContext().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
            String msg = "Error while retrieving data about versionName from manifest file.";
            Log.e(Tools.class.getSimpleName(), msg);
//            Crashlytics.log(Log.ERROR, Tools.class.getSimpleName(), msg);
		}
		return null;
	}

    /**
     * Get device unique ID
     *
     * @return
     */
    public static String getDeviceUniqueId() {
        return Settings.Secure.getString(App.getAppContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

	/**
	 * DELETE LATER
	 * 
	 * @param txtData
	 * @param filename
	 */
	public static void writeToSdcard(String txtData, String filename) {
		try {
			File myFile = new File("/sdcard/" + filename + ".txt");
			myFile.createNewFile();
			FileOutputStream fOut = new FileOutputStream(myFile);
			OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
			myOutWriter.append(txtData);
			myOutWriter.close();
			fOut.close();
			Toast.makeText(App.getAppContext(),
                    "Done writing SD '" + filename + ".txt'",
                    Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(App.getAppContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static int getVersionCode() {
		Context context = App.getAppContext();
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

    public static boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) App.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null;
    }

    public static void showKeyboard(final Context context, final View view) {
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    view.requestFocus();
                    InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }

    public static void hideKeyboard(final Context context, final View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static String formatError (String message) {
        return formatError(null, message);
    }

    public static String formatError (Integer code, String message) {
        String resultMsg = "Error";
        if (code != null) {
            resultMsg += " '" + code + "'";
        }
        if (!TextUtils.isEmpty(message)) {
            resultMsg += ": " + message;
        }
        else {
            resultMsg += ": (no message)";
        }
        return resultMsg;
    }

    /**
     * Exception free parsing<br/>
     * Get string value from json or get empty string value.
     * @param jsonItem
     * @param key
     * @return
     */
    public static String getStringFromJson(JSONObject jsonItem, String key) {
        String value;
        try {
            value = jsonItem.getString(key);
        } catch (JSONException e) {
            return "";
        }
        return value;
    }

    /**
     * Exception free parsing<br/>
     * Get int value from json or get -1 value.
     * @param jsonItem
     * @param key
     * @return -1 when key does not exist
     */
    public static int getIntFromJson(JSONObject jsonItem, String key) {
        int value = -1;
        try {
            value = jsonItem.getInt(key);
        } catch (JSONException e) { }
        return value;
    }

    /**
     * @param jsonItem - object where field with key has value as an array
     * @param key - key of field
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
        for(int i = 0; i < count - number.length(); i++) {
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

        public static String md5Hex (String message) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] array = md.digest(message.getBytes("CP1252"));
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < array.length; ++i) {
                    sb.append(Integer.toHexString((array[i]
                            & 0xFF) | 0x100).substring(1,3));
                }
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
            } catch (UnsupportedEncodingException e) {
            }
            return null;
        }

    }

    public static class Sip {

        public static String createRegisterUri(String sipHost) {
            return "sip:" + sipHost;
        }

        public static String createAccountUri(String sipUser, String sipHost) {
            return "sip:" + sipUser + "@" + sipHost;
        }

        public static String createCalleeUri(String callingPrefix, String calleeNumber, String sipHost) {
            return "sip:" + callingPrefix + calleeNumber + "@" + sipHost;
        }

        public static String cleanNumber(String number) {
            return number
                .replaceAll(" ", "")
                .replaceAll("\\+", "00")
                .replaceAll("\\/", "");
        }
    }
}
