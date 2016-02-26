package com.cleveroad.audiowidget;

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
public class PlayPauseButton extends View implements PlaybackState.PlaybackStateListener {

	private static final float BUBBLE_MIN_SIZE = 10;
	private static final float BUBBLE_MAX_SIZE = 20;
	private static final float BUBBLES_ANGLE_STEP = 18.0f;
	private static final float ANIMATION_TIME_F = 12 * Configuration.FRAME_SPEED;
	private static final long ANIMATION_TIME_L = (long) ANIMATION_TIME_F;
	private static final float COLOR_ANIMATION_TIME_F = ANIMATION_TIME_F / 4f;
	private static final float COLOR_ANIMATION_TIME_START_F = (ANIMATION_TIME_F - COLOR_ANIMATION_TIME_F) / 2;
	private static final float COLOR_ANIMATION_TIME_END_F = COLOR_ANIMATION_TIME_START_F + COLOR_ANIMATION_TIME_F;
	private static final int TOTAL_BUBBLES_COUNT = (int) (360 / BUBBLES_ANGLE_STEP);

	private static final float MOVE_DURATION_F = 3 * Configuration.FRAME_SPEED;
	private static final long MOVE_DURATION_L = (long) MOVE_DURATION_F;

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

	private boolean animatingBubbles;
	private Timer timer;

	private byte expandDirection;
	private float moveFrom, moveTo;
	private boolean animatingMove;
	private float startAngle;


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
				playbackState.pause();
			} else {
				colorChanger
						.fromColor(pausedColor)
						.toColor(playingColor);
				bubblesPaint.setColor(playingColor);
				playbackState.start();
			}
			startBubblesAnimation();
		}
	}

	public boolean isAnimationInProgress() {
		return animatingBubbles || animatingMove;
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
		animatingMove = false;
		animatingBubbles = false;
		timer.cancel();
		timer.purge();
		timer = null;
		timeInterval.reset();
		invalidate();
	}

	@Override
	public void onDraw(@NonNull Canvas canvas) {
		float cx = getWidth() >> 1;
		float cy = getHeight() >> 1;
		if (animatingMove) {
			timeInterval.step();
			updateMoveAnimation();
		} else if (animatingBubbles) {
			timeInterval.step();
			float dur = timeInterval.duration();
			if (dur > ANIMATION_TIME_F) {
				dur = ANIMATION_TIME_F;
			}
			float dt = DrawableUtils.normalize(dur, 0, ANIMATION_TIME_F);
			int alpha = (int) DrawableUtils.customFunction(dt, 0, 0, 0, 0.3f, 255, 0.5f, 225, 0.7f, 0, 1f);
			bubblesPaint.setAlpha(alpha);
			if (dur >= COLOR_ANIMATION_TIME_START_F && dur <= COLOR_ANIMATION_TIME_END_F) {
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
				canvas.drawCircle(x, y, bubbleSizes[i], buttonPaint);
			}
		}

		canvas.drawCircle(cx, cy, radius, buttonPaint);
		int l = (int) (cx - radius + Configuration.BUTTON_PADDING);
		int t = (int) (cy - radius + Configuration.BUTTON_PADDING);
		int r = (int) (cx + radius - Configuration.BUTTON_PADDING);
		int b = (int) (cy + radius - Configuration.BUTTON_PADDING);
		playDrawable.setBounds(l, t, r, b);
		pauseDrawable.setBounds(l, t, r, b);
		playDrawable.draw(canvas);
		pauseDrawable.draw(canvas);
	}

	private void updateMoveAnimation() {
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, MOVE_DURATION_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), 0, MOVE_DURATION_F);
			float l;
			if (expandDirection == ExpandCollapseWidget.DIRECTION_LEFT) {
				l = DrawableUtils.enlarge(moveFrom, moveTo, time);
			} else {
				l = DrawableUtils.reduce(moveFrom, moveTo, time);
			}
			// TODO: 26.02.2016 use touchmanager
//			float t = bounds().top;
//			float b = bounds().bottom;
//			float r = l + bounds().width();
//			bounds().set(l, t, r, b);
		}
	}

	public void startMoveAnimation(float moveFrom, float moveTo, byte expandDirection, Runnable runnable) {
		animatingMove = true;
		this.moveFrom = moveFrom;
		this.moveTo = moveTo;
		this.expandDirection = expandDirection;
		setupTimer(MOVE_DURATION_L, runnable);
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
	public void onStateChanged(int oldState, int newState) {
		invalidate();
	}

	@Override
	public void onProgressChanged(int position, int duration, float percentage) {

	}
}