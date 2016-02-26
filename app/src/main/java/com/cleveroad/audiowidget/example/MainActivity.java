package com.cleveroad.audiowidget.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cleveroad.audiowidget.AudioWidget;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		AudioWidget audioWidget = new AudioWidget(this);
		audioWidget.show(0, 400);
		finish();
	}
}
