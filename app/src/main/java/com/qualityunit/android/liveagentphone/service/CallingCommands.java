package com.qualityunit.android.liveagentphone.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by rasto on 7.11.16.
 */

public final class CallingCommands {

    public static void makeCall(@NonNull Context context, String remoteNumber, String prefix, String remoteName) {
        if (TextUtils.isEmpty(prefix)) prefix = "";
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.MAKE_CALL)
                .putExtra("prefix", prefix)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName));
    }

    public static void incomingCall(@NonNull  Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.INCOMING_CALL));
    }

    public static void receiveCall (@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.RECEIVE_CALL));
    }

    public static void sendDtmf(@NonNull Context context, String character) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SEND_DTMF)
                .putExtra("character", character));
    }

    public static void hangupCall (@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.HANGUP_CALL));
    }

    public static void toggleMute(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_MUTE));
    }

    public static void toggleSpeaker(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_SPEAKER));
    }

    public static void adjustIncallVolume(@NonNull Context context, boolean increase) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.ADJUST_INCALL_VOLUME)
                .putExtra("increase", increase));
    }

    public static void updateState(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_STATE));
    }

    public static void silenceRinging(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SILENCE_RINGING));
    }

    public static void updateAll(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_ALL));
    }

    public static void toggleHold(@NonNull Context context) {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_HOLD));
    }
}
