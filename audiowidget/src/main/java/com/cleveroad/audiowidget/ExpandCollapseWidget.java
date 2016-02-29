package com.cleveroad.audiowidget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Александр on 25.02.2016.
 */
@SuppressLint("ViewConstructor")
class ExpandCollapseWidget extends View implements PlaybackState.PlaybackStateListener {

	static final byte DIRECTION_LEFT = 1;
	static final byte DIRECTION_RIGHT = 2;

	private static final float EXPAND_DURATION_F = (34 * Configuration.FRAME_SPEED);
	private static final long EXPAND_DURATION_L = (long) EXPAND_DURATION_F;
	private static final float EXPAND_COLOR_END_F = 9 * Configuration.FRAME_SPEED;
	private static final float EXPAND_SIZE_END_F = 12 * Configuration.FRAME_SPEED;
	private static final float EXPAND_POSITION_START_F = 10 * Configuration.FRAME_SPEED;
	private static final float EXPAND_POSITION_END_F = 18 * Configuration.FRAME_SPEED;
	private static final float EXPAND_BUBBLES_START_F = 18 * Configuration.FRAME_SPEED;
	private static final float EXPAND_BUBBLES_END_F = 32 * Configuration.FRAME_SPEED;
	private static final float EXPAND_ELEMENTS_START_F = 20 * Configuration.FRAME_SPEED;
	private static final float EXPAND_ELEMENTS_END_F = 27 * Configuration.FRAME_SPEED;

	private static final float COLLAPSE_DURATION_F = 12 * Configuration.FRAME_SPEED;
	private static final long COLLAPSE_DURATION_L = (long) COLLAPSE_DURATION_F;
	private static final float COLLAPSE_ELEMENTS_END_F = 3 * Configuration.FRAME_SPEED;
	private static final float COLLAPSE_SIZE_START_F = 2 * Configuration.FRAME_SPEED;
	private static final float COLLAPSE_SIZE_END_F = 12 * Configuration.FRAME_SPEED;
	private static final float COLLAPSE_POSITION_START_F = 3 * Configuration.FRAME_SPEED;
	private static final float COLLAPSE_POSITION_END_F = 12 * Configuration.FRAME_SPEED;


	private static final int INDEX_PLATE = 0;
	private static final int INDEX_PREV = 1;
	private static final int INDEX_PLAY = 2;
	private static final int INDEX_NEXT = 3;
	private static final int INDEX_ALBUM = 4;
	private static final int INDEX_PAUSE = 5;

	private static final int TOTAL_BUBBLES_COUNT = 30;
	private static final float BUBBLE_MIN_SIZE = 10;
	private static final float BUBBLE_MAX_SIZE = 25;


	private final Paint paint;
	private final float radius;
	private final float widgetWidth;
	private final float widgetHeight;
	private final TimeInterval timeInterval;
	private final ColorChanger colorChanger;
	private final int playColor;
	private final int pauseColor;
	private final int widgetColor;
	private final Drawable[] drawables;
	private final Rect[] buttonBounds;
	private final Rect[] animationBounds;
	private final float sizeStep;
	private final float[] bubbleSizes;
	private final float[] bubbleSpeeds;
	private final float[] bubblePositions;
	private final Random random;
	private final Paint bubblesPaint;
	private final RectF bounds;
	private final PlaybackState playbackState;

	private boolean expanded;
	private boolean animatingExpand, animatingCollapse;
	private Timer timer;
	private boolean updatedBubbles;
	private byte expandDirection;
	private OnCollapseListener collapseListener;

