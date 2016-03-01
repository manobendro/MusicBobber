package com.cleveroad.audiowidget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.Random;

/**
 * Created by Александр on 24.02.2016.
 */
public class AudioWidget {

	private final PlayPauseButton playPauseButton;
	private final ExpandCollapseWidget expandCollapseWidget;
	private final RemoveWidgetView removeWidgetView;
	private final WindowManager windowManager;
	private final PlaybackState playbackState;
	private final Handler handler;
	private final Point screenSize;
	private final RectF removeBounds;
	private float hiddenRemWidY, visibleRemWidY;
	private float width, height, radius;
	private final int colorPlay, colorPause;
	private boolean shown;
	private boolean released;
	private boolean removeWidgetShown;

	@SuppressWarnings("deprecation")
	public AudioWidget(@NonNull Context context) {
		context = context.getApplicationContext();
		this.handler = new Handler();
		this.screenSize = new Point();
		this.removeBounds = new RectF();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			windowManager.getDefaultDisplay().getSize(screenSize);
		} else {
			screenSize.x = windowManager.getDefaultDisplay().getWidth();
			screenSize.y = windowManager.getDefaultDisplay().getHeight();
		}
		screenSize.y -= DrawableUtils.dpToPx(context, 25);
		this.playbackState = new PlaybackState();
		height = context.getResources().getDimensionPixelSize(R.dimen.player_height);
		width = context.getResources().getDimensionPixelSize(R.dimen.player_width);
		radius = height / 2f;

		int playColor = VersionUtil.color(context, R.color.bg_dark);
		int pauseColor = VersionUtil.color(context, R.color.bg_lite);
		int progressColor = VersionUtil.color(context, R.color.bg_progress);
		int expandColor = VersionUtil.color(context, R.color.bg_progress);
		this.colorPlay = playColor;
		this.colorPause = pauseColor;

		Drawable playDrawable = VersionUtil.drawable(context, R.drawable.ic_play);
		Drawable pauseDrawable = VersionUtil.drawable(context, R.drawable.ic_pause);
		Drawable prevDrawable = VersionUtil.drawable(context, R.drawable.ic_previous);
		Drawable nextDrawable = VersionUtil.drawable(context, R.drawable.ic_next);
		Drawable plateDrawable = VersionUtil.drawable(context, R.drawable.ic_plate);
		Drawable albumDrawable = VersionUtil.drawable(context, R.drawable.ic_album);

		Configuration configuration = new Configuration.Builder()
				.context(context)
				.playbackState(playbackState)
				.random(new Random())
				.playColor(playColor)
				.pauseColor(pauseColor)
				.progressColor(progressColor)
				.expandedColor(expandColor)
				.widgetWidth(width)
				.radius(radius)
				.plateDrawable(plateDrawable)
				.playDrawable(playDrawable)
				.prevDrawable(prevDrawable)
				.nextDrawable(nextDrawable)
				.pauseDrawable(pauseDrawable)
				.albumDrawable(albumDrawable)
				.build();
		playPauseButton = new PlayPauseButton(configuration);
		expandCollapseWidget = new ExpandCollapseWidget(configuration);
		removeWidgetView = new RemoveWidgetView(configuration);
		TouchManager playPauseButtonManager = TouchManager.create(playPauseButton);
		TouchManager expandedWidgetManager = TouchManager.create(expandCollapseWidget);

		playPauseButtonManager.callback(new PlayPauseButtonCallback());
		expandedWidgetManager.callback(new ExpandCollapseWidgetCallback());
		expandCollapseWidget.onCollapseListener(() -> windowManager.removeView(expandCollapseWidget));
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

	class PlayPauseButtonCallback extends TouchManager.SimpleCallback {

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
		}

		@Override
		public void onLongClick(float x, float y) {
			released = true;
			checkSpaceAndShowExpanded();
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
			byte expandDirection;
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
				removeWidgetView.setColor(readyToRemove ? colorPlay : colorPause);
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
				animator.addListener(new Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {

					}

					@Override
					public void onAnimationEnd(Animator animation) {
						removeWidgetShown = false;
						if (!shown) {
							windowManager.removeView(removeWidgetView);
						}
					}

					@Override
					public void onAnimationCancel(Animator animation) {

					}

					@Override
					public void onAnimationRepeat(Animator animation) {

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

	class ExpandCollapseWidgetCallback extends TouchManager.SimpleCallback {
		@Override
		public void onClick(float x, float y) {
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) expandCollapseWidget.getLayoutParams();
			float widgetX = x - params.x;
			float widgetY = y - params.y;
			expandCollapseWidget.onClick(widgetX, widgetY);
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
}
