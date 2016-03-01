package com.cleveroad.audiowidget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;

import java.util.Random;

/**
 * Created by Александр on 24.02.2016.
 */
class Configuration {

	public static final float FRAME_SPEED = 70.0f;
	public static final int BUTTON_PADDING = 24;

	public static final long CLICK_THRESHOLD = 200;
	public static final long LONG_CLICK_THRESHOLD = 400;
	public static final float MOVEMENT_THRESHOLD = 10.0f;

	private final int lightColor;
	private final int darkColor;
	private final int progressColor;
	private final int expandedColor;
	private final Random random;
	private final float width;
	private final float height;
	private final Drawable playDrawable;
	private final Drawable pauseDrawable;
	private final Drawable prevDrawable;
	private final Drawable nextDrawable;
	private final Drawable playlistDrawable;
	private final Drawable albumDrawable;
	private final Context context;
	private final PlaybackState playbackState;

	private Configuration(Builder builder) {
		this.context = builder.context;
		this.random = builder.random;
		this.width = builder.width;
		this.height = builder.radius;
		this.lightColor = builder.lightColor;
		this.darkColor = builder.darkColor;
		this.progressColor = builder.progressColor;
		this.expandedColor = builder.expandedColor;
		this.playlistDrawable = builder.playlistDrawable;
		this.playDrawable = builder.playDrawable;
		this.pauseDrawable = builder.pauseDrawable;
		this.prevDrawable = builder.prevDrawable;
		this.nextDrawable = builder.nextDrawable;
		this.albumDrawable = builder.albumDrawable;
		this.playbackState = builder.playbackState;
	}

	public Context context() {
		return context;
	}

	public Random random() {
		return random;
	}

	@ColorInt
	public int lightColor() {
		return lightColor;
	}

	@ColorInt
	public int darkColor() {
		return darkColor;
	}

	@ColorInt
	public int progressColor() {
		return progressColor;
	}

	@ColorInt
	public int expandedColor() {
		return expandedColor;
	}

	public float widgetWidth() {
		return width;
	}

	public float radius() {
		return height;
	}

	public Drawable playDrawable() {
		return playDrawable;
	}

	public Drawable pauseDrawable() {
		return pauseDrawable;
	}

	public Drawable prevDrawable() {
		return prevDrawable;
	}

	public Drawable nextDrawable() {
		return nextDrawable;
	}

	public Drawable playlistDrawable() {
		return playlistDrawable;
	}

	public Drawable albumDrawable() {
		return albumDrawable;
	}

	public PlaybackState playbackState() {
		return playbackState;
	}

	public static final class Builder {

		private int lightColor;
		private int darkColor;
		private int progressColor;
		private int expandedColor;
		private float width;
		private float radius;
		private Context context;
		private Random random;
		private Drawable playDrawable;
		private Drawable pauseDrawable;
		private Drawable prevDrawable;
		private Drawable nextDrawable;
		private Drawable playlistDrawable;
		private Drawable albumDrawable;
		private PlaybackState playbackState;

		public Builder context(Context context) {
			this.context = context;
			return this;
		}

		public Builder playColor(@ColorInt int pauseColor) {
			this.lightColor = pauseColor;
			return this;
		}

		public Builder darkColor(@ColorInt int playColor) {
			this.darkColor = playColor;
			return this;
		}

		public Builder progressColor(@ColorInt int progressColor) {
			this.progressColor = progressColor;
			return this;
		}

		public Builder expandedColor(@ColorInt int expandedColor) {
			this.expandedColor = expandedColor;
			return this;
		}

		public Builder random(Random random) {
			this.random = random;
			return this;
		}

		public Builder widgetWidth(float width) {
			this.width = width;
			return this;
		}

		public Builder radius(float radius) {
			this.radius = radius;
			return this;
		}

		public Builder playDrawable(@Nullable Drawable playDrawable) {
			this.playDrawable = playDrawable;
			return this;
		}

		public Builder pauseDrawable(@Nullable Drawable pauseDrawable) {
			this.pauseDrawable = pauseDrawable;
			return this;
		}

		public Builder prevDrawable(@Nullable Drawable prevDrawable) {
			this.prevDrawable = prevDrawable;
			return this;
		}

		public Builder nextDrawable(@Nullable Drawable nextDrawable) {
			this.nextDrawable = nextDrawable;
			return this;
		}

		public Builder playlistDrawable(@Nullable Drawable plateDrawable) {
			this.playlistDrawable = plateDrawable;
			return this;
		}

		public Builder albumDrawable(@Nullable Drawable albumDrawable) {
			this.albumDrawable = albumDrawable;
			return this;
		}

		public Builder playbackState(PlaybackState playbackState) {
			this.playbackState = playbackState;
			return this;
		}

		public Configuration build() {
			return new Configuration(this);
		}
	}
}
