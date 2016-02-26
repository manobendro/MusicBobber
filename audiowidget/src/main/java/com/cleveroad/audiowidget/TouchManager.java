package com.cleveroad.audiowidget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by Александр on 26.02.2016.
 */
public class TouchManager implements View.OnTouchListener {

	private static final long CLICK_THRESHOLD = 200;
	private static final long LONG_CLICK_THRESHOLD = 400;
	private static final float MOVEMENT_THRESHOLD = 10.0f;

	private final View view;
	private final WindowManager windowManager;
	private final int rootWidth;
	private final int rootHeight;

	private long onDownTimestamp;
	private boolean longClickCanceled;
	private float prevX;
	private float prevY;
	private float prevLeft;
	private float prevTop;
	private boolean movedFarEnough;
	private boolean longClickPerformed;
	private Callback callback;

	private TouchManager(@NonNull View view) {
		this.view = view;
		this.view.setOnTouchListener(this);
		Context context = view.getContext().getApplicationContext();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		rootWidth = context.getResources().getDisplayMetrics().widthPixels;
		rootHeight = context.getResources().getDisplayMetrics().heightPixels;
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
				prevX = event.getRawX();
				prevY = event.getRawY();
				prevLeft = layoutParams.x;
				prevTop = layoutParams.y;
				onDownTimestamp = System.currentTimeMillis();
				longClickCanceled = false;
				movedFarEnough = false;
				longClickPerformed = false;
				postDelayed(() -> {
					if (!longClickCanceled && !movedFarEnough) {
						longClickPerformed = true;
						if (callback != null) {
							callback.onLongClick(prevX, prevY);
						}
					}
				}, LONG_CLICK_THRESHOLD);
				return true;
			}
			case MotionEvent.ACTION_MOVE: {
				if (longClickPerformed) {
					return false;
				}
				float diffX = event.getRawX() - prevX;
				float diffY = event.getRawY() - prevY;
				float l = prevLeft + diffX;
				float t = prevTop + diffY;
				if (l < 0) {
					l = 0;
				}
				if (t < 0) {
					t = 0;
				}
				float r = l + layoutParams.width;
				float b = t + layoutParams.height;
				if (rootWidth > 0 && r > rootWidth) {
					l = rootWidth - layoutParams.width;
				}
				if (rootHeight > 0 && b > rootHeight) {
					t = rootHeight - layoutParams.height;
				}
				if (l < 0 || t < 0) {
					Log.w("SHAPE", "Can't place shape in correct position. Root view is too small.");
				}
				movedFarEnough = Math.hypot(diffX, diffY) >= MOVEMENT_THRESHOLD;
				layoutParams.x = (int) l;
				layoutParams.y = (int) t;
				windowManager.updateViewLayout(view, layoutParams);
				return true;
			}
			case MotionEvent.ACTION_UP: {
				long curTime = System.currentTimeMillis();
				long diff = curTime - onDownTimestamp;
				if (diff <= LONG_CLICK_THRESHOLD) {
					longClickCanceled = true;
				}
				if (diff <= CLICK_THRESHOLD) {
					if (callback != null) {
						callback.onClick(prevX, prevY);
					}
				}
				return true;
			}
			case MotionEvent.ACTION_OUTSIDE: {
				if (callback != null) {
					callback.onTouchOutside();
				}
				Log.d("TEST", "touched outside");
				break;
			}
		}
		return false;
	}

	private void postDelayed(@NonNull Runnable runnable, long delayMillis) {
		view.postDelayed(runnable, delayMillis);
	}

	public static TouchManager create(View view) {
		return new TouchManager(view);
	}

	interface Callback {
		void onClick(float x, float y);
		void onLongClick(float x, float y);
		void onTouchOutside();
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
		public boolean canBeTouched() {
			return true;
		}
	}
}
