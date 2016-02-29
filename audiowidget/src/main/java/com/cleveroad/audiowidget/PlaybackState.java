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

	public void start(Object initiator) {
		state(STATE_PLAYING, initiator);
	}

	public void pause(Object initiator) {
		state(STATE_PAUSED, initiator);
	}

	public void stop(Object initiator) {
		state(STATE_STOPPED, initiator);
		position(0);
	}

	private void state(int state, Object initiator) {
		if (this.state == state)
			return;
		int oldState = this.state;
		this.state = state;
		for (PlaybackStateListener listener : stateListeners) {
			listener.onStateChanged(oldState, state, initiator);
		}
	}

	private void notifyProgressChanged(int position) {
		float progress = 1f * position / duration;
		for (PlaybackStateListener listener : stateListeners) {
			listener.onProgressChanged(position, duration, progress);
		}
	}

	public interface PlaybackStateListener {

		void onStateChanged(int oldState, int newState, Object initiator);

		void onProgressChanged(int position, int duration, float percentage);
	}
}
