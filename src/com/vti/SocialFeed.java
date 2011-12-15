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
import java.util.List;

import twitter4j.GeoLocation;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.RecognizerIntent;
import android.util.Log;
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
import android.widget.Toast;

import com.vti.adapters.TwitAdapter;
import com.vti.managers.AccountManager;
import com.vti.managers.LocManager;
import com.vti.managers.TwitterManager;
import com.vti.model.Twit;
import com.vti.services.ISocialService;
import com.vti.services.SocialServiceImpl;

public class SocialFeed extends ListActivity {
	private static final String TAG = SocialFeed.class.getSimpleName();
	
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
	
	private AccountManager authMgr;
	private ISocialService socialService;
	private ImageButton refreshButton;
	private Button publishButton;
	private Button followButton;
	private Button routeButton;

	private class FetchFromDBTask extends
			AsyncTask<AccessToken, Void, List<Twit>> {
		private ProgressDialog dialog = new ProgressDialog(SocialFeed.this);
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Loading twits......");
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
			}
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(final List<Twit> result) {

			super.onPostExecute(result);
			if (null != result) {
				final TwitAdapter adapter = new TwitAdapter(
						getApplicationContext(), result);
				Log.d(TAG, "Get twit results from Database");
				setListAdapter(adapter);
			}
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}

	};

	private class FetchFromServer extends
			AsyncTask<AccessToken, Void, List<Twit>> {
		private ProgressDialog dialog = new ProgressDialog(SocialFeed.this);

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage("Refreshing twits......");
			dialog.show();
		}

		@Override
		protected List<Twit> doInBackground(final AccessToken... params) {
			List<Twit> result = null;
			if (null != socialService) {
				try {
					result = socialService.getFromServer();
				} catch (final RemoteException e) {
					e.printStackTrace();
				}
			}

			return result;

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(final List<Twit> result) {
			super.onPostExecute(result);
			if (null != result) {
				final TwitAdapter adapter = new TwitAdapter(
						getApplicationContext(), result);
				Log.d(TAG, "Get twit results from Database");
				setListAdapter(adapter);

			}
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
		}

	};

	private class TweetAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... params) {
			final TwitterManager feedManager = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = feedManager.getOAuthMgr();
			final LocManager locMgr=new LocManager(getApplicationContext());
			Location loc=locMgr.getLatestLocation();
			//TODO: check if the location is embeded
			if (!authMgr.isAccountEmpty()) {
				feedManager.tweet(params[0], new GeoLocation(loc.getLatitude(),loc.getLongitude()));
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
			final TwitterManager feedManager = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = feedManager.getOAuthMgr();
			if (!authMgr.isAccountEmpty()) {
				feedManager.follow(params[0]);
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
	
	private class UnFollowAsyncTask extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(final String... params) {
			final TwitterManager feedManager = new TwitterManager(
					getApplicationContext());
			final AccountManager authMgr = feedManager.getOAuthMgr();
			if (!authMgr.isAccountEmpty()) {
				feedManager.unfollow(params[0]);
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

	private ServiceConnection connection = new ServiceConnection() {
		// Called when the connection with the service is established
		public void onServiceConnected(final ComponentName className,
				final IBinder service) {
			socialService = ISocialService.Stub.asInterface(service);
			// As soon as Service connection is established load data from DB
			FetchFromDBTask fetchFromDBTask = new FetchFromDBTask();
			fetchFromDBTask.execute(authMgr.getAuthTokens());
			// Also register onclick listener for refresh button, its now safe to do so
			refreshButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					FetchFromServer fetchFromServer = new FetchFromServer();
					fetchFromServer.execute(authMgr.getAuthTokens());
				}
			});
		}

		// Called when the connection with the service disconnects
		// unexpectedly
		public void onServiceDisconnected(final ComponentName className) {
			socialService = null;
		}

	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.feed_list);
			refreshButton = (ImageButton) findViewById(R.id.force_refresh);
			
			publishButton = (Button)findViewById(R.id.publish);
			followButton = (Button)findViewById(R.id.follow_unfollow);
			routeButton =(Button)findViewById(R.id.route);
			
			publishButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handlePublish();
				}
			});
			
			followButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					handleFollowUnfollow();
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

		followButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				FollowAsyncTask followAsyncTask = new FollowAsyncTask();
				followAsyncTask.execute(edittext.getText().toString());
			}
		});
		
		unfollowButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
				UnFollowAsyncTask unfollowAsyncTask = new UnFollowAsyncTask();
				unfollowAsyncTask.execute(edittext.getText().toString());
			}
		});
	
		dialog.show();
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
	private void handlePublish() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.publish);
		dialog.setContentView(R.layout.publish);

		final Button publishButton = (Button) dialog.findViewById(R.id.publish);
		final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
		final EditText tweetText = (EditText) dialog.findViewById(R.id.publish_text);
		final ImageButton speakButton= (ImageButton) dialog.findViewById(R.id.speak);

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
							e.printStackTrace();
						}
					else
						out.println("anonymous");
					out.println(feedbackText.getText().toString());
					out.close();
					clientSocket.close();
					Toast.makeText(getApplicationContext(),"Successfully sent feedback.", Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					e.printStackTrace();
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

		final Button applyButton = (Button) dialog
				.findViewById(R.id.apply_button);
		final Button cancelButton = (Button) dialog
				.findViewById(R.id.cancel_button);
		final CheckBox removeAccountCheck = (CheckBox) dialog
				.findViewById(R.id.remove_account_check);
		final EditText frequencySelect = (EditText) dialog
				.findViewById(R.id.refresh_rates);

		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (removeAccountCheck.isChecked()) {
					resetAccount();
				}
				if (!removeAccountCheck.isChecked()) {
					updateFeedRefreshInterval(frequencySelect.getText()
							.toString());
				}
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
	 * @param selectedItem
	 * @param refreshInterval
	 */
	private void updateFeedRefreshInterval(final String selectedItem) {
		final int interval = Integer.valueOf(selectedItem);

		final TwitterManager feedManager = new TwitterManager(getApplicationContext());
		feedManager.setTwitterFeedRefreshInterval(interval);

		// Update alarm manager in current running application instance
		final AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		final Intent intent = new Intent(getApplicationContext(),
				AlarmReceiver.class);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "ON START");

	}

	/**
	 * Clear All Notification when this screen is shown
	 */
	private void clearNotification() {
		NotificationManager notificationManager = (NotificationManager) getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancelAll();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "ON PAUSE");
	}

	/**
	 * Register Broadcast receiver to update twit feeds when SocialService sends
	 * a broadcast of new twtis available
	 * */
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "ON START");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onNewIntent(android.content.Intent)
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "ON NEW INTENT");
		super.onNewIntent(intent);
		// If this Resume is happening because of clicking on Notification than
		// refresh from database

		if (null != intent && null != intent.getExtras()
				&& intent.getExtras().containsKey("Refresh")
				&& intent.getExtras().getBoolean("Refresh")) {
			Log.d(TAG, "***** Refresh as I am coming from notification");
			// FIXME not action happens when refreshButton is clicked();
			refreshButton.performClick();
			;
		}
		clearNotification();
	}

	@Override
	protected void onStop() {
		super.onStop();
		socialService = null;
		Log.d(TAG, "ON STOP");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "ON DESTROY");
		// Other wise we will leak a connection
		try {
			unbindService(connection);
		} catch (Exception ex) {

		}

	}
}
