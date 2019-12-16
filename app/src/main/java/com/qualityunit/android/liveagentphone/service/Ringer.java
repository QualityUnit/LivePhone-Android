package com.qualityunit.android.liveagentphone.service;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

public class Ringer implements MediaPlayer.OnErrorListener {

    private static final String TAG = Ringer.class.getSimpleName();
    private static final long[] VIBRATOR_PATTERN = {0, 1000, 1000};
    private final Context context;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;

    public Ringer(Context context) {
        this.context  = context;
    }

    public void start() {
        if (mediaPlayer != null) return;
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int ringerMode = audioManager.getRingerMode();
        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATOR_PATTERN, 1);
            Log.d(TAG, "Starting vibration...");
        }
        mediaPlayer = createMediaPlayer();
        if (mediaPlayer != null && ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "Playing ringtone...");
            } catch (IllegalStateException | IOException e) {
                Log.e(TAG, e.getMessage(), e);
                mediaPlayer = null;
            }
        } else {
            Log.d(TAG, "Not ringing, mode: " + ringerMode);
        }
    }

    public void stop() {
        if (vibrator != null) {
            Log.d(TAG, "Cancelling vibrator...");
            vibrator.cancel();
            vibrator = null;
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error while stopping ringtone", e);
            } finally {
                mediaPlayer = null;
            }
        }
    }

    private MediaPlayer createMediaPlayer() {
        try {
            Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setDataSource(context, uri);
            mediaPlayer.setLooping(true);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            return mediaPlayer;
        } catch (IOException e) {
            Log.e(TAG, "Failed to create mediaPlayer for incoming call ringer", e);
            return null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Error while ringing: " + mp + ", " + what + ", " + extra);
        stop();
        return false;
    }
}