package com.cleveroad.audiowidget;

import android.animation.Animator;
import android.animation.FloatEvaluator;
import android.animation.IntEvaluator;
import android.animation.PropertyValuesHolder;
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
    private final int rootWidth;
    private final int rootHeight;
    private final int edgeOffsetX;
    private final int edgeOffsetY;
    private final StickyEdgeAnimator stickyEdgeAnimator;
    private final VelocityAnimator velocityAnimator;

    private GestureListener gestureListener;
    private GestureDetector gestureDetector;
    private Callback callback;

    private TouchManager(@NonNull View view, @NonNull BoundsChecker boundsChecker, int edgeOffsetX, int edgeOffsetY) {
        this.gestureDetector = new GestureDetector(view.getContext(), gestureListener = new GestureListener());
        gestureDetector.setIsLongpressEnabled(true);
        this.view = view;
        this.boundsChecker = boundsChecker;
        this.view.setOnTouchListener(this);
        this.edgeOffsetX = edgeOffsetX;
        this.edgeOffsetY = edgeOffsetY;
        Context context = view.getContext().getApplicationContext();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.rootWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.rootHeight = context.getResources().getDisplayMetrics().heightPixels - context.getResources().getDimensionPixelSize(R.dimen.aw_status_bar_height);
        stickyEdgeAnimator = new StickyEdgeAnimator();
        velocityAnimator = new VelocityAnimator();
    }

    public TouchManager callback(Callback callback) {
        this.callback = callback;
        return this;
    }

    private Float lastRawX, lastRawY;

    @Override
    public boolean onTouch(@NonNull View v, @NonNull MotionEvent event) {
        boolean res = gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            gestureListener.onUpEvent(event);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            gestureListener.onMove(event);
        } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            gestureListener.onTouchOutsideEvent(event);
        }
        return res;
    }

    public static TouchManager create(@NonNull View view, @NonNull BoundsChecker boundsChecker, int edgeOffsetX, int edgeOffsetY) {
        return new TouchManager(view, boundsChecker, edgeOffsetX, edgeOffsetY);
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

        float stickyLeftSide(float screenWidth);

        float stickyRightSide(float screenWidth);

        float stickyTopSide(float screenHeight);

        float stickyBottomSide(float screenHeight);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private int prevX, prevY;
        private float velX, velY;
        private long lastEventTime;

        @Override
        public boolean onDown(MotionEvent e) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) view.getLayoutParams();
            prevX = params.x;
            prevY = params.y;
            boolean result = !stickyEdgeAnimator.isAnimating();
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
            velocityAnimator.animate(velX, velY);
            return true;
        }

        private void onMove(MotionEvent e2) {
            if (lastRawX != null && lastRawY != null) {
                long diff = e2.getEventTime() - lastEventTime;
                float dt = diff == 0 ? 0 : 1000f / diff;
                float newVelX = (e2.getRawX() - lastRawX) * dt;
                float newVelY = (e2.getRawY() - lastRawY) * dt;
                velX = DrawableUtils.smooth(velX, newVelX, 0.2f);
                velY = DrawableUtils.smooth(velY, newVelY, 0.2f);
            }
            lastRawX = e2.getRawX();
            lastRawY = e2.getRawY();
            lastEventTime = e2.getEventTime();
        }

        private void onUpEvent(MotionEvent e) {
            if (callback != null) {
                callback.onReleased(e.getX(), e.getY());
            }
            lastRawX = null;
            lastRawY = null;
            lastEventTime = 0;
            velX = velY = 0;
            if (!velocityAnimator.isAnimating()) {
                stickyEdgeAnimator.animate();
            }
        }

        private void onTouchOutsideEvent(MotionEvent e) {
            if (callback != null) {
                callback.onTouchOutside();
            }
        }
    }

    private class VelocityAnimator {
        private final ValueAnimator velocityAnimator;
        private final PropertyValuesHolder dxHolder;
        private final PropertyValuesHolder dyHolder;
        private WindowManager.LayoutParams params;
        private long prevPlayTime;

        public VelocityAnimator() {
            dxHolder = PropertyValuesHolder.ofFloat("dx", 0, 0);
            dyHolder = PropertyValuesHolder.ofFloat("dy", 0, 0);
            dxHolder.setEvaluator(new FloatEvaluator());
            dyHolder.setEvaluator(new FloatEvaluator());
            velocityAnimator = ValueAnimator.ofPropertyValuesHolder(dxHolder, dyHolder);
            velocityAnimator.setDuration(500);
            velocityAnimator.addUpdateListener(animation -> {
                long curPlayTime = animation.getCurrentPlayTime();
                long dt = curPlayTime - prevPlayTime;
                float dx = (float) animation.getAnimatedValue("dx") * dt / 1000f;
                float dy = (float) animation.getAnimatedValue("dy") * dt / 1000f;
                prevPlayTime = curPlayTime;
                params.x += dx;
                params.y += dy;
                if (callback != null) {
                    callback.onMoved(dx, dy);
                }
                windowManager.updateViewLayout(view, params);
            });
            velocityAnimator.addListener(new SimpleAnimatorListener() {

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    prevPlayTime = 0;
                    stickyEdgeAnimator.animate();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    prevPlayTime = 0;
                    stickyEdgeAnimator.animate();
                }
            });
        }

        public void animate(float velocityX, float velocityY) {
            if (isAnimating())
                return;
            params = (WindowManager.LayoutParams) view.getLayoutParams();
            dxHolder.setFloatValues(velocityX, 0);
            dyHolder.setFloatValues(velocityY, 0);
            velocityAnimator.start();
        }

        public boolean isAnimating() {
            return velocityAnimator.isRunning();
        }
    }

    private class StickyEdgeAnimator {
        private final PropertyValuesHolder dxHolder;
        private final PropertyValuesHolder dyHolder;
        private final ValueAnimator edgeAnimator;
        private WindowManager.LayoutParams params;

        public StickyEdgeAnimator() {
            dxHolder = PropertyValuesHolder.ofInt("x", 0, 0);
            dyHolder = PropertyValuesHolder.ofInt("y", 0, 0);
            dxHolder.setEvaluator(new IntEvaluator());
            dyHolder.setEvaluator(new IntEvaluator());
            edgeAnimator = ValueAnimator.ofPropertyValuesHolder(dxHolder, dyHolder);
            edgeAnimator.setDuration(200);
            edgeAnimator.addUpdateListener(animation -> {
                int x = (int) animation.getAnimatedValue("x");
                int y = (int) animation.getAnimatedValue("y");
                if (callback != null) {
                    callback.onMoved(x - params.x, y - params.y);
                }
                params.x = x;
                params.y = y;
                windowManager.updateViewLayout(view, params);
            });
        }

        public void animate() {
            params = (WindowManager.LayoutParams) view.getLayoutParams();
            float cx = params.x + view.getWidth() / 2f;
            float cy = params.y + view.getWidth() / 2f;
            int x;
            if (cx < rootWidth / 2f) {
                x = (int) boundsChecker.stickyLeftSide(rootWidth) + edgeOffsetX;
            } else {
                x = (int) boundsChecker.stickyRightSide(rootWidth) - edgeOffsetX;
            }
            int y = params.y;
            int top = (int) boundsChecker.stickyTopSide(rootHeight) + edgeOffsetY;
            int bottom = (int) boundsChecker.stickyBottomSide(rootHeight) - edgeOffsetY;
            if (params.y > bottom || params.y < top) {
                if (cy < rootHeight / 2f) {
                    y = top;
                } else {
                    y = bottom;
                }
            }
            dxHolder.setIntValues(params.x, x);
            dyHolder.setIntValues(params.y, y);
            edgeAnimator.start();
        }

        public boolean isAnimating() {
            return edgeAnimator.isRunning();
        }
    }
}
