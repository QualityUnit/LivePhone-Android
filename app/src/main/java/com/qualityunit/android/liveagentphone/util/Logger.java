package com.qualityunit.android.liveagentphone.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

import androidx.core.content.FileProvider;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

/**
 * Created by rasto on 11.02.16.
 */
public class Logger {

    private static final String TAG = Logger.class.getSimpleName();
    public static String logFileName = "logfile.log";

    public static void logToFile(Context context, String text) {
        File logFile = new File(context.getExternalFilesDir(null), logFileName);
        try {
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    text = Tools.getDeviceName() + System.lineSeparator() + System.lineSeparator() + new Date().toString() + ": " + text; // initial line
                }
            }
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.append(new Date().toString() + ": " + text + System.lineSeparator());
            osw.flush();
            osw.close();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Error while 'logToFile': " + logFileName, e);
        }
    }

    public static void sendLogFile(Context context) {
        File logFile = new File(context.getExternalFilesDir(null), logFileName);
        Uri path = FileProvider.getUriForFile(context, "com.qualityunit.android.liveagentphone.provider", logFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent .setType("vnd.android.cursor.dir/email");
        String to[] = {"support@qualityunit.com"};
        intent.putExtra(Intent.EXTRA_EMAIL, to);
        intent.putExtra(Intent.EXTRA_STREAM, path);
        intent.putExtra(Intent.EXTRA_SUBJECT, "LivePhone log file");
        PackageManager pm = context.getPackageManager();
        if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent);
        }
    }

}
