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
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import twitter4j.GeoLocation;
import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.vti.adapters.TwitAdapter;
import com.vti.managers.AccountManager;
import com.vti.managers.LocManager;
import com.vti.managers.TwitterManager;
import com.vti.model.Twit;
import com.vti.services.ISocialService;
import com.vti.services.SocialServiceImpl;
import com.vti.utils.CustomEventListener;
import com.vti.utils.GeoCoder;
import com.vti.utils.Log;

public class SocialFeed extends ListActivity implements CustomEventListener, TextToSpeech.OnInitListener{
	private static final String TAG = SocialFeed.class.getSimpleName();
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	private static final String UPDATE_FREQUENCY_INPUT_ERROR="Update frequncy has to be an integer.";
	
	private TextToSpeech mTts;
	private AccountManager authMgr;
	private ISocialService socialService;
	private ImageButton refreshButton;
	private ImageButton publishButton;
	private ImageButton followUnfollowButton;
	private ImageButton followButton;
	private ImageButton unfollowButton;
	private ImageButton routeButton;
		
	private ServiceConnection connection = new ServiceConnection() {
		// Called when the connection with the service is established
		public void onServiceConnected(final ComponentName className,
				final IBinder service) {
			Log.e(TAG, className+"  "+service);
			socialService = ISocialService.Stub.asInterface(service);
			// As soon as Service connection is established load data from DB
			FetchFromDBTask fetchFromDBTask = new FetchFromDBTask();
			fetchFromDBTask.execute(authMgr.getAuthTokens());
			// Also register onclick listener for refresh button, its now safe to do so
			refreshButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					new FetchFromServer().execute(authMgr.getAuthTokens());
					Log.e(TAG, "WITHIN REFRESH CLICK");
				}
			});
		}
		// Called when the connection with the service disconnects unexpectedly
		public void onServiceDisconnected(final ComponentName className) {
			socialService = null;
		}
	};

	private class FetchFromDBTask extends
			AsyncTask<AccessToken, Void, List<Twit>> {
		private ProgressDialog dialog = new ProgressDialog(SocialFeed.this);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Loading notifications......");
			dialog.show();
		}

		@Override
		protected List<Twit> doInBackground(final AccessToken... params) {
			List<Twit> result = null;
			if (null != socialService) {
				try {
					result = socialService.getFromDB();
					// Nothing found in database so do a force fetch from Internet
					if ((null == result) || (result.size() == 0)) {
						result = socialService.getFromServer();
					}
				} catch (final RemoteException e) {
					Log.e(TAG, "Got Exception in getSocialFeed() " + e.getMessage());
				}
			}else
				Log.e(TAG, "socialService is empty");
			return result;
		}

		@Override
		protected void onPostExecute(final List<Twit> result) {

			super.onPostExecute(result);
			if (null != result) {
				final TwitAdapter adapter = new TwitAdapter(getApplicationContext(), result, SocialFeed.this);
				Log.d(TAG, "Get twit results from Database");
				setListAdapter(adapter);
			}
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}

	};

	private class FetchFromServer extends AsyncTask<AccessToken, Void, List<Twit>> {
		private ProgressDialog dialog = new ProgressDialog(SocialFeed.this);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Refreshing Notifications......");
			dialog.show();
		}

		@Override
		protected List<Twit> doInBackground(final AccessToken... params) {
			List<Twit> result = null;
			if (null != socialService) {
				try {
					result = socialService.getFromServer();
				} catch (final RemoteException e) {
					Log.d(TAG, Log.stack2string(e));
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(final List<Twit> result) {
			super.onPostExecute(result);
			if (null != result) {
				final TwitAdapter adapter = new TwitAdapter(getApplicationContext(), result, SocialFeed.this);
				Log.e(TAG, "Get notifications results from server");
				setListAdapter(adapter);

			}
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}
	};
	
	private class FetechFromEncounteredAccount extends AsyncTask<String, Void, List<Twit>> {
		@Override
		protected List<Twit> doInBackground(final String... params) {
			List<Twit> result = null;
			if (null != socialService) {
				try {
					result = socialService.getFromEncounteredAccount(params[0]);
				} catch (final RemoteException e) {
					Log.d(TAG, Log.stack2string(e));
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(final List<Twit> result) {
			super.onPostExecute(result);
			if (null != result) {
				final TwitAdapter adapter = new TwitAdapter(getApplicationContext(), result, SocialFeed.this);
				Log.e(TAG, "Get publications from encountered account");
				setListAdapter(adapter);
			}
		}
	};

	private class TweetAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... params) {
			final TwitterManager twitterMgr = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = twitterMgr.getOAuthMgr();
			final LocManager locMgr=new LocManager(getApplicationContext());
			Location loc=locMgr.getLatestLocation();
			if (!authMgr.isAccountEmpty()) {
				twitterMgr.tweet(params[0], new GeoLocation(loc.getLatitude(),loc.getLongitude()));
			}
			return null;
		}
		/*
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			refreshButton.performClick();
		}
	};
	
	private class FollowAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... params) {
			final TwitterManager twitterMgr = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = twitterMgr.getOAuthMgr();
			if (!authMgr.isAccountEmpty()) {
				Log.e(TAG, params[0]);
				params[0]=params[0].replaceAll(" ","").replaceAll("\n", "");
				Log.e(TAG, params[0]);
				if(params[0].contains(";")){ //multiple accounts
					String[] accounts=params[0].split(";");
					twitterMgr.follow(accounts);
				}
				else
					twitterMgr.follow(new String[]{params[0]});
			}
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			refreshButton.performClick();
		}
	};
	
	private class UnFollowAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... params) {
			final TwitterManager twitterMgr = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = twitterMgr.getOAuthMgr();
			if (!authMgr.isAccountEmpty()) {
				Log.e(TAG, params[0]);
				params[0]=params[0].replaceAll(" ","").replaceAll("\n", "");
				Log.e(TAG, params[0]);
				if(params[0].contains(";")){ //multiple accounts
					String[] accounts=params[0].split(";");
					twitterMgr.unfollow(accounts);
				}
				else
					twitterMgr.unfollow(new String[]{params[0]});
			}
			return null;
		}

		@Override
		protected void onPostExecute(final Void result) {
			super.onPostExecute(result);
			//refreshButton.performClick();
		}
	};


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.feed_list);
	        // Initialize text-to-speech. This is an asynchronous operation.
	        // The OnInitListener (second argument) is called after initialization completes.
	        mTts = new TextToSpeech(this,
	        		this  // TextToSpeech.OnInitListener
	            );
			
			refreshButton = (ImageButton) findViewById(R.id.force_refresh);
			publishButton = (ImageButton)findViewById(R.id.publish);
			followUnfollowButton = (ImageButton)findViewById(R.id.follow_unfollow);
			followButton = (ImageButton)findViewById(R.id.follow);
			unfollowButton = (ImageButton)findViewById(R.id.unfollow);
			routeButton =(ImageButton)findViewById(R.id.route);
			
			publishButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					// retrieve the tweets of the encountered account first
					retrieveEncounteredAccount();
					handlePublish(null);
				}
			});
			
			followUnfollowButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handleFollowUnfollow();
				}
			});
			
			followButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handleFollow();
				}
			});
			
			unfollowButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handleUnfollow();
				}
			});
			
			routeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handleRoute();
				}
			});
			
			authMgr = new AccountManager(getApplicationContext());
	
			if (null != authMgr.getTwitterFactory()) {
				bindService(new Intent(getApplicationContext(),
						SocialServiceImpl.class), connection,
						Context.BIND_AUTO_CREATE);
			}

		} catch (Exception ex) {
				Log.e(TAG,	"Got exception in SocialFeed.onCreate() " + ex.getMessage());
		}
	}

	/**
	 * Handle list item click
	 */
	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		super.onListItemClick(l, v, position, id);
	}

	/**
	 * Create options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	/**
	 * Handle menu items
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			handleSettings();
			return true;
		case R.id.feedback:
			handleFeedback();
			return true;
		case R.id.help:
			handleHelp();
			return true;
		case R.id.about_us:
			handleAboutUs();
			return true;
		default:
			return false;
		}
	}
	
	private void retrieveEncounteredAccount(){
		final LocManager locMgr=new LocManager(getApplicationContext());
		Location loc=locMgr.getLatestLocation();
		if (!authMgr.isAccountEmpty()) {
			String accountName=GeoCoder.determineVTIAccount(
					new GeoPoint((int)(loc.getLatitude()*1.0E6),(int)(loc.getLongitude()*1.0E6)));
			if(accountName!=null)
				new  FetechFromEncounteredAccount().execute(accountName);
		}
	}

	/**
	 * handle follow/unfollow click
	 */
	private void handleFollowUnfollow() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.follow_unfollow);
		dialog.setContentView(R.layout.follow_unfollow);

		final Button followButton = (Button) dialog.findViewById(R.id.follow);
		final Button unfollowButton = (Button) dialog.findViewById(R.id.unfollow);
		final EditText edittext = (EditText) dialog
				.findViewById(R.id.tweet_text);
		
		if(!isOnline()){
			Toast.makeText(getApplicationContext(), Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}

		followButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				new FollowAsyncTask().execute(edittext.getText().toString());
			}
		});
		
		unfollowButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				new UnFollowAsyncTask().execute(edittext.getText().toString());
			}
		});
		
		dialog.show();
	}
	/**
	 * handle follow click 
	 */
	private void handleFollow(){
		if(!isOnline()){
			Toast.makeText(getApplicationContext(), Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}
		final TwitterManager twitterMgr = new TwitterManager(this);
		final HashMap<String, String> followableAccounts=twitterMgr.getUnfollowedVTIAccounts();
		if(followableAccounts==null){
			Toast.makeText(getApplicationContext(), Constants.ACCOUNT_LIST_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}
		
		List<String> accountList=new ArrayList<String>(followableAccounts.keySet());
		Collections.sort(accountList);
		final CharSequence[] items= accountList.toArray(new CharSequence[1]);
		final ArrayList<Boolean> selected=new ArrayList<Boolean>();
		for(int i=0;i<items.length;i++)
			selected.add(false);
		final StringBuilder followList=new StringBuilder();
		AlertDialog select=new AlertDialog.Builder(SocialFeed.this)
        .setTitle("Please select VTI accounts to be followed")
        .setMultiChoiceItems(items, null, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int position, boolean checked) {
				if(checked){ //the selection check the item
					selected.set(position, true);
					showAccountDetail(followableAccounts.get(items[position]));
				}
				else // the selection uncheck the item;
					selected.set(position, false);
			}
        })
        .setPositiveButton("Follow", new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface dialog, int whichButton) {
        		for(int i=0;i<selected.size();i++)
        			if(selected.get(i)){
        				if(followList.length()>0)
        					followList.append(";");
        				followList.append(items[i]);
        			}
        		Log.e(TAG,followList.toString());
         		new FollowAsyncTask().execute(followList.toString());
             }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();
		select.show();
	}
	
	private void showAccountDetail(String detail) {
		final Dialog dialog=new Dialog(SocialFeed.this);
		dialog.setTitle("Account Details");
		dialog.setContentView(R.layout.account_detail);
		TextView detailBox=(TextView)dialog.findViewById(R.id.accountDetail);
		detailBox.setText(detail);
		Button backButton=(Button)dialog.findViewById(R.id.back);
		backButton.setOnClickListener(new OnClickListener(){
			public void onClick(final View v){
				dialog.cancel();
			}
		});
		dialog.show();
	} 
	
	/**
	 * handle unfollow click
	 */
	private void handleUnfollow() {
		if(!isOnline()){
			Toast.makeText(getApplicationContext(), Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}
		final TwitterManager twitterMgr = new TwitterManager(this);
		final CharSequence[] items=  twitterMgr.getFriends();
		final StringBuilder unfollowList=new StringBuilder();
		if(items==null){
			Toast.makeText(this, "you do not follow any VTI accounts at this moment", Toast.LENGTH_SHORT).show();
			return;
		}
		
		final ArrayList<Boolean> selected=new ArrayList<Boolean>();
		for(int i=0;i<items.length;i++)
			selected.add(false);
		AlertDialog select=new AlertDialog.Builder(SocialFeed.this)
        .setTitle("Please select VTI accounts to be unfollowed")
        .setMultiChoiceItems(items, null, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface arg0, int position, boolean checked) {
				if(checked){ //the selection check the item
					selected.set(position, true);
				}
				else // the selection uncheck the item;
					selected.set(position, false);
			}
        })
        .setPositiveButton("Unfollow", new DialogInterface.OnClickListener() {
        	@Override
            public void onClick(DialogInterface dialog, int whichButton) {
        		for(int i=0;i<selected.size();i++)
        			if(selected.get(i)){
        				if(unfollowList.length()>0)
        					unfollowList.append(";");
        				unfollowList.append(items[i]);
        		}
        		Log.e(TAG,unfollowList.toString());
				new UnFollowAsyncTask().execute(unfollowList.toString());
             }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();
		select.show();
	}

	/**
	 * handle about us click
	 */
	private void handleAboutUs() {
		final Intent navIntent = new Intent(getApplicationContext(),AboutUs.class);
		startActivity(navIntent);
	}
	
	/**
	 * handle help click
	 */
	private void handleHelp() {
		final Intent navIntent = new Intent(getApplicationContext(),Help.class);
		startActivity(navIntent);
	}
	
	/**
	 * handle route click
	 */
	private void handleRoute(){
		final Intent navIntent = new Intent(getApplicationContext(), RouteSubscription.class);
		startActivity(navIntent);
	}

	/**
	 * handle tweet click
	 */
	private void handlePublish(String input) {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.publish);
		dialog.setContentView(R.layout.publish);

		final Button publishButton = (Button) dialog.findViewById(R.id.publish);
		final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
		final EditText tweetText = (EditText) dialog.findViewById(R.id.publish_text);
		//final ImageButton speakButton= (ImageButton) dialog.findViewById(R.id.speak);
		if(!isOnline()){
			Toast.makeText(getApplicationContext(), Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}
		
		if(input!=null){
			//remove location information
			int idx=input.lastIndexOf(" reported from ");
			if(idx>=0)
				input=input.substring(0,idx);
			tweetText.setText(input);
		}

		publishButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				TweetAsyncTask tweetAsyncTask = new TweetAsyncTask();
				//always add "@VTI " to the beginning of the tweet
				tweetAsyncTask.execute("@vti_robot "+tweetText.getText().toString());
			}
		});
		
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
			}
		});
		/*
		speakButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
		        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
		                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
		        //TODO: fix me, how to start a speech input
		        //startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
		   	}
		});
		 */
		dialog.show();
	}
	
	/**
	 * handle feedback click
	 */
	private void handleFeedback() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle("Thansk for your feedback.");
		dialog.setContentView(R.layout.feed_back);

		final Twitter twitter=new TwitterManager(getApplicationContext()).getTwitter();
		final Button feedbackButton = (Button) dialog.findViewById(R.id.sendFeedback);
		final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
		final EditText feedbackText = (EditText) dialog.findViewById(R.id.feedback_text);
		
		feedbackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				InetAddress addr = null;
				SocketAddress sockaddr = null;
				PrintWriter out = null;
				Socket clientSocket = new Socket();
				try {
					addr = InetAddress.getByName(Constants.SERVER_IP);
					sockaddr = new InetSocketAddress(addr,
							Constants.SERVER_PORT);
				} catch (UnknownHostException e) {
					Log.e(TAG, "Unknow Host Exception: cannot resolve "+ Constants.SERVER_IP);
					return;
				}
				// set connection time out
				try {
					clientSocket.connect(sockaddr, Constants.THREE_SECONDS);
				} catch (IOException e) {
					Log.e(TAG, "Time out when connect to server");
					Toast.makeText(getApplicationContext(),"Cannot connect to the server.", Toast.LENGTH_SHORT).show();
					return;
				}
				try {
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					out.println("Feedback");
					if(twitter!=null)
						try {
							out.println(twitter.getScreenName());
						} catch (Exception e) {
							out.println("anonymous");
							Log.d(TAG, Log.stack2string(e));
						}
					else
						out.println("anonymous");
					out.println(feedbackText.getText().toString());
					out.close();
					clientSocket.close();
					Toast.makeText(getApplicationContext(),"Successfully sent feedback.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					Log.d(TAG, Log.stack2string(e));
				}
			}
		});
		
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
			}
		});
		
		dialog.show();
	}
	

	/**
     * Handle the results from the speech recognition activity.
     */
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //TODO: how to show the matched result back to a edittext
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	

	/**
	 * Handle Setting click
	 */
	private void handleSettings() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.settings);
		dialog.setContentView(R.layout.settings);
		
		final SharedPreferences settings = getSharedPreferences(
				Constants.SETTING_PREFERENCE_FILE, 0);
		final Editor editor=settings.edit();
		
		final Button applyButton = (Button) dialog
				.findViewById(R.id.apply_button);
		final Button cancelButton = (Button) dialog
				.findViewById(R.id.cancel_button);
		final CheckBox removeAccountCheck = (CheckBox) dialog
				.findViewById(R.id.remove_account_check);
		final EditText frequencyEditText = (EditText) dialog
				.findViewById(R.id.refresh_rates);
		frequencyEditText.setText(String.valueOf(settings.getInt(Constants.UPDATE_FREQUENCY, 1)));
		
		final ToggleButton voiceNotifyButton=(ToggleButton)dialog
				.findViewById(R.id.voice_notify);
		voiceNotifyButton.setChecked(settings.getBoolean(Constants.VOICE_NOTIFY, true));

		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (removeAccountCheck.isChecked()) {
					resetAccount();
				}
				if (!removeAccountCheck.isChecked()) {
					String input=frequencyEditText.getText().toString();
					int interval=1;
					try{
						interval = Integer.valueOf(input);
						editor.putInt(Constants.UPDATE_FREQUENCY, Integer.valueOf(input));
					}catch(NumberFormatException ex){
						Toast.makeText(getApplicationContext(), UPDATE_FREQUENCY_INPUT_ERROR, Toast.LENGTH_SHORT).show();
					}
					updateFeedRefreshInterval(interval);
				}
				if (voiceNotifyButton.isChecked())
					editor.putBoolean(Constants.VOICE_NOTIFY, true);
				else
					editor.putBoolean(Constants.VOICE_NOTIFY, false);
				editor.commit();
				dialog.cancel();
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
			}
		});

		dialog.show();
	}

	/**
	 * Update refresh interval in SharedPreferences and update AlarmManager to
	 * update interval
	 * 
	 * @param interval
	 */
	private void updateFeedRefreshInterval(final int interval) {
		final TwitterManager twitterMgr = new TwitterManager(getApplicationContext());
		twitterMgr.setTwitterFeedRefreshInterval(interval);

		// Update alarm manager in current running application instance
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final Intent intent = new Intent(getApplicationContext(),AlarmReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
				100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmManager.cancel(pendingIntent);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + 1000, interval
						* Constants.ONE_MINUTE, pendingIntent);
	}
	
	/**
	 * Remove account and access tokens associated with it
	 */
	private void resetAccount() {
		authMgr.saveAccount(null, null);
		finish();
		final Intent navIntent = new Intent(getApplicationContext(),
				SplashScreen.class);
		startActivity(navIntent);
	}

	private boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "ON START");

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "ON PAUSE");
	}

	/**
	 * Register Broadcast receiver to update twit feeds when SocialService sends
	 * a broadcast of new twits available
	 * */
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "ON START");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "ON NEW INTENT");
		super.onNewIntent(intent);
		// If this Resume is happening because of clicking on Notification than refresh from database
		if (null != intent && null != intent.getExtras()
				&& intent.getExtras().containsKey("Refresh")
				&& intent.getExtras().getBoolean("Refresh")) {
			Log.d(TAG, "***** Refresh as I am coming from notification");
			new FetchFromDBTask().execute(authMgr.getAuthTokens());
		}
		NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	@Override
	protected void onStop() {
		super.onStop();
		/**
		 * press HOME key is to stop(), press back key is to destroy();
		 * socialService cannot be nullified here!!!!
		 */
     	//socialService = null;
		Log.d(TAG, "ON STOP");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	     // Don't forget to shutdown mTts!
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }

		Log.d(TAG, "ON DESTROY");
		// Other wise we will leak a connection
		try {
			unbindService(connection);
		} catch (Exception ex) {
		}
	}
	
	@Override
	public boolean onCustomLongClick(View row, String input) {
		handlePublish(input);
		return false;
	}

	@Override
	public void onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mTts.setLanguage(Locale.US);
            // Try this someday for some interesting results.
            // int result mTts.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
               // Lanuage data is missing or the language is not supported.
                Log.e(TAG, "Language is not available.");
            } 
        } else {
            // Initialization failed.
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
	}
	
}
