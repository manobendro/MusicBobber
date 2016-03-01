package com.cleveroad.audiowidget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Александр on 24.02.2016.
 */
@SuppressLint("ViewConstructor")
class PlayPauseButton extends View implements PlaybackState.PlaybackStateListener, TouchManager.BoundsChecker {

	private static final float BUBBLE_MIN_SIZE = 10;
	private static final float BUBBLE_MAX_SIZE = 20;
	private static final float BUBBLES_ANGLE_STEP = 18.0f;
	private static final float ANIMATION_TIME_F = 12 * Configuration.FRAME_SPEED;
	private static final long ANIMATION_TIME_L = (long) ANIMATION_TIME_F;
	private static final float COLOR_ANIMATION_TIME_F = ANIMATION_TIME_F / 4f;
	private static final float COLOR_ANIMATION_TIME_START_F = (ANIMATION_TIME_F - COLOR_ANIMATION_TIME_F) / 2;
	private static final float COLOR_ANIMATION_TIME_END_F = COLOR_ANIMATION_TIME_START_F + COLOR_ANIMATION_TIME_F;
	private static final int TOTAL_BUBBLES_COUNT = (int) (360 / BUBBLES_ANGLE_STEP);

	private final TimeInterval timeInterval;
	private final Paint buttonPaint;
	private final Paint bubblesPaint;
	private final int pausedColor;
	private final int playingColor;
	private final float[] bubbleSizes;
	private final float[] bubbleSpeeds;
	private final Random random;
	private final ColorChanger colorChanger;
	private final Drawable playDrawable;
	private final Drawable pauseDrawable;
	private final float radius;
	private final PlaybackState playbackState;
	private final ValueAnimator touchDownAnimator;
	private final ValueAnimator touchUpAnimator;

	private boolean animatingBubbles;
	private Timer timer;
	private float startAngle;
	private float buttonSize = 1.0f;

	public PlayPauseButton(@NonNull Configuration configuration) {
		super(configuration.context());
		this.playbackState = configuration.playbackState();
		this.random = configuration.random();
		this.timeInterval = new TimeInterval();
		this.buttonPaint = new Paint();
		this.buttonPaint.setColor(configuration.pauseColor());
		this.buttonPaint.setStyle(Paint.Style.FILL);
		this.buttonPaint.setAntiAlias(true);
		this.bubblesPaint = new Paint();
		this.bubblesPaint.setStyle(Paint.Style.FILL);
		this.pausedColor = configuration.pauseColor();
		this.playingColor = configuration.playColor();
		this.radius = configuration.radius();
		this.bubbleSizes = new float[TOTAL_BUBBLES_COUNT];
		this.bubbleSpeeds = new float[TOTAL_BUBBLES_COUNT];
		this.colorChanger = new ColorChanger();
		this.playDrawable = configuration.playDrawable().getConstantState().newDrawable();
		this.pauseDrawable = configuration.pauseDrawable().getConstantState().newDrawable();
		this.pauseDrawable.setAlpha(0);
		this.playbackState.addPlaybackStateListener(this);
		final ValueAnimator.AnimatorUpdateListener listener = animation -> {
			buttonSize = (float) animation.getAnimatedValue();
			invalidate();
		};
		this.touchDownAnimator = ValueAnimator.ofFloat(1, 0.9f).setDuration(100);
		this.touchDownAnimator.addUpdateListener(listener);
		this.touchUpAnimator = ValueAnimator.ofFloat(0.9f, 1).setDuration(100);
		this.touchUpAnimator.addUpdateListener(listener);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int size = MeasureSpec.makeMeasureSpec((int) (radius * 4), MeasureSpec.EXACTLY);
		super.onMeasure(size , size);
	}

	public void onClick() {
		if (!animatingBubbles) {
			if (playbackState.state() == PlaybackState.STATE_PLAYING) {
				colorChanger
						.fromColor(playingColor)
						.toColor(pausedColor);
				bubblesPaint.setColor(pausedColor);
				playbackState.pause(this);
			} else {
				colorChanger
						.fromColor(pausedColor)
						.toColor(playingColor);
				bubblesPaint.setColor(playingColor);
				playbackState.start(this);
			}
			startBubblesAnimation();
		}
	}

	public boolean isAnimationInProgress() {
		return animatingBubbles;
	}

	private void startBubblesAnimation() {
		animatingBubbles = true;
		startAngle = 360 * random.nextFloat();
		for (int i=0; i<TOTAL_BUBBLES_COUNT; i++) {
			float speed = 0.5f + 0.5f * random.nextFloat();
			float size = BUBBLE_MIN_SIZE + (BUBBLE_MAX_SIZE - BUBBLE_MIN_SIZE) * random.nextFloat();
			float radius = size / 2f;
			bubbleSizes[i] = radius;
			bubbleSpeeds[i] = speed;
		}
		setupTimer(ANIMATION_TIME_L, null);
	}

	private void stopAnyAnimation() {
		animatingBubbles = false;
		timer.cancel();
		timer.purge();
		timer = null;
		timeInterval.reset();
		invalidate();
	}

