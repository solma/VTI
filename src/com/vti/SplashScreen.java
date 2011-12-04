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

import java.net.URI;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
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

import com.vti.services.managers.FeedManager;
import com.vti.services.managers.AccountManager;

public class SplashScreen extends Activity {
	
	private static final String TAG = SplashScreen.class.getSimpleName() ;

	private Twitter twitter=null;
	private RequestToken requestToken;
	
	private ImageButton twitterButton;
	private AccountManager authMgr = null;

		

	/**
	 * Called when the activity is first created. Here we will setup oAuth
	 * related implementations.
	 * 
	 * */

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.main);
			twitterButton = (ImageButton) findViewById(R.id.twitter);
			createAuthorizationRequests(twitterButton);
			
			
			authMgr = new AccountManager(getApplicationContext());
			//FIXME - This is not the right place to set the alarm. We should have a broad cast receiver which listens for Phone Boot Event.
			// When the phone boots we should set the alarm manager.
			//FIXME - In the same broadcast receiver we should register our broadcast receiver to listen to battery levels and register another 
			// broadcast receiver there. That broadcast receiver will shut down or restart Alarm Manager depending on the battery condition.
			// These changes will be coming soon.......
			//setAlarm();
						
			if (authMgr.isAuthTokenEmpty()) {
				twitterButton.setVisibility(View.VISIBLE);
				Log.d(TAG,"not authorized yet");
				createAuthorizationRequests(twitterButton);
			} else {
				twitterButton.setVisibility(View.INVISIBLE);
				navigateToSocialFeed();
				Log.d(TAG,authMgr.getAuthTokens().getAccessToken()+"  "+authMgr.getAuthTokens().getAccessSecret());
			}
		
		
		} catch (Exception ex) {
			//To ensure application does not crash
			ex.printStackTrace();
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
					twitter = new TwitterFactory().getInstance();
					twitter.setOAuthConsumer(Constants.CONSUMER_KEY,Constants.CONSUMER_SECRET);
					
					requestToken=twitter.getOAuthRequestToken(Constants.CALLBACK_URL);
					System.err.println("twitterUrl=="+requestToken.getAuthorizationURL());
	                /*
	                 * @Documented
	                 * Warning:  getOAuthRequestToken method can only be invoked once before getting accessToken, otherwise 
					 * Twitter returns 401 error code;
	                 */
						
					//final URI twitterUrl = new URI(twitter.getOAuthRequestToken(Constants.CALLBACK_URL).getAuthorizationURL());
					final URI twitterUrl = new URI(requestToken.getAuthorizationURL());
					authUrl = twitterUrl.toString();
					
					Log.d(TAG, authUrl);
								
					startActivity(new Intent(Intent.ACTION_VIEW, Uri
							.parse(authUrl)));
				} catch (final Exception e) {
					e.printStackTrace();
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
	 * Get access token from verifier received in callback URL
	 * 
	 * @param intent
	 * @throws Exception
	 */
	private void saveAccessToken(final Intent intent) throws Exception {

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
			
			authMgr.saveAuthTokens(accessToken.getToken(), accessToken.getTokenSecret());
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