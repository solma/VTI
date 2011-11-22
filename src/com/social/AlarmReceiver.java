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
package com.social;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.social.model.OAuthTokens;
import com.social.services.ISocialService;
import com.social.services.SocialServiceImpl;
import com.social.services.managers.OAuthAuthenticatonMgr;

/**
 * @author rohit
 * 
 */

public class AlarmReceiver extends BroadcastReceiver {

	

	@Override
	public void onReceive(final Context context, Intent intent) {
		Runnable runnable = new Runnable(){

			public void run() {
				Intent serviceIntent = new Intent(context,SocialServiceImpl.class);
				serviceIntent.putExtra("ACTION", "UPDATE_FEEDS");
				context.startService(serviceIntent);
				
			}
			
		};
		
		Thread thread = new Thread(runnable);
		thread.start();
		
		
	}
}
