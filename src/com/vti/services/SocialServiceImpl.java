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
package com.vti.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.vti.Constants;
import com.vti.R;
import com.vti.services.ISocialService;
import com.vti.SocialFeed;
import com.vti.adapters.DBAdapter;
import com.vti.managers.AccountManager;
import com.vti.managers.TwitterManager;
import com.vti.model.Twit;

public class SocialServiceImpl extends Service implements TextToSpeech.OnInitListener{
	private static final String TAG = SocialServiceImpl.class.getSimpleName();
	
	private TextToSpeech mTts;
	private final ISocialService.Stub mBinder = new ISocialService.Stub() {

		public List<Twit> getFromServer() throws RemoteException {
			final TwitterManager feedManager = new TwitterManager(getApplicationContext());
			final AccountManager authMgr = new AccountManager(getApplicationContext());
			if (!authMgr.isAccountEmpty()) {
				final List<Twit> twits = feedManager.getSocialFeed();
				final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
				dbAdapter.open();
				//updated local notification database
				if (twits != null)
					for (final Twit twit : twits) {
						final long noOfRowsEffected = dbAdapter.updateTwit(
								twit.getTwitId(), twit.getTimestamp(), twit.getProfileName(),
								twit.getImageUrl(), twit.getTwitMessage(),
								twit.getUpThumbs(), twit.getDownThumbs());
						Log.e(TAG, "GetCurrentSF: " + twit.getProfileName()
								+ " " + twit.getTwitMessage());
						// Check for new notifications
						if (noOfRowsEffected < 1) {
							// Insert if not already present
							dbAdapter.insertTwit(twit.getTwitId(), twit.getTimestamp(),twit.getProfileName(), 
									twit.getImageUrl(),	twit.getTwitMessage(),
									twit.getUpThumbs(), twit.getDownThumbs());
						}
					}
				dbAdapter.close();
				//return twits;
				return getFromDB();
			} else {
				return new ArrayList<Twit>();
			}
		}
		
		public List<Twit> getFromEncounteredAccount(String userName) throws RemoteException {
			final TwitterManager feedManager = new TwitterManager(getApplicationContext());
			final AccountManager authMgr = new AccountManager(getApplicationContext());
			if (!authMgr.isAccountEmpty()) {
				final List<Twit> twits = feedManager.getUserTimeline(userName);
				final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
				dbAdapter.open();
				//updated local notification database
				if (twits != null)
					for (final Twit twit : twits) {
						final long noOfRowsEffected = dbAdapter.updateTwit(
								twit.getTwitId(), twit.getTimestamp(),twit.getProfileName(),
								twit.getImageUrl(), twit.getTwitMessage(),
								twit.getUpThumbs(), twit.getDownThumbs());
						Log.e(TAG, "GetCurrentSF: " + twit.getProfileName()
								+ " " + twit.getTwitMessage());
						// Check for new notifications
						if (noOfRowsEffected < 1) {
							// Insert if not already present
							dbAdapter.insertTwit(twit.getTwitId(), twit.getTimestamp(),twit.getProfileName(), 
									twit.getImageUrl(),	twit.getTwitMessage(),
									twit.getUpThumbs(), twit.getDownThumbs());
						}
					}
				dbAdapter.close();
				//return twits;
				return getFromDB();
			} else {
				return new ArrayList<Twit>();
			}
		}

		/**
		 * 
		 * @return All Twits present in the DB
		 */
		public List<Twit> getFromDB() throws RemoteException{
			final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
			dbAdapter.open();
			final Cursor cursor = dbAdapter.getAllTwits();
			try {
				//final int dbEntries = cursor.getCount();
				final List<Twit> dbTwits = new ArrayList<Twit>();
				while (cursor.moveToNext()) {
					dbTwits.add(new Twit(cursor.getLong(0), cursor.getLong(1),
							cursor.getString(2), cursor.getString(3), cursor
									.getString(4)));
					Log.d(TAG,"GetDB: " + cursor.getString(1) + " "+ cursor.getString(3));
				}
				return dbTwits;
			} finally {
				if (null != cursor) {
					cursor.close();
				}
				dbAdapter.close();
			}
		}

	};

