package com.cleveroad.audiowidget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Touch detector for views.
 */
class TouchManager implements View.OnTouchListener {

	private final View view;
	private final WindowManager windowManager;
	private final float[] bounds;
	private final int rootWidth;
	private final int rootHeight;
    private final long clickThreshold;
    private final long longClickThreshold;
    private final float significantMovementThreshold;


	private long onDownTimestamp;
	private boolean longClickCanceled;
	private float prevRawX;
	private float prevRawY;
    private float prevX;
    private float prevY;
	private float prevLeft;
	private float prevTop;
	private boolean movedFarEnough;
	private boolean longClickPerformed;
	/**
	 * This flag indicates {@link MotionEvent#ACTION_DOWN} was called and handled by touch manager.
	 * Without it manager will receive other actions even if {@link MotionEvent#ACTION_DOWN} not handled.
	 */
	private boolean downCalled;
	private Callback callback;

	private TouchManager(@NonNull View view, long clickThreshold, long longClickThreshold, float significantMovementThreshold) {
		this.view = view;
        this.clickThreshold = clickThreshold;
        this.longClickThreshold = longClickThreshold;
        this.significantMovementThreshold = significantMovementThreshold;
		this.view.setOnTouchListener(this);
		Context context = view.getContext().getApplicationContext();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		this.rootWidth = context.getResources().getDisplayMetrics().widthPixels;
		this.rootHeight = context.getResources().getDisplayMetrics().heightPixels - context.getResources().getDimensionPixelSize(R.dimen.aw_status_bar_height);

		this.bounds = new float[2];
	}

	public TouchManager callback(Callback callback) {
		this.callback = callback;
		return this;
	}

	@Override
	public boolean onTouch(@NonNull View v, @NonNull MotionEvent event) {
		if (callback != null && !callback.canBeTouched())
			return false;
		WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) view.getLayoutParams();
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN: {
				prevRawX = event.getRawX();
				prevRawY = event.getRawY();
                prevX = event.getX();
                prevY = event.getY();
				prevLeft = layoutParams.x;
				prevTop = layoutParams.y;
				onDownTimestamp = System.currentTimeMillis();
				longClickCanceled = false;
				movedFarEnough = false;
				longClickPerformed = false;
				downCalled = true;
				postDelayed(() -> {
					if (!longClickCanceled && !movedFarEnough) {
						longClickPerformed = true;
						if (callback != null) {
							callback.onLongClick(prevX, prevY);
						}
					}
				}, longClickThreshold);
				if (callback != null) {
					callback.onTouched();
				}
				return true;
			}
			case MotionEvent.ACTION_MOVE: {
				if (!downCalled || longClickPerformed) {
					break;
				}
				float diffX = event.getRawX() - prevRawX;
				float diffY = event.getRawY() - prevRawY;
				float l = prevLeft + diffX;
				float t = prevTop + diffY;
				float r = l + layoutParams.width;
				float b = t + layoutParams.height;
				if (view instanceof BoundsChecker) {
					((BoundsChecker) view).checkBounds(l, t, r, b, rootWidth, rootHeight, bounds);
					l = bounds[0];
					t = bounds[1];
				}
				movedFarEnough = Math.hypot(diffX, diffY) >= significantMovementThreshold;
				layoutParams.x = (int) l;
				layoutParams.y = (int) t;
				windowManager.updateViewLayout(view, layoutParams);
				if (callback != null) {
					callback.onMoved(diffX, diffY);
				}
				return true;
			}
			case MotionEvent.ACTION_UP: {
				long curTime = System.currentTimeMillis();
				long diff = curTime - onDownTimestamp;
				if (diff <= longClickThreshold) {
					longClickCanceled = true;
				}
				if (diff <= clickThreshold) {
					if (callback != null) {
						callback.onClick(prevX, prevY);
					}
				}
				if (callback != null) {
					callback.onReleased();
				}
				return true;
			}
			case MotionEvent.ACTION_OUTSIDE: {
				if (callback != null) {
					callback.onTouchOutside();
				}
				return true;
			}
		}
		return false;
	}

	private void postDelayed(@NonNull Runnable runnable, long delayMillis) {
		view.postDelayed(runnable, delayMillis);
	}

	public static TouchManager create(@NonNull View view, long clickThreshold, long longClickThreshold, float significantMovementThreshold) {
		return new TouchManager(view, clickThreshold, longClickThreshold, significantMovementThreshold);
	}

	interface Callback {
		void onClick(float x, float y);
		void onLongClick(float x, float y);
		void onTouchOutside();
		void onTouched();
		void onMoved(float diffX, float diffY);
		void onReleased();
		boolean canBeTouched();
	}

	public static class SimpleCallback implements Callback {

		@Override
		public void onClick(float x, float y) {

		}

		@Override
		public void onLongClick(float x, float y) {

		}

		@Override
		public void onTouchOutside() {

		}

		@Override
		public void onTouched() {

		}

		@Override
		public void onMoved(float diffX, float diffY) {

		}

		@Override
		public void onReleased() {

		}

		@Override
		public boolean canBeTouched() {
			return true;
		}
	}

	interface BoundsChecker {
		void checkBounds(float left, float top, float right, float bottom, float screenWidth, float screenHeight, float[] outBounds);
	}
}
