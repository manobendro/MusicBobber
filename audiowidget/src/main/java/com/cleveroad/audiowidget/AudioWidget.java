package com.cleveroad.audiowidget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Random;

/**
 * Audio widget implementation.
 */
public class AudioWidget {

	/**
	 * Play/pause button view.
	 */
	private final PlayPauseButton playPauseButton;

	/**
	 * Expanded widget style view.
	 */
	private final ExpandCollapseWidget expandCollapseWidget;

	/**
	 * Remove widget view.
	 */
	private final RemoveWidgetView removeWidgetView;

	/**
	 * Playback state.
	 */
	private final PlaybackState playbackState;

	/**
	 * Widget controller.
	 */
	private final Controller controller;

	private final WindowManager windowManager;
	private final Handler handler;
	private final Point screenSize;
	private final Context context;

	/**
	 * Bounds of remove widget view. Used for checking if play/pause button is inside this bounds
	 * and ready for removing from screen.
	 */
	private final RectF removeBounds;

	/**
	 * Remove widget view Y position (hidden).
	 */
	private float hiddenRemWidY;

	/**
	 * Remove widget view Y position (visible).
	 */
	private float visibleRemWidY;
	private float width, height, radius;
	private final OnControlsClickListenerWrapper onControlsClickListener;
	private boolean shown;
	private boolean released;
	private boolean removeWidgetShown;
	private OnWidgetStateChangedListener onWidgetStateChangedListener;
	private OnPlaybackStateChangedListener onPlaybackStateChangedListener;

	@SuppressWarnings("deprecation")
	public AudioWidget(@NonNull Context context) {
		this.context = context.getApplicationContext();
		this.handler = new Handler();
		this.screenSize = new Point();
		this.removeBounds = new RectF();
		this.controller = newController();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			windowManager.getDefaultDisplay().getSize(screenSize);
		} else {
			screenSize.x = windowManager.getDefaultDisplay().getWidth();
			screenSize.y = windowManager.getDefaultDisplay().getHeight();
		}
		screenSize.y -= context.getResources().getDimensionPixelSize(R.dimen.aw_status_bar_height);
		this.playbackState = new PlaybackState();
		height = context.getResources().getDimensionPixelSize(R.dimen.aw_player_height);
		width = context.getResources().getDimensionPixelSize(R.dimen.aw_player_width);
		radius = height / 2f;

		int darkColor = VersionUtil.color(context, R.color.aw_dark);
		int lightColor = VersionUtil.color(context, R.color.aw_light);
		int progressColor = VersionUtil.color(context, R.color.aw_progress);
		int expandColor = VersionUtil.color(context, R.color.aw_expanded);

		Drawable playDrawable = VersionUtil.drawable(context, R.drawable.ic_play);
		Drawable pauseDrawable = VersionUtil.drawable(context, R.drawable.ic_pause);
		Drawable prevDrawable = VersionUtil.drawable(context, R.drawable.ic_prev);
		Drawable nextDrawable = VersionUtil.drawable(context, R.drawable.ic_next);
		Drawable playlistDrawable = VersionUtil.drawable(context, R.drawable.ic_playlist);
		Drawable albumDrawable = VersionUtil.drawable(context, R.drawable.ic_default_album);

