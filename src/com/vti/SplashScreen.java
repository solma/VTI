/**
 * Copyright 2011 Sol Ma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.vti;

import java.io.IOException;
import java.net.URI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.vti.adapters.DBAdapter;
import com.vti.adapters.DBAdapter.CTATrackerDatabaseHelper;
import com.vti.managers.AccountManager;
import com.vti.managers.TwitterManager;
import com.vti.utils.Log;

public class SplashScreen extends Activity {
	
	private static final String TAG = SplashScreen.class.getSimpleName() ;

	private Twitter twitter;
	private RequestToken requestToken;
	
	private ImageButton twitterButton;
	private AccountManager accMgr;
	
	/**
	 * Called when the activity is first created. Here we will setup oAuth
	 * related implementations.
	 * 
	 * */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.login);
					
			twitterButton = (ImageButton) findViewById(R.id.vti);
			createAuthorizationRequests(twitterButton);
			accMgr = new AccountManager(getApplicationContext());

			setAlarm();
			if (accMgr.isAccountEmpty()) {
				twitterButton.setVisibility(View.VISIBLE);
				Log.d(TAG,"not authorized yet");
				createAuthorizationRequests(twitterButton);
			} else {
				twitterButton.setVisibility(View.INVISIBLE);
				navigateToMain();
				Log.d(TAG,accMgr.getAuthTokens().toString());
			}
		} catch (Exception e) {
			//To ensure application does not crash
			Log.d(TAG, Log.stack2string(e));
			Log.e(SplashScreen.class.getSimpleName(),"Caught exception in SplashScreen.onCreate() "+e.getMessage());
		}
	}

	/**
	 * Set alarm manager for auto-refreshing twits
	 */
	private void setAlarm() {
		//FIXME - This is not the right place to set the alarm. We should have a broad cast receiver which listens for Phone Boot Event.
		// When the phone boots we should set the alarm manager.
		//FIXME - In the same broadcast receiver we should register our broadcast receiver to listen to battery levels and register another 
		// broadcast receiver there. That broadcast receiver will shut down or restart Alarm Manager depending on the battery condition.
		// These changes will be coming soon.......
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final Intent intent = new Intent(getApplicationContext(),AlarmReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
				100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		final TwitterManager feedManager = new TwitterManager(getApplicationContext());
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + (5000),
				feedManager.getTwitterFeedRefreshInterval(), pendingIntent);
	}

	/**
	 * Create authorization request and launch login url in the browser
	 * 
	 * @param button
	 */
	void createAuthorizationRequests(final ImageButton button) {
		// Set on click listener
		button.setOnClickListener(new OnClickListener() {
			public void onClick(final View view) {
				String authUrl = null;
				try {
					twitter = new TwitterFactory().getInstance();
					twitter.setOAuthConsumer(Constants.CONSUMER_KEY,Constants.CONSUMER_SECRET);
					
					requestToken=twitter.getOAuthRequestToken(Constants.CALLBACK_URL);
					System.err.println("twitterUrl=="+requestToken.getAuthorizationURL());
	                /**
	                 * @Documented
	                 * Warning:  getOAuthRequestToken method can only be invoked once before getting accessToken, otherwise 
					 * Twitter returns 401 error code;
	                 */
					//final URI twitterUrl = new URI(twitter.getOAuthRequestToken(Constants.CALLBACK_URL).getAuthorizationURL());
					final URI twitterUrl = new URI(requestToken.getAuthorizationURL());
					authUrl = twitterUrl.toString();
					Log.d(TAG, authUrl);
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)));
				} catch (final Exception e) {
					Log.d(TAG, Log.stack2string(e));
					Log.d(TAG ,	"Caught exception in createAuthorizationRequests"	+ e.getMessage());
				}
			}
		});
	}

	/**
	 * When we get callback from browser after authentication, get the twits and
	 * Launch ListActivity - SocialFeed to display these twits
	 */
	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		try {
			final SharedPreferences settings = getSharedPreferences(
					Constants.SETTING_PREFERENCE_FILE , 0);
			boolean tracker_table_ready = settings.getBoolean(Constants.CTA_TABLE, false);
			if(!tracker_table_ready){
				new BuildCTATables().doInBackground();
			}
			
			saveAccountInfo(intent);
			navigateToMain();
		} catch (final Exception e) {
			Log.d(TAG, Log.stack2string(e));
		}
	}

	/**
	 * GO to main window
	 */
	private void navigateToMain() {
		//TODO This is the switcher  
		final Intent navIntent = new Intent(getApplicationContext(),VTIMain.class);
		finish();
		startActivity(navIntent);
	}

	/**
	 * Get access token from verifier received in callback URL
	 * 
	 * @param intent
	 * @throws Exception
	 */
	private void saveAccountInfo(final Intent intent) throws Exception {

		Log.e(TAG, "Fetching access token ...");
		final Uri uri = intent.getData();
		String verifier=null;
		if ((uri != null) && uri.toString().startsWith(Constants.CALLBACK_URL)) {
			verifier = uri.getQueryParameter("oauth_verifier");
			Log.e(TAG, verifier);
			
			AccessToken accessToken = twitter.getOAuthAccessToken(
					requestToken, verifier);
	
			Log.e(TAG, "Access token: " + accessToken.getToken());
			Log.e(TAG, "Token secret: " + accessToken.getTokenSecret());
			//save access token and secret
			accMgr.saveAccount(accessToken.getToken(), accessToken.getTokenSecret());
			//save update frequency and voice notify preference
			accMgr.saveSettings(1, true);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG , "ON RESUME");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG , "ON PAUSE");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStart()
	 */

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG , "ON START");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.e(TAG , "ON DESTROY");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onStop()
	 */

	@Override
	protected void onStop() {
		super.onStop();
		Log.e(TAG , "ON STOP");
	}
	
	/**
	 * copy the CTA Tracker database
	 */
	private class BuildCTATables extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... params) {
			final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
			CTATrackerDatabaseHelper myDbHelper = dbAdapter.getCTATrackerDBHelper();
		    try {
		       	myDbHelper.createCTADatabase();
		       	myDbHelper.close();
		 	} catch (IOException ioe) {
		 		throw new Error("Unable to create database");
		 	}

			final SharedPreferences settings = getSharedPreferences(
					Constants.SETTING_PREFERENCE_FILE , 0);
			Editor editor = settings.edit();
			editor.putBoolean(Constants.CTA_TABLE, true);
			editor.commit();
			return null;
		}
	
	};

}