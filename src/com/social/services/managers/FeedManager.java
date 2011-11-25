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
package com.social.services.managers;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.social.Setting;
import com.social.SplashScreen;
import com.social.model.OAuthTokens;
import com.social.model.Twit;

/**
 * @author Sol
 * 
 */
public class FeedManager {

	Context context = null;

	/**
	 * 
	 */
	public FeedManager(final Context context) {
		this.context = context;
	}

	/**
	 * 
	 * @return twitter feed refresh interval
	 */
	public long getTwitterFeedRefreshInterval() {
		long refreshInterval = Setting.ONE_MINUTE;
		final SharedPreferences settings = context.getSharedPreferences(
				Setting.SETTING_VALUES, 0);

		refreshInterval = settings.getLong(Setting.REFRESH_INTERVAL,
				Setting.ONE_MINUTE);

		return refreshInterval;
	}

	/**
	 * Set refresh interval value in shared preferences
	 * 
	 * @param interval
	 */
	public void setTwitterFeedRefreshInterval(final long interval) {
		final SharedPreferences settings = context.getSharedPreferences(
				Setting.SETTING_VALUES, Context.MODE_PRIVATE);
		final Editor editor = settings.edit();
		editor.putLong(Setting.REFRESH_INTERVAL, interval * Setting.ONE_MINUTE);
		editor.commit();
	}

	/**
	 * Get Twitter feed
	 * 
	 * @param tokens
	 * @return
	 */
	public List<Twit> getSocialFeed(final OAuthTokens tokens) {
		/*
		  final OAuthSignpostClient client = new
		  OAuthSignpostClient(SplashScreen.CONSUMER_KEY,
		  SplashScreen.CONSUMER_SECRET, tokens.getAccessToken(),
		  tokens.getAccessSecret()); 
		 final Twitter twitter = new
		  Twitter(SplashScreen.CONSUMER_ACCOUNT, client); 
		  final List<Twitter.Status> statues = twitter.getHomeTimeline();
		 */
		List<Twit> twits = null;
		try {
			TwitterFactory tf = new TwitterFactory(new ConfigurationBuilder()
					.setDebugEnabled(true)
					.setOAuthConsumerKey(SplashScreen.CONSUMER_KEY)
					.setOAuthConsumerSecret(SplashScreen.CONSUMER_SECRET)
					.setOAuthAccessToken(tokens.getAccessToken())
					.setOAuthAccessTokenSecret(tokens.getAccessSecret())
					.build());

			Twitter twitter = tf.getInstance();
			User user = twitter.verifyCredentials();
			final List<Status> statues = twitter.getHomeTimeline();
			twits = new ArrayList<Twit>(statues.size());

			for (Status status : statues){
				// twits.add(new Twit(status.getId().longValue(),
				// user.getName(), user.getProfileImageUrl().toString(),
				// twitText));
				Log.d(FeedManager.class.getSimpleName(),status.getUser().getName()+"  "+status.getText());
				twits.add(new Twit(status.getId(), status.getUser().getName(), status.getUser().
						getProfileImageURL().toString(), status.getText()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return twits;

	}

	/**
	 * set status on twitter
	 * 
	 * @param tweet
	 * @param tokens
	 */
	public void tweet(final String tweet, final OAuthTokens tokens) {
		TwitterFactory tf = new TwitterFactory(new ConfigurationBuilder()
		.setDebugEnabled(true)
		.setOAuthConsumerKey(SplashScreen.CONSUMER_KEY)
		.setOAuthConsumerSecret(SplashScreen.CONSUMER_SECRET)
		.setOAuthAccessToken(tokens.getAccessToken())
		.setOAuthAccessTokenSecret(tokens.getAccessSecret())
		.build());

		Twitter twitter = tf.getInstance();
		try {
			twitter.updateStatus(tweet);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}
}
