package com.cleveroad.audiowidget;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
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

	private PlayPauseButton playPauseButton;
	private ExpandCollapseWidget expandCollapseWidget;
	private final WindowManager windowManager;
	private final PlaybackState playbackState;
	private final ExpandCollapseWidget.OnCollapseListener collapseListener;

	public AudioWidget(@NonNull Context context) {
		context = context.getApplicationContext();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		this.playbackState = new PlaybackState();
		int height = context.getResources().getDimensionPixelSize(R.dimen.player_height);
		int width = context.getResources().getDimensionPixelSize(R.dimen.player_width);
		float radius = height >> 1;
		int playColor = VersionUtil.color(context, R.color.bg_dark);
		int pauseColor = VersionUtil.color(context, R.color.bg_lite);
		int progressColor = VersionUtil.color(context, R.color.bg_progress);
		int expandColor = VersionUtil.color(context, R.color.bg_progress);

		Drawable playDrawable = VersionUtil.drawable(context, R.drawable.ic_play);
		Drawable pauseDrawable = VersionUtil.drawable(context, R.drawable.ic_pause);
		Drawable prevDrawable = VersionUtil.drawable(context, R.drawable.ic_previous);
		Drawable nextDrawable = VersionUtil.drawable(context, R.drawable.ic_next);
		Drawable plateDrawable = VersionUtil.drawable(context, R.drawable.ic_plate);
		Drawable albumDrawable = VersionUtil.drawable(context, R.drawable.ic_album);

		Configuration configuration = new Configuration.Builder()
				.context(context)
				.playbackState(playbackState)
				.handler(new Handler())
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
		TouchManager playPauseButtonManager = TouchManager.create(playPauseButton);
		TouchManager expandedWidgetManager = TouchManager.create(expandCollapseWidget);

		playPauseButtonManager.callback(new TouchManager.SimpleCallback() {
			@Override
			public void onClick(float x, float y) {
				playPauseButton.onClick();
			}

			@Override
			public void onLongClick(float x, float y) {
				checkSpaceAndShowExpanded();
				windowManager.removeView(playPauseButton);
			}

			@Override
			public boolean canBeTouched() {
				return !playPauseButton.isAnimationInProgress();
			}
		});
		expandedWidgetManager.callback(new TouchManager.SimpleCallback() {
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
		});
		this.collapseListener = () -> {
			WindowManager.LayoutParams params = (WindowManager.LayoutParams) expandCollapseWidget.getLayoutParams();
			show(playPauseButton, params.x, params.y);
			windowManager.removeView(expandCollapseWidget);
		};
		expandCollapseWidget.onCollapseListener(collapseListener);
	}

	private void checkSpaceAndShowExpanded() {
		byte expandDirection = ExpandCollapseWidget.DIRECTION_LEFT;
		//				if (playPauseButton.bounds().right - widgetWidth >= 0) {
//					expandDirection = DIRECTION_LEFT;
//					startExpandAnimation();
//				} else if (playPauseButton.bounds().left + widgetWidth <= rootWidth()) {
//					expandDirection = DIRECTION_RIGHT;
//					startExpandAnimation();
//				} else {
//					float moveFrom, moveTo;
//					if (playPauseButton.bounds().centerX() < rootWidth() >> 1) {
//						expandDirection = DIRECTION_RIGHT;
//						moveFrom = playPauseButton.bounds().left;
//						moveTo = playPauseButton.bounds().left - (widgetWidth - (rootWidth() - playPauseButton.bounds().left)) - 3 * Configuration.BUTTON_PADDING;
//					} else {
//						expandDirection = DIRECTION_LEFT;
//						moveFrom = playPauseButton.bounds().left;
//						moveTo = playPauseButton.bounds().left + widgetWidth - playPauseButton.bounds().right + 3 * Configuration.BUTTON_PADDING;
//					}
//					playPauseButton.startMoveAnimation(moveFrom, moveTo, expandDirection, this::startExpandAnimation);
//				}
		WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
		show(expandCollapseWidget, params.x, params.y);
		expandCollapseWidget.expand(expandDirection);
	}

	public void show(int left, int top) {
		show(playPauseButton, left, top);
	}

	private void show(View view, int left, int top) {
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
				PixelFormat.TRANSLUCENT);
		params.gravity = GravityCompat.START | Gravity.TOP;
		params.x = left;
		params.y = top;
		windowManager.addView(view, params);
	}


}