		Configuration configuration = new Configuration.Builder()
				.context(context)
				.playbackState(playbackState)
				.random(new Random())
				.darkColor(darkColor)
				.playColor(lightColor)
				.progressColor(progressColor)
				.expandedColor(expandColor)
				.widgetWidth(width)
				.radius(radius)
				.playlistDrawable(playlistDrawable)
				.playDrawable(playDrawable)
				.prevDrawable(prevDrawable)
				.nextDrawable(nextDrawable)
				.pauseDrawable(pauseDrawable)
				.albumDrawable(albumDrawable)
                .buttonPadding(context.getResources().getDimensionPixelSize(R.dimen.aw_button_padding))
                .crossStrokeWidth(context.getResources().getDimension(R.dimen.aw_cross_stroke_width))
                .progressStrokeWidth(context.getResources().getDimension(R.dimen.aw_progress_stroke_width))
                .shadowRadius(context.getResources().getDimension(R.dimen.aw_shadow_radius))
                .shadowDx(context.getResources().getDimension(R.dimen.aw_shadow_dx))
                .shadowDy(context.getResources().getDimension(R.dimen.aw_shadow_dy))
                .shadowColor(VersionUtil.color(context, R.color.aw_shadow))
                .bubblesMinSize(context.getResources().getDimension(R.dimen.aw_bubbles_min_size))
                .bubblesMaxSize(context.getResources().getDimension(R.dimen.aw_bubbles_max_size))
                .crossColor(VersionUtil.color(context, R.color.aw_cross_default))
                .crossOverlappedColor(VersionUtil.color(context, R.color.aw_cross_overlapped))
				.build();
		playPauseButton = new PlayPauseButton(configuration);
		expandCollapseWidget = new ExpandCollapseWidget(configuration);
		removeWidgetView = new RemoveWidgetView(configuration);
        float smt = context.getResources().getDimensionPixelSize(R.dimen.aw_significant_movement_threshold);
		TouchManager playPauseButtonManager = TouchManager.create(playPauseButton, Configuration.CLICK_THRESHOLD, Configuration.LONG_CLICK_THRESHOLD, smt);
		TouchManager expandedWidgetManager = TouchManager.create(expandCollapseWidget, Configuration.CLICK_THRESHOLD, Configuration.LONG_CLICK_THRESHOLD, smt);

