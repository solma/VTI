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
package com.vti.managers;

import java.util.ArrayList;
import java.util.List;

import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.vti.Constants;
import com.vti.model.Twit;

public class FeedManager {
	private static final String TAG = FeedManager.class.getSimpleName();
	Twitter twitter;
	Context context;
	AccountManager authMgr;
	

	/**
	 * 
	 */
	public FeedManager(final Context context) {
		this.context = context;
		authMgr = new AccountManager(context);
		if(!authMgr.isAccountEmpty())
			twitter=authMgr.getTwitterFactory().getInstance();
	}

	public AccountManager getOAuthMgr() {
		return authMgr;
	}

	/**
	 * 
	 * @return twitter feed refresh interval
	 */
	public long getTwitterFeedRefreshInterval() {
		long refreshInterval = Constants.ONE_MINUTE;
		final SharedPreferences settings = context.getSharedPreferences(
				Constants.SETTING_VALUES, 0);

		refreshInterval = settings.getLong(Constants.REFRESH_INTERVAL,
				Constants.ONE_MINUTE);

		return refreshInterval;
	}

	/**
	 * Set refresh interval value in shared preferences
	 * 
	 * @param interval
	 */
	public void setTwitterFeedRefreshInterval(final long interval) {
		final SharedPreferences settings = context.getSharedPreferences(
				Constants.SETTING_VALUES, Context.MODE_PRIVATE);
		final Editor editor = settings.edit();
		editor.putLong(Constants.REFRESH_INTERVAL, interval
				* Constants.ONE_MINUTE);
		editor.commit();
	}

	/**
	 * Get Twitter feed
	 * 
	 * @return List<Twits>
	 */
	public List<Twit> getSocialFeed() {
		List<Twit> twits = null;
		try {
			// User user = twitter.verifyCredentials();
			final List<Status> statues = twitter.getHomeTimeline();
			twits = new ArrayList<Twit>(statues.size());

			for (Status status : statues) {
				// only return tweets from VTI accounts
				if (status.getUser().getName().startsWith("vti_")) {
					Log.d(FeedManager.class.getSimpleName(), status.getUser()
							.getName() + "  " + status.getText());
					twits.add(new Twit(status.getId(), status.getUser()
							.getName(), status.getUser().getProfileImageURL()
							.toString(), status.getText()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return twits;

	}

	/**
	 * publish status to account vti_robot on twitter
	 * 
	 * @param tweet
	 */
	public void tweet(final String tweet, GeoLocation loc) {
		try {
			StatusUpdate status=new StatusUpdate(tweet);
			status.setLocation(loc);
			twitter.updateStatus(status);
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	/**
	 * follow accounts on twitter
	 * 
	 * @param accounts
	 */
	public void follow(final String accounts) {
		try {
			String[] ats = accounts.split(";");
			for (String s : ats)
				twitter.createFriendship(s.trim(), true);
		} catch (TwitterException e) {
			Log.e(TAG, "cannot follow the account, something wrong");
			e.printStackTrace();
		}
	}

	/**
	 * unfollow accounts on twitter
	 * 
	 * @param accounts
	 */
	public void unfollow(final String accounts) {
		try {
			String[] ats = accounts.split(";");
			for (String s : ats)
				twitter.destroyFriendship(s.trim());

		} catch (TwitterException e) {
			Log.e(TAG, "cannot unfollow the account, something wrong");
			e.printStackTrace();
		}
	}
}
