package com.cleveroad.audiowidget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

/**
 * Util that dealing with Android versions methods.
 */
public class VersionUtil {
	private VersionUtil() {

	}

	@SuppressWarnings("deprecation")
	public static int color(@NonNull Context context, @ColorRes int colorId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return context.getColor(colorId);
		}
		return context.getResources().getColor(colorId);
	}

	@SuppressWarnings("deprecation")
	public static Drawable drawable(@NonNull Context context, @DrawableRes int drawableId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return context.getDrawable(drawableId);
		}
		return context.getResources().getDrawable(drawableId);
	}
}
