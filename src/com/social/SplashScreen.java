/**
 * Copyright 2011 Saurabh Gangarde & Rohit Ghatol (http://code.google.com/p/droidtwit/)
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
package com.social;

import java.net.URI;

import winterwell.jtwitter.OAuthSignpostClient;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.social.services.managers.FeedManager;
import com.social.services.managers.OAuthAuthenticatonMgr;

public class SplashScreen extends Activity {

	public final static String CONSUMER_KEY = "UJxOUdtJm8p3wEOFatp1Q";
	public final static String CONSUMER_SECRET = "6wIgL90ZKeWPk7G1y0QfztkSm13NiD2Rk3v5Lf7XAg";
	//account name that creates this application
	public final static String CONSUMER_ACCOUNT = "Sol_Ma";
		
	private static final String TAG = SplashScreen.class.getSimpleName() ;
	//public static final String TWITTER_KEY = "QFgKeMtBipewO4IG0rCNvw";
	//public static final String TWITTER_SECRET = "OLUqNsO5oSRrv8MgjAHZ0zgHx60tHzfQ7P3dbzzZPoI";
	private static final String CALLBACK_URL = TAG +"://twitter";

	private OAuthSignpostClient client;
	private ImageButton twitterButton;
	private OAuthAuthenticatonMgr authMgr = null;

	private String verifier;
	private String[] accessTokenAndSecret;

	/**
	 * Called when the activity is first created. Here we will setup oAuth
	 * related implementations.
	 * 
	 * */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);

			//FIXME - This is not the right place to set the alarm. We should have a broad cast receiver which listens for Phone Boot Event.
			// When the phone boots we should set the alarm manager.
			//FIXME - In the same broadcast receiver we should register our broadcast receiver to listen to battery levels and register another 
			// broadcast receiver there. That broadcast receiver will shut down or restart Alarm Manager depending on the battery condition.
			// These changes will be coming soon.......

			setAlarm();
			setContentView(R.layout.main);
			authMgr = new OAuthAuthenticatonMgr(getApplicationContext());
			twitterButton = (ImageButton) findViewById(R.id.twitter);

			if (authMgr.isAuthenticationRequired()) {
				twitterButton.setVisibility(View.VISIBLE);
				createAuthorizationRequests(twitterButton);
			} else {
				twitterButton.setVisibility(View.INVISIBLE);
				navigateToSocialFeed();
			}
		} catch (Exception ex) {
			//To ensure application does not crash
			Log.e(SplashScreen.class.getSimpleName(),"Caught exception in SplashScreen.onCreate() "+ex.getMessage());
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
		final Intent intent = new Intent(getApplicationContext(),
				AlarmReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
				100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		final FeedManager feedManager = new FeedManager(getApplicationContext());
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
					authUrl = getAuthorizationUrl();

					startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(authUrl)));

				} catch (final Exception e) {
					Log.d(TAG ,
							"Caught exception in createAuthorizationRequests "
									+ e.getMessage());
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
			saveAccessToken(intent);
			navigateToSocialFeed();
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * GO to List View to load twits
	 */
	private void navigateToSocialFeed() {
		final Intent navIntent = new Intent(getApplicationContext(),
				SocialFeed.class);
		finish();
		startActivity(navIntent);
	}

	/**
	 * Get authorization url according social service selected
	 * 
	 * @param socialId
	 * @return
	 * @throws Exception
	 */
	private String getAuthorizationUrl() throws Exception {
		String authUrl = null;

		client = new OAuthSignpostClient(CONSUMER_KEY, CONSUMER_SECRET,
				CALLBACK_URL);

		final URI twitterUrl = client.authorizeUrl();
		authUrl = twitterUrl.toString();
		Log.e("Main", authUrl);

		return authUrl;
	}

	/**
	 * Get access token from verifier received in callback URL
	 * 
	 * @param intent
	 * @throws Exception
	 */
	private void saveAccessToken(final Intent intent) throws Exception {

		Log.e("OnResume", "Fetching access token ...");
		final Uri uri = intent.getData();
		if ((uri != null) && uri.toString().startsWith(CALLBACK_URL)) {
			verifier = uri.getQueryParameter("oauth_verifier");
			Log.e("OnResume", verifier);

			client.setAuthorizationCode(verifier);

			accessTokenAndSecret = client.getAccessToken();

			Log.e("NewIntent", "Access token: " + accessTokenAndSecret[0]);
			Log.e("NewIntent", "Token secret: " + accessTokenAndSecret[1]);

			authMgr.saveAuthTokens(accessTokenAndSecret[0],
					accessTokenAndSecret[1]);
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
}