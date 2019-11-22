package com.qualityunit.android.voice;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

public class VoiceLogWriter extends LogWriter {

    private final String TAG = "VOICE";

    @Override
    public void write(LogEntry entry) {
        if (entry == null) return;
        switch (entry.getLevel()) {
            case Log.ERROR:
                Log.e(TAG, entry.getMsg());
                break;
            default:
                Log.d(TAG, entry.getMsg());
        }
    }
}
