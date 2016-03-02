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
        expandCollapseWidget.onWidgetStateChangedListener(new OnWidgetStateChangedListener() {
            @Override
            public void onWidgetStateChanged(@NonNull State state) {
                if (state == State.COLLAPSED) {
                    playPauseButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    windowManager.removeView(expandCollapseWidget);
                    playPauseButton.enableProgressChanges(true);
                }
                if (onWidgetStateChangedListener != null) {
                    onWidgetStateChangedListener.onWidgetStateChanged(state);
                }
            }

            @Override
            public void onWidgetPositionChanged(int cx, int cy) {

            }
        });
        onControlsClickListener = new OnControlsClickListenerWrapper();
        expandCollapseWidget.onControlsClickListener(onControlsClickListener);
    }

    /**
     * Create new controller.
     *
     * @return new controller
     */
    @NonNull
    private Controller newController() {
        return new Controller() {

            @Override
            public void start() {
                playbackState.start(this);
            }

            @Override
            public void pause() {
                playbackState.pause(this);
            }

            @Override
            public void stop() {
                playbackState.stop(this);
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

    /**
     * Show widget at specified position.
     *
     * @param cx center x
     * @param cy center y
     */
    public void show(int cx, int cy) {
        if (shown) {
            return;
        }
        shown = true;
        float remWidX = screenSize.x / 2 - radius;
        hiddenRemWidY = screenSize.y + radius;
        visibleRemWidY = screenSize.y - height;
        show(removeWidgetView, (int) remWidX, (int) hiddenRemWidY);
        show(playPauseButton, (int) (cx - height), (int) (cy - height));
    }

    /**
     * Hide widget.
     */
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
        if (onWidgetStateChangedListener != null) {
            onWidgetStateChangedListener.onWidgetStateChanged(State.REMOVED);
        }
    }

    /**
     * Get current visibility state.
     *
     * @return true if widget shown on screen, false otherwise.
     */
    public boolean isShown() {
        return shown;
    }

    /**
     * Get widget controller.
     *
     * @return widget controller
     */
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
        params.gravity = Gravity.START | Gravity.TOP;
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
            } else {
                if (onWidgetStateChangedListener != null) {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
                    onWidgetStateChangedListener.onWidgetPositionChanged((int) (params.x + height), (int) (params.y + height));
                }
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
            if (onWidgetStateChangedListener != null) {
                onWidgetStateChangedListener.onWidgetPositionChanged((int) (params.x + height), (int) (params.y + height));
            }
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
        public boolean onPlayPauseClicked() {
            if (onControlsClickListener == null || onControlsClickListener.onPlayPauseClicked()) {
                if (playbackState.state() != Configuration.STATE_PLAYING) {
                    playbackState.start(AudioWidget.this);
                } else {
                    playbackState.pause(AudioWidget.this);
                }
                return true;
            }
            return false;
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

    /**
     * Audio widget controller.
     */
    public interface Controller {

        /**
         * Start playback.
         */
        void start();

        /**
         * Pause playback.
         */
        void pause();

        /**
         * Stop playback.
         */
        void stop();

        /**
         * Get track duration.
         *
         * @return track duration
         */
        int duration();

        /**
         * Set track duration.
         *
         * @param duration track duration
         */
        void duration(int duration);

        /**
         * Get track position.
         *
         * @return track position
         */
        int position();

        /**
         * Set track position.
         *
         * @param position track position
         */
        void position(int position);

        /**
         * Set controls click listener.
         *
         * @param onControlsClickListener controls click listener
         */
        void onControlsClickListener(@Nullable OnControlsClickListener onControlsClickListener);

        /**
         * Set widget state change listener.
         *
         * @param onWidgetStateChangedListener widget state change listener
         */
        void onWidgetStateChangedListener(@Nullable OnWidgetStateChangedListener onWidgetStateChangedListener);

        /**
         * Set album cover.
         *
         * @param albumCover album cover or null to set default one
         */
        void albumCover(@Nullable Drawable albumCover);

        /**
         * Set album cover.
         *
         * @param albumCover album cover or null to set default one
         */
        void albumCover(@Nullable Bitmap albumCover);
    }

    /**
     * Listener for control clicks.
     */
    public interface OnControlsClickListener {

        /**
         * Called when playlist button clicked.
         */
        void onPlaylistClicked();

        /**
         * Called when previous track button clicked.
         */
        void onPreviousClicked();

        /**
         * Called when play/pause button clicked.
         */
        boolean onPlayPauseClicked();

        /**
         * Called when next track button clicked.
         */
        void onNextClicked();

        /**
         * Called when album icon clicked.
         */
        void onAlbumClicked();
    }

    /**
     * Listener for widget state changes.
     */
    public interface OnWidgetStateChangedListener {

        /**
         * Called when widget state changed.
         *
         * @param state new widget state
         */
        void onWidgetStateChanged(@NonNull State state);

        /**
         * Called when position of widget is changed.
         *
         * @param cx center x
         * @param cy center y
         */
        void onWidgetPositionChanged(int cx, int cy);
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        REMOVED
    }
}
