package com.cleveroad.audiowidget;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
	private float width, height;

	public AudioWidget(@NonNull Context context) {
		context = context.getApplicationContext();
		this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		this.playbackState = new PlaybackState();
		height = context.getResources().getDimensionPixelSize(R.dimen.player_height);
		width = context.getResources().getDimensionPixelSize(R.dimen.player_width);
		float radius = height / 2f;
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
		expandCollapseWidget.onCollapseListener(() -> windowManager.removeView(expandCollapseWidget));
	}

	@SuppressWarnings("deprecation")
	private void checkSpaceAndShowExpanded() {
		Point size = new Point();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			windowManager.getDefaultDisplay().getSize(size);
		} else {
			size.x = windowManager.getDefaultDisplay().getWidth();
			size.y = windowManager.getDefaultDisplay().getHeight();
		}
		WindowManager.LayoutParams params = (WindowManager.LayoutParams) playPauseButton.getLayoutParams();
		int x = params.x;
		int y = params.y;
		byte expandDirection;
		if (x + params.width / 2f > size.x / 2) {
			expandDirection = ExpandCollapseWidget.DIRECTION_LEFT;
		} else {
			expandDirection = ExpandCollapseWidget.DIRECTION_RIGHT;
		}
		if (expandDirection == ExpandCollapseWidget.DIRECTION_LEFT) {
			x -= width - height * 1.5f;
		} else {
			x += height / 2f;
		}
		show(expandCollapseWidget, x, y);
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
						| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
						| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
				PixelFormat.TRANSLUCENT);
		params.gravity = GravityCompat.START | Gravity.TOP;
		params.x = left;
		params.y = top;
		windowManager.addView(view, params);
	}
}
