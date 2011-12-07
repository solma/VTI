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

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.vti.Constants;

/**
 * @author rohit
 * 
 */
public class AccountManager {
	private static final String AUTHORIZATIONS = "OAuthAccessTokens";
	private static final String ACCESS_TOKEN = "AccessToken";
	private static final String TOKEN_SECRET = "TokenSecret";

	private TwitterFactory tf;

	private Context context;

	public AccountManager(Context ctxt) {
		this.context = ctxt;
		final SharedPreferences settings = context.getSharedPreferences(
				AUTHORIZATIONS, 0);
		String accessToken = settings.getString(ACCESS_TOKEN, null);
		String tokenSecret = settings.getString(TOKEN_SECRET, null);

		if (null != accessToken && null != tokenSecret) {
			tf = new TwitterFactory(new ConfigurationBuilder()
					.setDebugEnabled(true)
					.setOAuthConsumerKey(Constants.CONSUMER_KEY)
					.setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
					.setOAuthAccessToken(accessToken)
					.setOAuthAccessTokenSecret(tokenSecret).build());
		}
	}

	public TwitterFactory getTwitterFactory() {
		return tf;
	}

	/**
	 * 
	 * @return true if OAuth token is empty, i.e. not required yet, false if
	 *         application already has required tokens
	 */
	public boolean isAccountEmpty() {
		return tf == null ? true : false;
	}

	/**
	 * 
	 * @return OAuth OAuthTokens from save Accounts
	 */
	public AccessToken getAuthTokens() {
		if (tf != null) {
			Twitter twitter = tf.getInstance();
			try {
				return twitter.getOAuthAccessToken();
			} catch (TwitterException e) {
				e.printStackTrace();
				return null;
			}
		} else
			return null;
	}

	/**
	 * Save the oAuth Token for future use
	 * 
	 * @param accessToken
	 * @param tokenSecret
	 */
	public void saveAccount(String accessToken, String tokenSecret) {
		final SharedPreferences settings = context.getSharedPreferences(
				AUTHORIZATIONS, 0);
		Editor editor = settings.edit();
		editor.putString(ACCESS_TOKEN, accessToken);
		editor.putString(TOKEN_SECRET, tokenSecret);
		editor.commit();
	}


}