	@Override
	public IBinder onBind(final Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
        // Initialize text-to-speech. This is an asynchronous operation.
        // The OnInitListener (second argument) is called after initialization completes.
        mTts = new TextToSpeech(this,
        		this  // TextToSpeech.OnInitListener
            );
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		// Don't forget to shutdown mTts!
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");
		if ((null != intent)
				&& intent.getExtras().containsKey("ACTION")
				&& "UPDATE_FEEDS".equals(intent.getExtras().getString("ACTION"))) {
			try {
				updateFeeds();
			} catch (final RemoteException e) {
				e.printStackTrace();
			}
		}
		return START_STICKY;
	}

	// FIXME - Need more optimum logic, but this will do for the demo
	private void updateFeeds() throws RemoteException {
		Log.d(TAG, "updateFeed() called at " + (new Date()));
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				ArrayList<String> notificationMsgs=new ArrayList<String>();
				boolean sendNotification = false;
				final TwitterManager feedManager = new TwitterManager(
						getApplicationContext());
				final AccountManager authMgr = new AccountManager(
						getApplicationContext());
				if (!authMgr.isAccountEmpty()) {
					final List<Twit> twits = feedManager.getSocialFeed();
					final DBAdapter dbAdapter = new DBAdapter(
							getApplicationContext());
					dbAdapter.open();
					if (twits != null)
						for (final Twit twit : twits) {
							final long noOfRowsEffected = dbAdapter.updateTwit(
									twit.getTwitId(), twit.getTimestamp(),twit.getProfileName(),
									twit.getImageUrl(), twit.getTwitMessage(),
									twit.getUpThumbs(), twit.getDownThumbs());
							// Check for new notification
							if (noOfRowsEffected < 1) {
								dbAdapter.insertTwit(twit.getTwitId(), twit.getTimestamp(),twit.getProfileName(),
										twit.getImageUrl(), twit.getTwitMessage(),
										twit.getUpThumbs(), twit.getDownThumbs());
								// Notify so that user comes to know about this
								sendNotification = true;
								notificationMsgs.add(twit.getTwitMessage());
							}
						}
					if (sendNotification) {
						sendNotification(notificationMsgs);
					}
					dbAdapter.close();
				}
			}
		};
		final Thread thread = new Thread(runnable);
		thread.start();
	}

	private void sendNotification(ArrayList<String> notificationMsgs) {
		final String ns = Context.NOTIFICATION_SERVICE;
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		final Notification notification = new Notification(R.drawable.icon, "VTI", System.currentTimeMillis());
		notification.defaults |= Notification.DEFAULT_SOUND;
		//notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		final Context context = getApplicationContext();
		final CharSequence contentTitle = "New VTI Notifications";
		final CharSequence contentText = "You have new notificaions!";
		final Intent notificationIntent = new Intent(this, SocialFeed.class);
		notificationIntent.putExtra("Refresh", true);
		Log.e(TAG, String.valueOf(notificationMsgs.size()) );
		final SharedPreferences settings = getSharedPreferences(
				Constants.SETTING_PREFERENCE_FILE, 0);
		boolean voiceNotify=settings.getBoolean(Constants.VOICE_NOTIFY, true);
		if(voiceNotify){
			for(int i=0;i<notificationMsgs.size();i++){
				if(i==0)
					mTts.speak("New VTI Message:"+notificationMsgs.get(i), TextToSpeech.QUEUE_FLUSH, null);
				else
					mTts.speak("New VTI Message:"+notificationMsgs.get(i), TextToSpeech.QUEUE_ADD, null);
			}
		}
		final PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,	contentIntent);
		final int HELLO_ID = 1;
		mNotificationManager.notify(HELLO_ID, notification);
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