	public ExpandCollapseWidget(@NonNull Configuration configuration) {
		super(configuration.context());
		this.playbackState = configuration.playbackState();
		this.random = configuration.random();
		this.bubblesPaint = new Paint();
		this.bubblesPaint.setStyle(Paint.Style.FILL);
		this.bubblesPaint.setAntiAlias(true);
		this.bubblesPaint.setColor(configuration.expandedColor());
		this.paint = new Paint();
		this.paint.setColor(configuration.expandedColor());
		this.paint.setAntiAlias(true);
		this.radius = configuration.radius();
		this.widgetWidth = configuration.widgetWidth();
		this.timeInterval = new TimeInterval();
		this.colorChanger = new ColorChanger();
		this.playColor = configuration.playColor();
		this.pauseColor = configuration.pauseColor();
		this.widgetColor = configuration.expandedColor();
		this.buttonBounds = new Rect[5];
		this.animationBounds = new Rect[5];
		this.drawables = new Drawable[6];
		this.bounds = new RectF();
		drawables[INDEX_PLATE] = configuration.plateDrawable().getConstantState().newDrawable();
		drawables[INDEX_PREV] = configuration.prevDrawable().getConstantState().newDrawable();
		drawables[INDEX_PLAY] = configuration.playDrawable().getConstantState().newDrawable();
		drawables[INDEX_PAUSE] = configuration.pauseDrawable().getConstantState().newDrawable();
		drawables[INDEX_NEXT] = configuration.nextDrawable().getConstantState().newDrawable();
		drawables[INDEX_ALBUM] = configuration.albumDrawable().getConstantState().newDrawable();
		sizeStep = widgetWidth / 5f;
		widgetHeight = radius * 2;
		for (int i = 0; i < buttonBounds.length; i++) {
			buttonBounds[i] = new Rect();
			animationBounds[i] = new Rect();
			int l, t, r, b;
			l = (int) (i * sizeStep + Configuration.BUTTON_PADDING);
			t = (int) (radius + Configuration.BUTTON_PADDING);
			r = (int) ((i + 1) * sizeStep - Configuration.BUTTON_PADDING);
			b = (int) (radius * 3 - Configuration.BUTTON_PADDING);
			buttonBounds[i].set(l, t, r, b);
		}
		this.bubbleSizes = new float[TOTAL_BUBBLES_COUNT];
		this.bubbleSpeeds = new float[TOTAL_BUBBLES_COUNT];
		this.bubblePositions = new float[TOTAL_BUBBLES_COUNT * 2];
		this.playbackState.addPlaybackStateListener(this);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int w = MeasureSpec.makeMeasureSpec((int) widgetWidth, MeasureSpec.EXACTLY);
		int h = MeasureSpec.makeMeasureSpec((int) (widgetHeight * 2), MeasureSpec.EXACTLY);
		float halfH = widgetHeight / 2;
		bounds.set(0, halfH, widgetWidth, widgetHeight + halfH);
		super.onMeasure(w, h);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		if (isAnimationInProgress()) {
			timeInterval.step();
		}
		if (animatingExpand) {
			updateExpandAnimation();
		} else if (animatingCollapse) {
			updateCollapseAnimation();
		}
		drawInt(canvas);
	}

	private void drawInt(@NonNull Canvas canvas) {
		if (animatingExpand) {
			if (DrawableUtils.isBetween(timeInterval.duration(), EXPAND_BUBBLES_START_F, EXPAND_BUBBLES_END_F)) {
				float time = DrawableUtils.normalize(timeInterval.duration(), EXPAND_BUBBLES_START_F, EXPAND_BUBBLES_END_F);
				int half = TOTAL_BUBBLES_COUNT / 2;
				for (int i=0; i<TOTAL_BUBBLES_COUNT; i++) {
					float radius = bubbleSizes[i];
					float speed = bubbleSpeeds[i] * time;
					float cx = bubblePositions[2 * i];
					float cy = bubblePositions[2 * i + 1];
					if (i < half)
						cy *= (1 - speed);
					else
						cy *= (1 + speed);
					canvas.drawCircle(cx, cy, radius, bubblesPaint);
				}
			}
		}
		canvas.drawRoundRect(bounds, radius, radius, paint);
		drawMediaButtons(canvas);
	}

