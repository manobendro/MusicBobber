package com.cleveroad.audiowidget;

/**
 * Created by Александр on 24.02.2016.
 */
public class TimeInterval {

	private long previous;
	private long dt;
	private long duration;

	public void step() {
		if (previous == 0) {
			dt = 0;
			previous = System.currentTimeMillis();
		} else {
			long cur = System.currentTimeMillis();
			dt = cur - previous;
			previous = cur;
		}
		duration += dt;
	}

	public long dt() {
		return dt;
	}

	public long duration() {
		return duration;
	}

	public void reset() {
		previous = 0;
		duration = 0;
	}
}
