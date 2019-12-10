package com.qualityunit.android.liveagentphone.service;

import android.os.Bundle;

public interface VoiceConnectionCallbacks {
    void onCallState(final String callState, final String remoteNumber, final String remoteName);
    void onCallUpdate(final Bundle keyValue);
    void onConnectionDestroyed();
    void onErrorState(final String value, Exception e);
}
