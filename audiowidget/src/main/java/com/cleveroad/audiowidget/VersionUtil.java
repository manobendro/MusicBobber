package com.cleveroad.audiowidget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

/**
 * Util that dealing with Android versions methods.
 */
public class VersionUtil {
	private VersionUtil() {

	}

    /**
     * Get color from resources.
     * @param context instance of context
     * @param colorId color resource id
     * @return color int
     */
    @ColorInt
	@SuppressWarnings("deprecation")
	public static int color(@NonNull Context context, @ColorRes int colorId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return context.getColor(colorId);
		}
		return context.getResources().getColor(colorId);
	}

    /**
     * Get drawable from resources.
     * @param context instance of context
     * @param drawableId drawable resource id
     * @return drawable
     */
	@SuppressWarnings("deprecation")
	public static Drawable drawable(@NonNull Context context, @DrawableRes int drawableId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return context.getDrawable(drawableId);
		}
		return context.getResources().getDrawable(drawableId);
	}
}
