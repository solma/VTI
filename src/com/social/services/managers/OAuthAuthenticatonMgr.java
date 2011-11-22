/**
 * Copyright 2011 Saurabh Gangarde & Rohit Ghatol (http://code.google.com/p/droidtwit/)
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
package com.social.services.managers;

import com.social.model.OAuthTokens;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * @author rohit
 * 
 */
public class OAuthAuthenticatonMgr {
	private static final String AUTHORIZATIONS = "OAuthAccessTokens";
	private static final String ACCESS_TOKEN = "AccessToken";
	private static final String TOKEN_SECRET = "TokenSecret";
	
	private Context context = null;
	

	public OAuthAuthenticatonMgr(Context context) {
		this.context = context;
	}

	/**
	 * 
	 * @return true if OAuth Authentication is required, false if application
	 *         already has required tokens
	 */
	public boolean isAuthenticationRequired() {
		final SharedPreferences settings = context.getSharedPreferences(
				AUTHORIZATIONS, 0);
		boolean result = true;
		String accessToken = settings.getString(ACCESS_TOKEN, null);
		String tokenSecret = settings.getString(TOKEN_SECRET, null);

		if (null != accessToken && null != tokenSecret) {
			result = false;
		}
		return result;
	}

	/**
	 * 
	 * @return OAuth OAuthTokens from shared preference, if tokens not found in shared preferences returns null
	 */
	public OAuthTokens getAuthTokens() {
		final SharedPreferences settings = context.getSharedPreferences(
				AUTHORIZATIONS, 0);

		String accessToken = settings.getString(ACCESS_TOKEN, null);
		String tokenSecret = settings.getString(TOKEN_SECRET, null);
		if (null != accessToken && null != tokenSecret) {
			return new OAuthTokens(accessToken, tokenSecret);
		} else {
			return null;
		}

	}
	
	/**
	 * Save the oAuth Token for future use
	 * @param accessToken
	 * @param tokenSecret
	 */
	public void saveAuthTokens(String accessToken, String tokenSecret){
		final SharedPreferences settings = context.getSharedPreferences(AUTHORIZATIONS,
				0);
		Editor editor = settings.edit();
		editor.putString(ACCESS_TOKEN, accessToken);
		editor.putString(TOKEN_SECRET, tokenSecret);
		editor.commit();
	}
}
