package com.cleveroad.audiowidget;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Handler;
import android.support.annotation.NonNull;

/**
 * Created by Александр on 24.02.2016.
 */
public abstract class Shape {

	private final RectF bounds;
	private final Invalidater invalidater;
	private final Handler handler;

	public Shape(@NonNull Configuration configuration) {
		this.invalidater = configuration.invalidater();
		this.handler = configuration.handler();
		this.bounds = new RectF();
	}

	protected RectF bounds() {
		return bounds;
	}


	public Invalidater invalidater() {
		return invalidater;
	}

	public Handler handler() {
		return handler;
	}

	protected abstract void draw(@NonNull Canvas canvas);
}
