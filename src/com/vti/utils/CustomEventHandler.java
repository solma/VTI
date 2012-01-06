package com.vti.utils;

import android.view.View;
import android.view.View.OnLongClickListener;

public class CustomEventHandler implements OnLongClickListener {
	private int position;
	private CustomEventListener callback;
	private String text;

        // Pass in the callback (this'll be the activity) and the row position
	public CustomEventHandler(CustomEventListener callback, int pos, String text) {
		position = pos;
		this.callback = callback;
		this.text=text;
	}

	// The onLongClick method which has NO position information
	@Override
	public boolean onLongClick(View v) {
		return callback.onCustomLongClick(v, text);
	}
}
