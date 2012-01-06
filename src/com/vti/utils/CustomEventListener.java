package com.vti.utils;

import android.view.View;

public interface CustomEventListener {
	//public void OnCustomClick(View aView, int position);
	public boolean onCustomLongClick(View aView, String input);
    // Feel free to add other methods of use. OnCustomTouch for example :)
}
