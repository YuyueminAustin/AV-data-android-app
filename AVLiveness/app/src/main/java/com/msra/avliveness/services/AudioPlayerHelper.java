package com.msra.avliveness.services;


import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.File;

import android.util.Log;
public class AudioPlayerHelper {
    private static final String TAG = "AudioPlayer";
    private Context context;
    private MediaPlayer mediaPlayer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRestart;
    private boolean loopEnabled;
    private long idleBetweenLoopsMs;

    public AudioPlayerHelper(Context context) {
        this.context = context;
    }

    public void startAudioPlayback(String audioFilePath, boolean loop, double idleBetweenLoopsSeconds) {
        stopAudioPlayback();
        loopEnabled = loop;
        idleBetweenLoopsMs = Math.max(0L, (long) (idleBetweenLoopsSeconds * 1000.0));
        mediaPlayer = new MediaPlayer();
        try {
            File file = new File(audioFilePath);
            if (file.exists()){
                Log.i(TAG, "File Exists");
            } else {
                Log.e(TAG, "File not exists.");
                return;
            }
            mediaPlayer.setDataSource(audioFilePath);
            if (loopEnabled) {
                if (idleBetweenLoopsMs <= 0) {
                    mediaPlayer.setLooping(true);
                } else {
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setOnCompletionListener(mp -> scheduleLoopRestart());
                }
            } else {
                mediaPlayer.setLooping(false);
            }
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopAudioPlayback() {
        cancelPendingRestart();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void scheduleLoopRestart() {
        cancelPendingRestart();
        if (!loopEnabled || mediaPlayer == null) {
            return;
        }
        pendingRestart = () -> {
            if (mediaPlayer == null) {
                return;
            }
            try {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to restart audio playback", e);
            }
        };
        mainHandler.postDelayed(pendingRestart, idleBetweenLoopsMs);
    }

    private void cancelPendingRestart() {
        if (pendingRestart != null) {
            mainHandler.removeCallbacks(pendingRestart);
            pendingRestart = null;
        }
    }

}
