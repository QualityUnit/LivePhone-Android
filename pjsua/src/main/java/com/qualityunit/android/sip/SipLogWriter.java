package com.qualityunit.android.sip;

import android.util.Log;

import org.pjsip.pjsua2.LogEntry;
import org.pjsip.pjsua2.LogWriter;

public class SipLogWriter extends LogWriter {

    private final String TAG = "PJSUA";

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
