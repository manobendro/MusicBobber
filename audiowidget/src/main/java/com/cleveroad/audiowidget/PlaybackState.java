package com.cleveroad.audiowidget;

import android.support.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Александр on 26.02.2016.
 */
public class PlaybackState {

	public static final int STATE_STOPPED = 0;
	public static final int STATE_PLAYING = 1;
	public static final int STATE_PAUSED = 2;

	private int state;

	private int position;
	private int duration;

	private final Set<PlaybackStateListener> stateListeners;

	public PlaybackState() {
		stateListeners = new HashSet<>();
	}

	public boolean addPlaybackStateListener(@NonNull PlaybackStateListener playbackStateListener) {
		return stateListeners.add(playbackStateListener);
	}

	public boolean removePlaybackStateListener(@NonNull PlaybackStateListener playbackStateListener) {
		return stateListeners.remove(playbackStateListener);
	}

	public int state() {
		return state;
	}

	public int position() {
		return position;
	}

	public int duration() {
		return duration;
	}

	public PlaybackState position(int position) {
		this.position = position;
		notifyProgressChanged(position);
		return this;
	}

	public PlaybackState duration(int duration) {
		this.duration = duration;
		return this;
	}

	public void start() {
		state(STATE_PLAYING);
	}

	public void pause() {
		state(STATE_PAUSED);
	}

	public void stop() {
		state(STATE_STOPPED);
		position(0);
	}

	private void state(int state) {
		if (this.state == state)
			return;
		int oldState = this.state;
		this.state = state;
		for (PlaybackStateListener listener : stateListeners) {
			listener.onStateChanged(oldState, state);
		}
	}

	private void notifyProgressChanged(int position) {
		float progress = 1f * position / duration;
		for (PlaybackStateListener listener : stateListeners) {
			listener.onProgressChanged(position, duration, progress);
		}
	}

	public interface PlaybackStateListener {

		void onStateChanged(int oldState, int newState);

		void onProgressChanged(int position, int duration, float percentage);
	}
}
