/**
 * Copyright Sol Ma
 * 
 * 
 * 
 * 
 */

package com.vti;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.vti.utils.Log;

import com.vti.managers.TwitterManager;

/**
 * Watch out for battery level and disable Twitter service, when battery level is high then start the service again
 * 
 */
public class BatteryBroadcastReceiver extends BroadcastReceiver
{

	/**
	 * On Receiving batter level lower than 20, we can stop AlarmManager and when its more than 20 we can start alarm
	 * manager again
	 */
	@Override
	public void onReceive(final Context context, final Intent intent)
	{
		
		final int level = intent.getIntExtra("level", 0);
		if ( level < 20 )
		{
			Log.d(BatteryBroadcastReceiver.class.getSimpleName(),"Battery level too low, suspending alarm manager");
			final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			final Intent alarmIntent = new Intent(context, AlarmReceiver.class);
			final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 100, alarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.cancel(pendingIntent);
		}
		else
		{
			Log.d(BatteryBroadcastReceiver.class.getSimpleName(),"Battery level back to normal, starting alarm manager");
			setAlarm(context);
		}

	}


	private void setAlarm(final Context context)
	{
		final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		final Intent intent = new Intent(context, AlarmReceiver.class);
		final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 100, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		final TwitterManager feedManager = new TwitterManager(context);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (5000), feedManager
				.getTwitterFeedRefreshInterval(), pendingIntent);

	}

}
