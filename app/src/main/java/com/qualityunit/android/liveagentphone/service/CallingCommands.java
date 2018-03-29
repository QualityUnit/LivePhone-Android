package com.qualityunit.android.liveagentphone.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 7.11.16.
 */

public final class CallingCommands {

    public static void makeCall(@NonNull Context context, String remoteNumber, String prefix, String remoteName) throws CallingException {
        if (CallingService.isRunning) throw new CallingException(context.getString(R.string.only_one_call));
        if (TextUtils.isEmpty(remoteNumber)) throw new CallingException("Argument 'calleeNumber' is empty or null");
        if (TextUtils.isEmpty(prefix)) prefix = "";
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.MAKE_CALL)
                .putExtra("prefix", prefix)
                .putExtra("remoteNumber", remoteNumber)
                .putExtra("remoteName", remoteName));
    }

    public static void incomingCall(@NonNull  Context context) throws CallingException {
        if (CallingService.isRunning) {
            // do nothing - simply ignore push notification
            return;
        }
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.INCOMING_CALL));
    }

    public static void receiveCall (@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.RECEIVE_CALL));
    }

    public static void sendDtmf(@NonNull Context context, String character) throws CallingException {
        if (TextUtils.isEmpty(character) || character.length() != 1) throw new CallingException("Invalid DTMF argument: '" + character + "'");
        if (!CallingService.isRunning) throw new CallingException("Cannot send DTMF signal when calling service is not running");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SEND_DTMF)
                .putExtra("character", character));
    }

    public static void hangupCall (@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.HANGUP_CALL));
    }

    public static void toggleMute(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_MUTE));
    }

    public static void toggleSpeaker(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_SPEAKER));
    }

    public static void adjustIncallVolume(@NonNull Context context, boolean increase) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.ADJUST_INCALL_VOLUME)
                .putExtra("increase", increase));
    }

    public static void updateState(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_STATE));
    }

    public static void silenceRinging(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SILENCE_RINGING));
    }

    public static void updateAll(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_ALL));
    }

    public static void toggleHold(@NonNull Context context) throws CallingException {
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_HOLD));
    }
}
