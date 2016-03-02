package com.cleveroad.audiowidget.example;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.cleveroad.audiowidget.AudioWidget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.glide.transformations.CropCircleTransformation;

/**
 * Created by Александр on 02.03.2016.
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioWidget.OnControlsClickListener, AudioWidget.OnWidgetStateChangedListener {

    private static final String EXTRA_FILE_URIS = "EXTRA_FILE_URIS";
    private static final String EXTRA_SELECT_TRACK = "EXTRA_SELECT_TRACK";
    private static final long UPDATE_INTERVAL = 1000;

    private AudioWidget audioWidget;
    private MediaPlayer mediaPlayer;
    private boolean preparing;
    private int playingIndex = -1;
    private final List<MusicItem> items = new ArrayList<>();
    private boolean paused;
    private Timer timer;
    private CropCircleTransformation cropCircleTransformation;


    public static void setTracks(@NonNull Context context, @NonNull MusicItem[] tracks) {
        Intent intent = new Intent(context, MusicService.class);
        intent.putExtra(EXTRA_FILE_URIS, tracks);
        context.startService(intent);
    }

    public static void playTrack(@NonNull Context context, @NonNull MusicItem item) {
        Intent intent = new Intent(context, MusicService.class);
        intent.putExtra(EXTRA_SELECT_TRACK, item);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        audioWidget = new AudioWidget(this);
        audioWidget.controller().onControlsClickListener(this);
        audioWidget.controller().onWidgetStateChangedListener(this);
        cropCircleTransformation = new CropCircleTransformation(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(EXTRA_FILE_URIS)) {
                addNewTracks(intent);
            } else if (intent.hasExtra(EXTRA_SELECT_TRACK)) {
                selectNewTrack(intent);
            }
            return START_STICKY;
        }
        return super.onStartCommand(null, flags, startId);
    }

    private void selectNewTrack(Intent intent) {
        if (preparing) {
            return;
        }
        MusicItem item = intent.getParcelableExtra(EXTRA_SELECT_TRACK);
        if (item == null && playingIndex == -1 || playingIndex != -1 && items.get(playingIndex).equals(item)) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                audioWidget.controller().pause();
            } else {
                mediaPlayer.start();
                audioWidget.controller().start();
            }
            return;
        }
        playingIndex = items.indexOf(item);
        startCurrentTrack();
    }

    private void startCurrentTrack() {
        if (mediaPlayer.isPlaying() || paused) {
            mediaPlayer.stop();
            paused = false;
        }
        mediaPlayer.reset();
        try {
            mediaPlayer.setDataSource(this, items.get(playingIndex).fileUri());
            mediaPlayer.prepareAsync();
            preparing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addNewTracks(Intent intent) {
        MusicItem playingItem = null;
        if (playingIndex != -1)
            playingItem = items.get(playingIndex);
        items.clear();
        Parcelable[] items = intent.getParcelableArrayExtra(EXTRA_FILE_URIS);
        for (Parcelable item : items) {
            if (item instanceof MusicItem)
                this.items.add((MusicItem) item);
        }
        if (playingItem == null) {
            playingIndex = -1;
        } else {
            playingIndex = this.items.indexOf(playingItem);
        }
        if (playingIndex == -1 && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    @Override
    public void onDestroy() {
        audioWidget.hide();
        audioWidget = null;
        mediaPlayer.stop();
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
        stopTrackingPosition();
        cropCircleTransformation = null;
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        preparing = false;
        mediaPlayer.start();
        if (!audioWidget.isShown()) {
            audioWidget.show(0, 0);
        }
        audioWidget.controller().start();
        audioWidget.controller().duration((int) items.get(playingIndex).duration());
        stopTrackingPosition();
        startTrackingPosition();
        Glide.with(this)
                .load(items.get(playingIndex).albumArtUri())
                .asBitmap()
                .transform(cropCircleTransformation)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        if (audioWidget != null) {
                            audioWidget.controller().albumCover(resource);
                        }
                    }
                });
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (playingIndex == -1) {
            audioWidget.controller().stop();
            return;
        }
        playingIndex++;
        if (playingIndex >= items.size()) {
            playingIndex = 0;
            if (items.size() == 0) {
                audioWidget.controller().stop();
                return;
            }
        }
        startCurrentTrack();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        preparing = true;
        return false;
    }

    @Override
    public void onPlaylistClicked() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onPreviousClicked() {
        if (items.size() == 0)
            return;
        playingIndex--;
        if (playingIndex < 0) {
            playingIndex = items.size() - 1;
        }
        startCurrentTrack();
    }

    @Override
    public boolean onPlayPauseClicked() {
        if (mediaPlayer.isPlaying()) {
            stopTrackingPosition();
            mediaPlayer.pause();
            audioWidget.controller().start();
            paused = true;
        } else {
            startTrackingPosition();
            audioWidget.controller().pause();
            mediaPlayer.start();
            paused = false;
        }
        return true;
    }

    @Override
    public void onNextClicked() {
        if (items.size() == 0)
            return;
        playingIndex++;
        if (playingIndex >= items.size()) {
            playingIndex = 0;
        }
        startCurrentTrack();
    }

    @Override
    public void onAlbumClicked() {

    }

    private void startTrackingPosition() {
        timer = new Timer("MusicService Timer");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                AudioWidget widget = audioWidget;
                MediaPlayer player = mediaPlayer;
                if (widget != null) {
                    widget.controller().position(player.getCurrentPosition());
                }
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }

    private void stopTrackingPosition() {
        if (timer == null)
            return;
        timer.cancel();
        timer.purge();
        timer = null;
    }

    @Override
    public void onWidgetStateChanged(@NonNull AudioWidget.State state) {
        if (state == AudioWidget.State.REMOVED) {
            stopSelf();
        }
    }
}