	public void onTouchDown() {
		touchDownAnimator.start();
	}

	public void onTouchUp() {
		touchUpAnimator.start();
	}

	@Override
	public void onDraw(@NonNull Canvas canvas) {
		float cx = getWidth() >> 1;
		float cy = getHeight() >> 1;
		canvas.scale(buttonSize, buttonSize, cx, cy);
		if (animatingBubbles) {
			timeInterval.step();
			float dur = timeInterval.duration();
			if (dur > ANIMATION_TIME_F) {
				dur = ANIMATION_TIME_F;
			}
			float dt = DrawableUtils.normalize(dur, 0, ANIMATION_TIME_F);
			int alpha = (int) DrawableUtils.customFunction(dt, 0, 0, 0, 0.3f, 255, 0.5f, 225, 0.7f, 0, 1f);
			bubblesPaint.setAlpha(alpha);
			if (DrawableUtils.isBetween(dur, COLOR_ANIMATION_TIME_START_F, COLOR_ANIMATION_TIME_END_F)) {
				float colorDt = DrawableUtils.normalize(dur, COLOR_ANIMATION_TIME_START_F, COLOR_ANIMATION_TIME_END_F);
				buttonPaint.setColor(colorChanger.nextColor(colorDt));
				if (playbackState.state() == PlaybackState.STATE_PLAYING) {
					pauseDrawable.setAlpha((int) DrawableUtils.between(255 * colorDt, 0, 255));
					playDrawable.setAlpha((int) DrawableUtils.between(255 * (1 - colorDt), 0, 255));
				} else {
					playDrawable.setAlpha((int) DrawableUtils.between(255 * colorDt, 0, 255));
					pauseDrawable.setAlpha((int) DrawableUtils.between(255 * (1 - colorDt), 0, 255));
				}
			}

			for (int i=0; i<TOTAL_BUBBLES_COUNT; i++) {
				float angle = startAngle + BUBBLES_ANGLE_STEP * i;
				float speed = dt * bubbleSpeeds[i];
				float x = DrawableUtils.rotateX(cx, cy * (1 - speed), cx, cy, angle);
				float y = DrawableUtils.rotateY(cx, cy * (1 - speed), cx, cy, angle);
				canvas.drawCircle(x, y, bubbleSizes[i], bubblesPaint);
			}
		} else if (playbackState.state() != PlaybackState.STATE_PLAYING) {
			playDrawable.setAlpha(255);
			pauseDrawable.setAlpha(0);
		} else {
			playDrawable.setAlpha(0);
			pauseDrawable.setAlpha(255);
		}

		canvas.drawCircle(cx, cy, radius, buttonPaint);

		int l = (int) (cx - radius + Configuration.BUTTON_PADDING);
		int t = (int) (cy - radius + Configuration.BUTTON_PADDING);
		int r = (int) (cx + radius - Configuration.BUTTON_PADDING);
		int b = (int) (cy + radius - Configuration.BUTTON_PADDING);
		if (animatingBubbles || playbackState.state() != PlaybackState.STATE_PLAYING) {
			playDrawable.setBounds(l, t, r, b);
			playDrawable.draw(canvas);
		}
		if (animatingBubbles || playbackState.state() == PlaybackState.STATE_PLAYING) {
			pauseDrawable.setBounds(l, t, r, b);
			pauseDrawable.draw(canvas);
		}
	}

	private void setupTimer(long duration, Runnable runnable) {
		timer = new Timer("PlayPauseButton Timer");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				postInvalidate();
			}
		}, Configuration.UPDATE_INTERVAL, Configuration.UPDATE_INTERVAL);
		postDelayed(() -> {
			stopAnyAnimation();
			if (runnable != null) {
				runnable.run();
			}
		}, duration);
		timeInterval.step();
	}

	@Override
	public void onStateChanged(int oldState, int newState, Object initiator) {
		if (initiator == this)
			return;
		if (newState == PlaybackState.STATE_PLAYING) {
			buttonPaint.setColor(playingColor);
			pauseDrawable.setAlpha(255);
			playDrawable.setAlpha(0);
		} else {
			buttonPaint.setColor(pausedColor);
			pauseDrawable.setAlpha(0);
			playDrawable.setAlpha(255);
		}
		invalidate();
	}

	@Override
	public void onProgressChanged(int position, int duration, float percentage) {

	}

	@Override
	public void checkBounds(float left, float top, float right, float bottom, float screenWidth, float screenHeight, float[] outBounds) {
		float bLeft = left + radius;
		float bTop = top + radius;
		if (bLeft < 0) {
			bLeft = 0;
		}
		if (bTop < 0) {
			bTop = 0;
		}
		float size = radius * 2;
		float bRight = bLeft + size;
		float bBottom = bTop + size;
		if (bRight > screenWidth) {
			bRight = screenWidth;
			bLeft = bRight - size;
		}
		if (bBottom > screenHeight) {
			bBottom = screenHeight;
			bTop = bBottom - size;
		}
		outBounds[0] = bLeft - radius;
		outBounds[1] = bTop - radius;
	}
}