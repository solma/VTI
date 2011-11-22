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
package com.social.model;

/**
 * @author rohit
 * 
 */
public class OAuthTokens {

	private String accessToken;
	private String accessSecret;

	/**
	 * @param accessToken
	 * @param accessSecret
	 */
	public OAuthTokens(String accessToken, String accessSecret) {
		super();
		this.accessToken = accessToken;
		this.accessSecret = accessSecret;
	}

	/**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}

	/**
	 * @return the accessSecret
	 */
	public String getAccessSecret() {
		return accessSecret;
	}

}
