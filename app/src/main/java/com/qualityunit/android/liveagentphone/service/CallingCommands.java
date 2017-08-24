package com.qualityunit.android.liveagentphone.service;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.qualityunit.android.liveagentphone.R;

/**
 * Created by rasto on 7.11.16.
 */

public final class CallingCommands {

    public static void makeCall(Context context, String calleeNumber, String callingPrefix, String callingAsNumber, String callingAsName) throws CallingException {
        if (CallingService.isRunning) throw new CallingException(context.getString(R.string.only_one_call));
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        if (TextUtils.isEmpty(calleeNumber)) throw new CallingException("Argument 'calleeNumber' is empty or null");
        if (callingPrefix == null) callingPrefix = "";
        if (TextUtils.isEmpty(callingAsNumber)) throw new CallingException("Argument 'callingAsNumber' is empty or null");
        if (callingAsName == null) callingAsName = "";
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.MAKE_CALL)
                .putExtra(CallingService.KEY_CALL_DIRECTION, CallingService.CALL_DIRECTION.OUTGOING)
                .putExtra("calleeNumber", calleeNumber)
                .putExtra("callingPrefix", callingPrefix)
                .putExtra("callingAsNumber", callingAsNumber)
                .putExtra("callingAsName", callingAsName));
    }

    public static void incomingCall(Context context) throws CallingException {
        if (CallingService.isRunning) {
            // do nothing - simply ignore push notification
            return;
        }
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.INCOMING_CALL)
                .putExtra(CallingService.KEY_CALL_DIRECTION, CallingService.CALL_DIRECTION.INCOMING));
    }

    public static void receiveCall (Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.RECEIVE_CALL));
    }

    public static void sendDtmf(Context context, String character) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        if (TextUtils.isEmpty(character) || character.length() != 1) throw new CallingException("Invalid DTMF argument: '" + character + "'");
        if (!CallingService.isRunning) throw new CallingException("Cannot send DTMF signal when calling service is not running");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SEND_DTMF)
                .putExtra("character", character));
    }

    public static void hangupCall (Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.HANGUP_CALL));
    }

    public static void toggleMute(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_MUTE));
    }

    public static void toggleSpeaker(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_SPEAKER));
    }

    public static void adjustIncallVolume(Context context, boolean increase) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.ADJUST_INCALL_VOLUME)
                .putExtra("increase", increase));
    }

    public static void updateState(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_STATE));
    }

    public static void silenceRinging(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.SILENCE_RINGING));
    }

    public static void updateAll(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.UPDATE_ALL));
    }

    public static void toggleHold(Context context) throws CallingException {
        if (context == null) throw new CallingException("Argument 'context' cannot be null");
        context.startService(new Intent(context, CallingService.class)
                .putExtra(CallingService.KEY_COMMAND, CallingService.COMMANDS.TOGGLE_HOLD));
    }
}
