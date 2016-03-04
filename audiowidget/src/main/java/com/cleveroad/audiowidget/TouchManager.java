package com.cleveroad.audiowidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * Touch detector for views.
 */
class TouchManager implements View.OnTouchListener {

    private final View view;
    private final BoundsChecker boundsChecker;
    private final WindowManager windowManager;
    private final float[] bounds;
    private final int rootWidth;
    private final int rootHeight;
    private final ValueAnimator stickyEdgeAnimator;
    private GestureListener gestureListener;
    private GestureDetector gestureDetector;
    private Callback callback;

    private TouchManager(@NonNull View view, @NonNull BoundsChecker boundsChecker) {
        this.gestureDetector = new GestureDetector(view.getContext(), gestureListener = new GestureListener());
        gestureDetector.setIsLongpressEnabled(true);
        this.view = view;
        this.boundsChecker = boundsChecker;
        this.view.setOnTouchListener(this);
        Context context = view.getContext().getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.rootWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.rootHeight = context.getResources().getDisplayMetrics().heightPixels - context.getResources().getDimensionPixelSize(R.dimen.aw_status_bar_height);
        this.bounds = new float[2];
        this.stickyEdgeAnimator = ValueAnimator.ofInt();
        stickyEdgeAnimator.setDuration(200);
        stickyEdgeAnimator.addUpdateListener(animation -> {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            int prevX1 = params.x;
            params.x = (int) animation.getAnimatedValue();
            if (callback != null) {
                callback.onMoved(params.x - prevX1, 0);
            }
            windowManager.updateViewLayout(view, params);
        });
    }

    public TouchManager callback(Callback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public boolean onTouch(@NonNull View v, @NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            gestureListener.onUpEvent(event);
        } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            gestureListener.onTouchOutsideEvent(event);
        }
        return gestureDetector.onTouchEvent(event);
    }

    private void animateToBounds() {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
        float cx = params.x + view.getWidth() / 2f;
        int position;
        if (cx < rootWidth / 2f) {
            position = (int) boundsChecker.stickyLeftSide(rootWidth);
        } else {
            position = (int) boundsChecker.stickyRightSide(rootWidth);
        }
        stickyEdgeAnimator.setIntValues(params.x, position);
        stickyEdgeAnimator.start();
    }

    public static TouchManager create(@NonNull View view, @NonNull BoundsChecker boundsChecker) {
        return new TouchManager(view, boundsChecker);
    }

    interface Callback {
        void onClick(float x, float y);

        void onLongClick(float x, float y);

        void onTouchOutside();

        void onTouched(float x, float y);

        void onMoved(float diffX, float diffY);

        void onReleased(float x, float y);
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
        public void onTouched(float x, float y) {

        }

        @Override
        public void onMoved(float diffX, float diffY) {

        }

        @Override
        public void onReleased(float x, float y) {

        }

    }

    interface BoundsChecker {
        void checkBounds(float left, float top, float right, float bottom, float screenWidth, float screenHeight, float[] outBounds);

        float stickyLeftSide(float screenWidth);

        float stickyRightSide(float screenWidth);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private int prevX, prevY;

        @Override
        public boolean onDown(MotionEvent e) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            prevX = params.x;
            prevY = params.y;
            boolean result = !stickyEdgeAnimator.isRunning();
            if (result) {
                if (callback != null) {
                    callback.onTouched(e.getX(), e.getY());
                }
            }
            return result;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (callback != null) {
                callback.onClick(e.getX(), e.getY());
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float diffX = e2.getRawX() - e1.getRawX();
            float diffY = e2.getRawY() - e1.getRawY();
            float l = prevX + diffX;
            float t = prevY + diffY;
            float r = l + view.getWidth();
            float b = t + view.getHeight();
            boundsChecker.checkBounds(l, t, r, b, rootWidth, rootHeight, bounds);
            l = bounds[0];
            t = bounds[1];
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            params.x = (int) l;
            params.y = (int) t;
            windowManager.updateViewLayout(view, params);
            if (callback != null) {
                callback.onMoved(distanceX, distanceY);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if (callback != null) {
                callback.onLongClick(e.getX(), e.getY());
            }
            onUpEvent(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        private void onUpEvent(MotionEvent e) {
            if (callback != null) {
                callback.onReleased(e.getX(), e.getY());
            }
            animateToBounds();
        }

        private void onTouchOutsideEvent(MotionEvent e) {
            if (callback != null) {
                callback.onTouchOutside();
            }
        }
    }
}
