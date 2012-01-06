/**
 * Copyright 2011 Sol Ma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.vti.managers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.vti.services.SocialServiceImpl;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

public class DrawableManager {
	private static final String TAG = DrawableManager.class.getSimpleName();

	private final Map<String, Drawable> drawableMap;

	public DrawableManager() {
		drawableMap = new HashMap<String, Drawable>();
	}

	private Drawable fetchDrawable(String urlString) {
		try {
			InputStream is = fetch(urlString);
			Drawable drawable = Drawable.createFromStream(
					new FlushedInputStream(is), "src");
			if (null != drawable) {
				drawableMap.put(urlString, drawable);
			}
			return drawable;
		} catch (Exception e) {
			Log.e(TAG, "*************   fetchDrawable() -> " + urlString
					+ " fetchDrawable failed", e);
			return null;
		}
	}

	public void fetchDrawableOnThread(final String urlString,
			final ImageView imageView) {
		if (drawableMap.containsKey(urlString)) {
			imageView.setImageDrawable(drawableMap.get(urlString));
		} else {
			Thread thread = new Thread() {
				@Override
				public void run() {
					final Drawable drawable = fetchDrawable(urlString);
					if (!drawableMap.containsKey(urlString)) {
						drawableMap.put(urlString, drawable);
					}
					if (null != drawable) {
						imageView.post(new Runnable() {
							@Override
							public void run() {
								imageView.setImageDrawable(drawable);
							}
						});
					}

				}
			};
			thread.start();
		}

	}

	private InputStream fetch(String urlString) throws MalformedURLException,
			IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet request = new HttpGet(urlString);
		HttpResponse response = httpClient.execute(request);
		return response.getEntity().getContent();
	}

}