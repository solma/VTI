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
import java.util.HashMap;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import twitter4j.GeoLocation;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.vti.Constants;
import com.vti.model.Twit;
import com.vti.utils.Log;

public class TwitterManager {
	private static final String TAG = TwitterManager.class.getSimpleName();
	Twitter twitter;
	Context context;
	AccountManager authMgr;
	

	/**
	 * 
	 */
	public TwitterManager(final Context context) {
		this.context = context;
		authMgr = new AccountManager(context);
		if(!authMgr.isAccountEmpty())
			twitter=authMgr.getTwitterFactory().getInstance();
	}

	public AccountManager getOAuthMgr() {
		return authMgr;
	}
	
	public Twitter getTwitter(){
		return twitter;
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
	 * Get homeline of the user
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
				if (status.getUser().getName().toLowerCase().startsWith("vti_")) {
					//Log.d(TwitterManager.class.getSimpleName(), status.getUser().getName() + "  " + status.getText());
					twits.add(new Twit(status.getId(),  status.getCreatedAt().getTime(), status.getUser()
							.getName(), status.getUser().getProfileImageURL()
							.toString(), status.getText()));
				}
			}
		} catch (Exception e) {
			Log.d(TAG, Log.stack2string(e));
		}
		return twits;

	}
	
	/**
	 * Get timeline of  a specified user
	 * 
	 * @return List<Twits>
	 */
	public List<Twit> getUserTimeline(String userName) {
		List<Twit> twits = null;
		try {
			// User user = twitter.verifyCredentials();
			final List<Status> statues = twitter.getUserTimeline(userName);
			twits = new ArrayList<Twit>(statues.size());
			for (Status status : statues) {
					Log.e(TAG, status.getText());
					twits.add(new Twit(status.getId(), status.getCreatedAt().getTime(), status.getUser()
							.getName(), status.getUser().getProfileImageURL()
							.toString(), status.getText()));
			}
		} catch (Exception e) {
			Log.d(TAG, Log.stack2string(e));
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
			Log.d(TAG, Log.stack2string(e));
		}
	}

	/**
	 * follow accounts on twitter
	 * 
	 * @param accounts
	 */
	public void follow(final String[] accounts) {
		if(accounts!=null)
			for (int i = 0; i < accounts.length; i++) {
				try {
					twitter.createFriendship(accounts[i].trim(), true);
				} catch (TwitterException e) {
					Log.e(TAG, "cannot follow the account: " + accounts[i]);
					//e.printStackTrace();
					continue;
				}
			}
	}

	/**
	 * unfollow accounts on twitter
	 * 
	 * @param 
	 */
	public void unfollow(final String[] accounts) {
		if(accounts!=null)
			for (int i = 0; i < accounts.length; i++) {
				try {
					Log.e(TAG,accounts[i]);
					twitter.destroyFriendship(accounts[i].trim());
				} catch (TwitterException e) {
					Log.e(TAG, "cannot unfollow the account: " + accounts[i]);
					//e.printStackTrace();
					continue;
				}
			}
	}
	
	/**
	 * return all current following accounts
	 * 
	 * @param accounts
	 */
	public String[] getFriends(){
		try {
			ArrayList<String> vti_accounts=new ArrayList<String>();
			long[] ids=twitter.getFriendsIDs(-1).getIDs();
			ResponseList<User> users=twitter.lookupUsers(ids);
			String name;
			if(users!=null&&users.size()>0){
				for (int i = 0; i < users.size(); i++){
					name = users.get(i).getScreenName();
					if(name.toLowerCase().startsWith("vti_")){
						vti_accounts.add(name);
					}
				}
				return vti_accounts.toArray(new String[vti_accounts.size()]);
			}
		} catch (TwitterException e) {
			Log.e(TAG, "cannot get Frindliest");
			Log.d(TAG, Log.stack2string(e));
		}
		return null;
	}
	
	/**
	 * return all existing VTI accounts
	 * 
	 * @param accounts
	 */
	public HashMap<String, String> getAllVTIAccounts(){
		HashMap<String,String> ret=new HashMap<String,String>();
		try {
			Document doc;
			int i,j;
			doc = Jsoup.connect(Constants.ACCOUNTS_LIST).get();
			org.jsoup.select.Elements accounts = doc.select("account");
			StringBuilder details=new StringBuilder();
			for(i=0;i<accounts.size();i++) {
				details.delete(0, details.length());
				org.jsoup.select.Elements childrenEles=accounts.get(i).children();
				for(j=0;j<childrenEles.size();j++){
					String tagName=childrenEles.get(j).tagName();
					if(tagName.equals("southwest")||tagName.equals("northeast")){
						String coords=childrenEles.get(j).select("lat").text()+" , "+childrenEles.get(j).select("lng").text();
						details.append(tagName+" : "+coords+"\n");
					}
					else
						details.append(tagName+" : "+childrenEles.get(j).text()+"\n");
				}
				ret.put(childrenEles.get(0).text(), details.toString());
			}
		} catch (Exception e) {
			Log.e(TAG, "cannot parse the accounts list file");
			Log.d(TAG, Log.stack2string(e));
		}
		return ret;
	}
	
	/**
	 * return all VTI accounts that are not currently followed
	 * 
	 * @param accounts
	 */
	public HashMap<String, String> getUnfollowedVTIAccounts(){
		HashMap<String, String> allAccounts=getAllVTIAccounts();
		String [] friends=getFriends();
		if(allAccounts.size()==0)
			return null;
		if(friends==null||friends.length<1)
			return allAccounts;
		else{
			for(int i=0;i<friends.length;i++)
				allAccounts.remove(friends[i]);
			return allAccounts;
		}
	} 
}
