package com.qualityunit.android.liveagentphone.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import static com.qualityunit.android.liveagentphone.service.CallingService.VoiceConnection.EXTRA_PREFIX;
import static com.qualityunit.android.liveagentphone.service.CallingService.VoiceConnection.EXTRA_REMOTE_NAME;
import static com.qualityunit.android.liveagentphone.service.CallingService.VoiceConnection.EXTRA_REMOTE_NUMBER;

/**
 * Created by rasto on 7.11.16.
 */

public final class CallingCommands {

    public static void makeCall(@NonNull Context context, String remoteNumber, String prefix, String remoteName) {
        if (TextUtils.isEmpty(prefix)) prefix = "";
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.MAKE_CALL)
                .putExtra(EXTRA_PREFIX, prefix)
                .putExtra(EXTRA_REMOTE_NUMBER, remoteNumber)
                .putExtra(EXTRA_REMOTE_NAME, remoteName));
    }

    public static void incomingCall(@NonNull  Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.INCOMING_CALL));
    }

    public static void answerCall(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.ANSWER_CALL));
    }

    public static void sendDtmf(@NonNull Context context, String character) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.SEND_DTMF)
                .putExtra("character", character));
    }

    public static void hangupCall (@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.HANGUP_CALL));
    }

    public static void toggleMute(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.TOGGLE_MUTE));
    }

    public static void toggleSpeaker(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.TOGGLE_SPEAKER));
    }

    public static void adjustIncallVolume(@NonNull Context context, boolean increase) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.ADJUST_INCALL_VOLUME)
                .putExtra("increase", increase));
    }

    public static void getCallState(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.GET_CALL_STATE));
    }

    public static void silenceRinging(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.SILENCE_RINGING));
    }

    public static void getCallUpdates(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.GET_CALL_UPDATES));
    }

    public static void toggleHold(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra("command", CallingService.COMMANDS.TOGGLE_HOLD));
    }
}
