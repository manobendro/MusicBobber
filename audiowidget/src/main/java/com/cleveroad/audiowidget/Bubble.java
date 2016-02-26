package com.cleveroad.audiowidget;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by Александр on 24.02.2016.
 */
public class Bubble {

	private final Paint paint;
	private float radius;
	private float speed;

	public Bubble(@NonNull Paint paint) {
		this.paint = paint;
	}

	public void update(float radius) {
		this.radius = radius;
	}

	public void draw(@NonNull Canvas canvas, float dt, float angle) {
		int cx = canvas.getWidth() >> 1;
		int cy = canvas.getHeight() >> 1;
		float curX = cx;
		float curY = cy - cy * dt;
		Log.d("TEST", curY + ", " + angle);
		curX = DrawableUtils.rotateX(curX, curY, cx, cy, angle);
		curY = DrawableUtils.rotateY(curX, curY, cx, cy, angle);
		canvas.drawCircle(curX, curY, radius, paint);
	}
}
