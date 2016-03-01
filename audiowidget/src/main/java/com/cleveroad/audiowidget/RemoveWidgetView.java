package com.cleveroad.audiowidget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.View;

/**
 * Created by Александр on 29.02.2016.
 */
@SuppressLint("ViewConstructor")
class RemoveWidgetView extends View {

	private final float size;
	private final float radius;
	private final Paint paint;

	public RemoveWidgetView(@NonNull Configuration configuration) {
		super(configuration.context());
		this.radius = configuration.radius();
		this.size = configuration.radius() * 2;
		this.paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(DrawableUtils.dpToPx(configuration.context(), 4));
		paint.setColor(configuration.lightColor());
		paint.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int size = MeasureSpec.makeMeasureSpec((int) this.size, MeasureSpec.EXACTLY);
		super.onMeasure(size, size);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int cx = canvas.getWidth() >> 1;
		int cy = canvas.getHeight() >> 1;
		float rad = radius * 0.75f;
		canvas.drawCircle(cx, cy, rad, paint);
		drawCross(canvas, cx, cy, rad * 0.5f, 45);
	}

	private void drawCross(@NonNull Canvas canvas, float cx, float cy, float radius, float startAngle) {
		drawLine(canvas, cx, cy, radius, startAngle);
		drawLine(canvas, cx, cy, radius, startAngle + 90);
	}

	private void drawLine(@NonNull Canvas canvas, float cx, float cy, float radius, float angle) {
		float x1 = DrawableUtils.rotateX(cx, cy + radius, cx, cy, angle);
		float y1 = DrawableUtils.rotateY(cx, cy + radius, cx, cy, angle);
		angle += 180;
		float x2 = DrawableUtils.rotateX(cx, cy + radius, cx, cy, angle);
		float y2 = DrawableUtils.rotateY(cx, cy + radius, cx, cy, angle);
		canvas.drawLine(x1, y1, x2, y2, paint);
	}

	public void setColor(@ColorInt int color) {
		paint.setColor(color);
		invalidate();
	}
}
