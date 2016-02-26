package com.cleveroad.audiowidget;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by Александр on 24.02.2016.
 */
public abstract class ClickableShape extends Shape {

	private static final long CLICK_THRESHOLD = 200;
	private static final long LONG_CLICK_THRESHOLD = 400;
	private static final float MOVEMENT_THRESHOLD = 10.0f;

	private long onDownTimestamp;
	private boolean longClickCanceled;
	private float prevX;
	private float prevY;
	private float prevLeft;
	private float prevTop;
	private boolean movedFarEnough;
	private boolean longClickPerformed;
	private int rootWidth;
	private int rootHeight;

	public ClickableShape(@NonNull Configuration configuration) {
		super(configuration);
	}

	public boolean isTouched(float x, float y) {
		return bounds().contains(x, y);
	}

	public boolean onTouchEvent(@NonNull MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				prevX = event.getX();
				prevY = event.getY();
				prevLeft = bounds().left;
				prevTop = bounds().top;
				onDownTimestamp = System.currentTimeMillis();
				longClickCanceled = false;
				movedFarEnough = false;
				longClickPerformed = false;
				handler().postDelayed(() -> {
					if (!longClickCanceled && !movedFarEnough) {
						longClickPerformed = true;
						onLongClick(prevX, prevY);
					}
				}, LONG_CLICK_THRESHOLD);
				return true;
			}
			case MotionEvent.ACTION_MOVE: {
				if (longClickPerformed) {
					return false;
				}
				float diffX = event.getX() - prevX;
				float diffY = event.getY() - prevY;
				float l = prevLeft + diffX;
				float t = prevTop + diffY;
				if (l < 0) {
					l = 0;
				}
				if (t < 0) {
					t = 0;
				}
				float r = l + bounds().width();
				float b = t + bounds().height();
				if (rootWidth > 0 && r > rootWidth) {
					r = rootWidth;
					l = rootWidth - bounds().width();
				}
				if (rootHeight > 0 && b > rootHeight) {
					b = rootHeight;
					t = rootHeight - bounds().height();
				}
				if (l < 0 || t < 0) {
					Log.w("SHAPE", "Can't place shape in correct position. Root view is too small.");
				}
				movedFarEnough = Math.hypot(diffX, diffY) >= MOVEMENT_THRESHOLD;
				bounds().set(l, t ,r ,b);
				onBoundsChanged(l, t, r, b);
				invalidater().invalidate();
				return true;
			}
			case MotionEvent.ACTION_UP: {
				long curTime = System.currentTimeMillis();
				long diff = curTime - onDownTimestamp;
				if (diff <= LONG_CLICK_THRESHOLD) {
					longClickCanceled = true;
				}
				if (diff <= CLICK_THRESHOLD) {
					onClick(prevX, prevY);
				}
				return true;
			}
		}
		return false;
	}

	protected void onBoundsChanged(float l, float t, float r, float b) {

	}

	public int rootWidth() {
		return rootWidth;
	}

	public int rootHeight() {
		return rootHeight;
	}

	protected abstract void onClick(float x, float y);

	protected abstract void onLongClick(float x, float y);
}
