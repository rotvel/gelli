package com.dkanada.gramophone.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.dkanada.gramophone.service.playback.Playback;
import com.google.android.exoplayer2.SimpleExoPlayer;

import java.util.concurrent.CountDownLatch;

public class MultiPlayerThreadWrapper implements Playback {

    private MultiPlayer multiPlayer;
    private SimpleExoPlayer exoPlayer;
    private Handler exoThreadHandler;

    public MultiPlayerThreadWrapper(Context context) {
        multiPlayer = new MultiPlayer(context);

        exoPlayer = multiPlayer.getExoPlayer();
        exoThreadHandler = new Handler(exoPlayer.getApplicationLooper());
    }

    @Override
    public void setDataSource(String path) {
        runOnExoThread(() -> multiPlayer.setDataSource(path));
    }

    @Override
    public void queueDataSource(String path) {
        runOnExoThread(() -> multiPlayer.queueDataSource(path));
    }

    @Override
    public void setCallbacks(PlaybackCallbacks callbacks) {
        multiPlayer.setCallbacks(callbacks);
    }

    @Override
    public void start() {
        runOnExoThread(() -> multiPlayer.start());
    }

    @Override
    public void pause() {
        runOnExoThread(() -> multiPlayer.pause());
    }

    @Override
    public void stop() {
        multiPlayer.stop();
    }

    @Override
    public boolean isInitialized() {
        return multiPlayer.isInitialized();
    }

    @Override
    public boolean isPlaying() {
        return multiPlayer.isPlaying();
    }

    @Override
    public int position() {
        if (isOnExoThread()) return (int) multiPlayer.position();

        final CountDownLatch latch = new CountDownLatch(1);
        final int[] value = new int[1];

        exoThreadHandler.post(() -> {
            value[0] = (int) multiPlayer.position();
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return value[0];
    }

    @Override
    public int duration() {
        return multiPlayer.duration();
    }

    @Override
    public int seek(int position) {
        runOnExoThread(() -> multiPlayer.seek(position));
        return position;
    }

    @Override
    public void setVolume(float volume) {
        runOnExoThread(() -> multiPlayer.setVolume(volume));
    }

    private boolean isOnExoThread() {
        // Same check as in SimpleExoPlayer.verifyApplicationThread()
        return Looper.myLooper() == exoPlayer.getApplicationLooper();
    }

    private void runOnExoThread(Runnable runnable) {
        if (!isOnExoThread()) {
            exoThreadHandler.post(runnable);
        } else {
            runnable.run();
        }
    }
}