		playPauseButtonManager.callback(new PlayPauseButtonCallback());
		expandedWidgetManager.callback(new ExpandCollapseWidgetCallback());
		expandCollapseWidget.onWidgetStateChangedListener(state -> {
			if (state == State.COLLAPSED) {
				playPauseButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				windowManager.removeView(expandCollapseWidget);
				playPauseButton.enableProgressChanges(true);
			}
			if (onWidgetStateChangedListener != null) {
				onWidgetStateChangedListener.onStateChanged(state);
			}
		});
		onControlsClickListener = new OnControlsClickListenerWrapper();
		expandCollapseWidget.onControlsClickListener(onControlsClickListener);
		playbackState.addPlaybackStateListener(new PlaybackState.PlaybackStateListener() {
			@Override
			public void onStateChanged(int oldState, int newState, Object initiator) {
				if (onPlaybackStateChangedListener != null) {
					onPlaybackStateChangedListener.onStateChanged(newState);
				}
			}

			@Override
			public void onProgressChanged(int position, int duration, float percentage) {

			}
		});
	}

	@NonNull
	private Controller newController() {
		return new Controller() {

			@Override
			public void start() {
				playbackState.start(AudioWidget.this);
			}

			@Override
			public void pause() {
				playbackState.pause(AudioWidget.this);
			}

			@Override
			public void stop() {
				playbackState.stop(AudioWidget.this);
			}

			@Override
			public int duration() {
				return playbackState.duration();
			}

			@Override
			public void duration(int duration) {
				playbackState.duration(duration);
			}

			@Override
			public int position() {
				return playbackState.position();
			}

			@Override
			public void position(int position) {
				playbackState.position(position);
			}

			@Override
			public void onControlsClickListener(@Nullable OnControlsClickListener onControlsClickListener) {
				AudioWidget.this.onControlsClickListener.onControlsClickListener(onControlsClickListener);
			}

			@Override
			public void onWidgetStateChangedListener(@Nullable OnWidgetStateChangedListener onWidgetStateChangedListener) {
				AudioWidget.this.onWidgetStateChangedListener = onWidgetStateChangedListener;
			}

			@Override
			public void onPlaybackStateChangedListener(@Nullable OnPlaybackStateChangedListener onPlaybackStateChangedListener) {
				AudioWidget.this.onPlaybackStateChangedListener = onPlaybackStateChangedListener;
			}

			@Override
			public int state() {
				return playbackState.state();
			}

			@Override
			public void albumCover(@Nullable Drawable albumCover) {
				expandCollapseWidget.albumCover(albumCover);
			}

			@Override
			public void albumCover(@Nullable Bitmap bitmap) {
				if (bitmap == null)
					expandCollapseWidget.albumCover(null);
				else
					expandCollapseWidget.albumCover(new BitmapDrawable(context.getResources(), bitmap));
			}
		};
	}

	public void show(int left, int top) {
		if (shown) {
			return;
		}
		shown = true;
		float remWidX = screenSize.x / 2 - radius;
		hiddenRemWidY = screenSize.y + radius;
		visibleRemWidY = screenSize.y - height;
		show(removeWidgetView, (int) remWidX, (int) hiddenRemWidY);
		show(playPauseButton, left, top);
	}

	public void hide() {
		if (!shown) {
			return;
		}
		shown = false;
		released = true;
		windowManager.removeView(playPauseButton);
		try {
			windowManager.removeView(expandCollapseWidget);
		} catch (IllegalArgumentException e) {
			// widget not added to window yet
		}
	}

	@NonNull
	public Controller controller() {
		return controller;
	}

	private void show(View view, int left, int top) {
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);
		params.gravity = GravityCompat.START | Gravity.TOP;
		params.x = left;
		params.y = top;
		windowManager.addView(view, params);
	}

	private class PlayPauseButtonCallback extends TouchManager.SimpleCallback {

		private final ValueAnimator.AnimatorUpdateListener animatorUpdateListener;
		private boolean readyToRemove;

		public PlayPauseButtonCallback() {
			animatorUpdateListener = animation -> {
				if (!removeWidgetShown)
					return;
				WindowManager.LayoutParams params = (WindowManager.LayoutParams) removeWidgetView.getLayoutParams();
				float y = (float) animation.getAnimatedValue();
				params.y = (int) y;
				windowManager.updateViewLayout(removeWidgetView, params);
			};
		}

		@Override
		public void onClick(float x, float y) {
			playPauseButton.onClick();
			if (onControlsClickListener != null) {
				onControlsClickListener.onPlayPauseClicked();
			}
		}

		@Override
		public void onLongClick(float x, float y) {
			released = true;
			playPauseButton.enableProgressChanges(false);
			playPauseButton.postDelayed(this::checkSpaceAndShowExpanded, PlayPauseButton.PROGRESS_CHANGES_DURATION);
		}

		@Override
		public boolean canBeTouched() {
			return !playPauseButton.isAnimationInProgress();
		}

		@SuppressWarnings("deprecation")
		private void checkSpaceAndShowExpanded() {
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
			int x = params.x;
			int y = params.y;
			int expandDirection;
			if (x + height > screenSize.x / 2) {
				expandDirection = ExpandCollapseWidget.DIRECTION_LEFT;
			} else {
				expandDirection = ExpandCollapseWidget.DIRECTION_RIGHT;
			}
			if (expandDirection == ExpandCollapseWidget.DIRECTION_LEFT) {
				x -= width - height * 1.5f;
			} else {
				x += height / 2f;
			}
			playPauseButton.setLayerType(View.LAYER_TYPE_NONE, null);
			show(expandCollapseWidget, x, y);
			expandCollapseWidget.expand(expandDirection);
		}

		@Override
		public void onTouched() {
			super.onTouched();
			released = false;
			handler.postDelayed(() -> {
				if (!released) {
					removeWidgetShown = true;
					ValueAnimator animator = ValueAnimator.ofFloat(hiddenRemWidY, visibleRemWidY);
					animator.setDuration(200);
					animator.addUpdateListener(animatorUpdateListener);
					animator.start();
				}
			}, Configuration.LONG_CLICK_THRESHOLD);
			playPauseButton.onTouchDown();
		}

		@Override
		public void onMoved(float diffX, float diffY) {
			super.onMoved(diffX, diffY);
			boolean curReadyToRemove = isReadyToRemove();
			if (curReadyToRemove != readyToRemove) {
				readyToRemove = curReadyToRemove;
				removeWidgetView.setOverlapped(readyToRemove);
			}
		}

		@Override
		public void onReleased() {
			super.onReleased();
			playPauseButton.onTouchUp();
			released = true;
			if (removeWidgetShown) {
				ValueAnimator animator = ValueAnimator.ofFloat(visibleRemWidY, hiddenRemWidY);
				animator.setDuration(200);
				animator.addUpdateListener(animatorUpdateListener);
				animator.addListener(new SimpleAnimatorListener() {

					@Override
					public void onAnimationEnd(Animator animation) {
						removeWidgetShown = false;
						if (!shown) {
							windowManager.removeView(removeWidgetView);
						}
					}
				});
				animator.start();
			}
			if (isReadyToRemove()) {
				hide();
			}
		}

		private boolean isReadyToRemove() {
			WindowManager.LayoutParams removeParams = (WindowManager.LayoutParams) removeWidgetView.getLayoutParams();
			removeBounds.set(removeParams.x, removeParams.y, removeParams.x + height, removeParams.y + height);
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
			float cx = params.x + height;
			float cy = params.y + height;
			return removeBounds.contains(cx, cy);
		}
	}

	private class ExpandCollapseWidgetCallback extends TouchManager.SimpleCallback {
		@Override
		public void onClick(float x, float y) {
            expandCollapseWidget.onClick(x, y);
		}

		@Override
		public void onTouchOutside() {
			expandCollapseWidget.collapse();
		}

		@Override
		public boolean canBeTouched() {
			return !expandCollapseWidget.isAnimationInProgress();
		}

		@Override
		public void onMoved(float diffX, float diffY) {
			super.onMoved(diffX, diffY);
			WindowManager.LayoutParams widgetParams = (WindowManager.LayoutParams) expandCollapseWidget.getLayoutParams();
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
			if (expandCollapseWidget.expandDirection() == ExpandCollapseWidget.DIRECTION_RIGHT) {
				params.x = (int) (widgetParams.x - radius);
			} else {
				params.x = (int) (widgetParams.x + width - height - radius);
			}
			params.y = widgetParams.y;
			windowManager.updateViewLayout(playPauseButton, params);
		}
	}

	private class OnControlsClickListenerWrapper implements OnControlsClickListener {

		private OnControlsClickListener onControlsClickListener;

		public OnControlsClickListenerWrapper onControlsClickListener(OnControlsClickListener inner) {
			this.onControlsClickListener = inner;
			return this;
		}

		@Override
		public void onPlaylistClicked() {
			if (onControlsClickListener != null) {
				onControlsClickListener.onPlaylistClicked();
			}
		}

		@Override
		public void onPreviousClicked() {
			if (onControlsClickListener != null) {
				onControlsClickListener.onPreviousClicked();
			}
		}

		@Override
		public void onPlayPauseClicked() {
			if (playbackState.state() != Controller.STATE_PLAYING) {
				playbackState.start(AudioWidget.this);
			} else {
				playbackState.pause(AudioWidget.this);
			}
			if (onControlsClickListener != null) {
				onControlsClickListener.onPlayPauseClicked();
			}
		}

		@Override
		public void onNextClicked() {
			if (onControlsClickListener != null) {
				onControlsClickListener.onNextClicked();
			}
		}

		@Override
		public void onAlbumClicked() {
			if (onControlsClickListener != null) {
				onControlsClickListener.onAlbumClicked();
			}
		}
	}

	public interface Controller {

		int STATE_STOPPED = 0;
		int STATE_PLAYING = 1;
		int STATE_PAUSED = 2;

		void start();
		void pause();
		void stop();
		int duration();
		void duration(int duration);
		int position();
		void position(int position);
		void onControlsClickListener(@Nullable OnControlsClickListener onControlsClickListener);
		void onWidgetStateChangedListener(@Nullable OnWidgetStateChangedListener onWidgetStateChangedListener);
		void onPlaybackStateChangedListener(@Nullable OnPlaybackStateChangedListener onPlaybackStateChangedListener);
		int state();
		void albumCover(@Nullable Drawable albumCover);
		void albumCover(@Nullable Bitmap bitmap);
	}

	public interface OnControlsClickListener {
		void onPlaylistClicked();
		void onPreviousClicked();
		void onPlayPauseClicked();
		void onNextClicked();
		void onAlbumClicked();
	}

	public interface OnWidgetStateChangedListener {
		void onStateChanged(@NonNull State state);
	}

	public interface OnPlaybackStateChangedListener {
		void onStateChanged(int newState);
	}

	public enum State {
		COLLAPSED,
		EXPANDED
	}
}
