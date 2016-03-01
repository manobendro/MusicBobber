package com.cleveroad.audiowidget.example;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cleveroad.audiowidget.AudioWidget;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final AudioWidget audioWidget = new AudioWidget(this);
		audioWidget.show(0, 400);
		int duration = 5_000;
		ValueAnimator animator = ValueAnimator.ofInt(0, duration).setDuration(duration);
		audioWidget.setDuration(duration);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				audioWidget.setPosition((Integer) animation.getAnimatedValue());
			}
		});
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.start();
	}
}