	private void drawMediaButtons(@NonNull Canvas canvas) {
		for (int i = 0; i < buttonBounds.length; i++) {
			Rect bounds;
			if (isAnimationInProgress()) {
				bounds = animationBounds[i];
			} else {
				bounds = buttonBounds[i];
			}
			Drawable drawable;
			if (i == INDEX_PLAY) {
				if (playbackState.state() == PlaybackState.STATE_PLAYING) {
					drawable = drawables[INDEX_PAUSE];
				} else {
					drawable = drawables[INDEX_PLAY];
				}
			} else {
				drawable = drawables[i];
			}
			drawable.setBounds(bounds);
			drawable.draw(canvas);
		}
	}

	private void updateExpandAnimation() {
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, EXPAND_COLOR_END_F)) {
			float t = DrawableUtils.normalize(timeInterval.duration(), 0, EXPAND_COLOR_END_F);
			paint.setColor(colorChanger.nextColor(t));
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, EXPAND_SIZE_END_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), 0, EXPAND_SIZE_END_F);
			float l, r, t, b;
			float height = radius * 2;
			b = bounds.bottom;
			t = b - height;
			if (expandDirection == DIRECTION_LEFT) {
				r = bounds.right;
				l = r - height - (widgetWidth - height) * time;
			} else {
				l = bounds.left;
				r = l + height + (widgetWidth - height) * time;
			}
			bounds.set(l, t, r, b);
		} else if (timeInterval.duration() > EXPAND_SIZE_END_F) {
			if (expandDirection == DIRECTION_LEFT) {
				bounds.left = bounds.right - widgetWidth;
			} else {
				bounds.right = bounds.left + widgetWidth;
			}

		}
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, EXPAND_POSITION_START_F)) {
			Rect bounds;
			if (expandDirection == DIRECTION_LEFT) {
				bounds = buttonBounds[INDEX_ALBUM];
			} else {
				bounds = buttonBounds[INDEX_PLATE];
			}
			animationBounds[INDEX_PLAY].set(bounds);
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, EXPAND_ELEMENTS_START_F)) {
			for (int i = 0; i < buttonBounds.length; i++) {
				if (i != INDEX_PLAY) {
					drawables[i].setAlpha(0);
				}
			}
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), EXPAND_ELEMENTS_START_F, EXPAND_ELEMENTS_END_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), EXPAND_ELEMENTS_START_F, EXPAND_ELEMENTS_END_F);
			expandCollapseElements(time);
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), EXPAND_POSITION_START_F, EXPAND_POSITION_END_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), EXPAND_POSITION_START_F, EXPAND_POSITION_END_F);
			Rect playBounds = buttonBounds[INDEX_PLAY];
			int l, t, r, b;
			t = playBounds.top;
			b = playBounds.bottom;
			if (expandDirection == DIRECTION_LEFT) {
				Rect albumBounds = buttonBounds[INDEX_ALBUM];
				l = (int) DrawableUtils.reduce(albumBounds.left, playBounds.left, time);
				r = l + playBounds.width();
			} else {
				Rect plateBounds = buttonBounds[INDEX_PLATE];
				l = (int) DrawableUtils.enlarge(plateBounds.left, playBounds.left, time);
				r = l + playBounds.width();
			}
			animationBounds[INDEX_PLAY].set(l, t, r, b);
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), EXPAND_BUBBLES_START_F, EXPAND_BUBBLES_END_F)) {
			if (!updatedBubbles) {
				updatedBubbles = true;
				int half = TOTAL_BUBBLES_COUNT / 2;
				float step = widgetWidth / half;
				for (int i=0; i<TOTAL_BUBBLES_COUNT; i++) {
					int index = i % half;
					float speed = 0.3f + 0.7f * random.nextFloat();
					float size = BUBBLE_MIN_SIZE + (BUBBLE_MAX_SIZE - BUBBLE_MIN_SIZE) * random.nextFloat();
					float radius = size / 2f;
					float cx = buttonBounds[INDEX_PLATE].left + index * step + step * random.nextFloat() * (random.nextBoolean() ? 1 : -1);
					float cy = bounds.centerY();
					bubbleSpeeds[i] = speed;
					bubbleSizes[i] = radius;
					bubblePositions[2 * i] = cx;
					bubblePositions[2 * i + 1] = cy;
				}
			}
			float time = DrawableUtils.normalize(timeInterval.duration(), EXPAND_BUBBLES_START_F, EXPAND_BUBBLES_END_F);
			bubblesPaint.setAlpha((int) DrawableUtils.customFunction(time, 0, 0, 255, 0.33f, 255, 0.66f, 0, 1f));
		} else if (timeInterval.duration() > EXPAND_BUBBLES_END_F) {
			updatedBubbles = false;
		}
	}

	private void updateCollapseAnimation() {
		if (DrawableUtils.isBetween(timeInterval.duration(), 0, COLLAPSE_ELEMENTS_END_F)) {
			float time = 1 - DrawableUtils.normalize(timeInterval.duration(), 0, COLLAPSE_ELEMENTS_END_F);
			expandCollapseElements(time);
		}
		if (timeInterval.duration() > COLLAPSE_ELEMENTS_END_F) {
			for (int i = 0; i < buttonBounds.length; i++) {
				if (i != INDEX_PLAY) {
					animationBounds[i].setEmpty();
					drawables[i].setAlpha(0);
				}
			}
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), COLLAPSE_POSITION_START_F, COLLAPSE_POSITION_END_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), COLLAPSE_POSITION_START_F, COLLAPSE_POSITION_END_F);
			Rect playBounds = buttonBounds[INDEX_PLAY];
			int l, t, r, b;
			t = playBounds.top;
			b = playBounds.bottom;
			if (expandDirection == DIRECTION_LEFT) {
				l = (int) DrawableUtils.enlarge(playBounds.left, buttonBounds[INDEX_ALBUM].left, time);
				r = l + playBounds.width();
			} else {
				l = (int) DrawableUtils.reduce(playBounds.left, buttonBounds[INDEX_PLATE].left, time);
				r = l + playBounds.width();
			}
			animationBounds[INDEX_PLAY].set(l, t, r, b);
		}
		if (DrawableUtils.isBetween(timeInterval.duration(), COLLAPSE_SIZE_START_F, COLLAPSE_SIZE_END_F)) {
			float time = DrawableUtils.normalize(timeInterval.duration(), COLLAPSE_SIZE_START_F, COLLAPSE_SIZE_END_F);
			paint.setColor(colorChanger.nextColor(time));
			float l, r, t, b;
			float height = radius * 2;
			b = bounds.bottom;
			t = bounds.top;
			if (expandDirection == DIRECTION_LEFT) {
				r = bounds.right;
				l = r - height - (widgetWidth - height) * (1 - time);
			} else {
				l = bounds.left;
				r = l + height + (widgetWidth - height) * (1 - time);
			}
			bounds.set(l, t, r, b);
		}
	}

	private void expandCollapseElements(float time) {
		int alpha = (int) DrawableUtils.between(time * 255, 0, 255);
		for (int i = 0; i < buttonBounds.length; i++) {
			Rect bounds = buttonBounds[i];
			if (i != INDEX_PLAY) {
				float w = time * bounds.width() / 2f;
				float h = time * bounds.width() / 2f;
				int cx = bounds.centerX();
				int cy = bounds.centerY();
				animationBounds[i].set((int) (cx - w), (int) (cy - h), (int) (cx + w), (int) (cy + h));
				drawables[i].setAlpha(alpha);
			}
		}
	}

	public void onClick(float x, float y) {
		if (isAnimationInProgress())
			return;
		int index = -1;
		for (int i = 0; i < buttonBounds.length; i++) {
			if (buttonBounds[i].contains((int) x, (int) y)) {
				index = i;
				break;
			}
		}
		switch (index) {
			case INDEX_PLATE: {
				onPlateClicked();
				break;
			}
			case INDEX_PREV: {
				onPrevClicked();
				break;
			}
			case INDEX_PLAY: {
				onPlayPauseClicked();
				break;
			}
			case INDEX_NEXT: {
				onNextClicked();
				break;
			}
			case INDEX_ALBUM: {
				onAlbumClicked();
				break;
			}
			default: {
				Log.w(ExpandCollapseWidget.class.getSimpleName(), "Unknown index: " + index);
				break;
			}
		}
	}

	private void onPlateClicked() {

	}

	private void onPlayPauseClicked() {
		if (playbackState.state() != PlaybackState.STATE_PLAYING) {
			playbackState.start(this);
		} else {
			playbackState.pause(this);
		}
	}

	private void onNextClicked() {

	}

	private void onPrevClicked() {

	}

	private void onAlbumClicked() {

	}

	public void expand(byte expandDirection) {
		if (expanded)
			return;
		this.expandDirection = expandDirection;
		startExpandAnimation();
	}

	private void startExpandAnimation() {
		if (isAnimationInProgress())
			return;
		animatingExpand = true;
		if (playbackState.state() == PlaybackState.STATE_PLAYING) {
			colorChanger
					.fromColor(playColor)
					.toColor(widgetColor);

		} else {
			colorChanger
					.fromColor(pauseColor)
					.toColor(widgetColor);
		}
		setupTimer(EXPAND_DURATION_L, () -> expanded = true);
	}

	private void startCollapseAnimation() {
		if (isAnimationInProgress())
			return;
		animatingCollapse = true;
		setupTimer(COLLAPSE_DURATION_L, () -> {
			expanded = false;
			if (collapseListener != null) {
				collapseListener.onCollapsed();
			}
		});
	}

	private void setupTimer(long animationDuration, Runnable runnable) {
		timer = new Timer("ExpandCollapseWidget Timer");
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
		}, animationDuration);
		timeInterval.step();
	}

	private void stopAnyAnimation() {
		animatingExpand = false;
		animatingCollapse = false;
		timer.cancel();
		timer.purge();
		timer = null;
		timeInterval.reset();
		invalidate();
	}

	public boolean isAnimationInProgress() {
		return animatingCollapse || animatingExpand;
	}

	public void collapse() {
		if (!expanded) {
			return;
		}
		if (playbackState.state() == PlaybackState.STATE_PLAYING) {
			colorChanger
					.fromColor(widgetColor)
					.toColor(playColor);
		} else {
			colorChanger
					.fromColor(widgetColor)
					.toColor(pauseColor);
		}
		startCollapseAnimation();
	}

	@Override
	public void onStateChanged(int oldState, int newState, Object initiator) {
		invalidate();
	}

	@Override
	public void onProgressChanged(int position, int duration, float percentage) {

	}

	public ExpandCollapseWidget onCollapseListener(OnCollapseListener collapseListener) {
		this.collapseListener = collapseListener;
		return this;
	}

	public byte expandDirection() {
		return expandDirection;
	}

	//
//	@Override
//	public void checkBounds(float left, float top, float right, float bottom, float screenWidth, float screenHeight, float[] outBounds) {
//		float bLeft = left + radius;
//		float bTop = top + radius;
//		if (bLeft < 0) {
//			bLeft = 0;
//		}
//		if (bTop < 0) {
//			bTop = 0;
//		}
//		float bRight = bLeft + widgetHeight;
//		float bBottom = bTop + widgetHeight;
//		if (bRight > screenWidth) {
//			bLeft = screenWidth - widgetHeight;
//		}
//		if (bBottom > screenHeight) {
//			bTop = screenHeight - widgetHeight;
//		}
//		outBounds[0] = bLeft;
//		outBounds[1] = bTop - radius;
//	}

	interface OnCollapseListener {
		void onCollapsed();
	}
}
