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
package com.vti;

import com.vti.R;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Help extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help);
		TextView help = (TextView) findViewById(R.id.help);
		final String helpText = "VTI (Volunteered Traveler Information) is a twitter application that dedicates to real-time " +
				"transportation information. Users can publish transportation information to Twitter and also subscribe such " +
				"information from Twitter via VTI.\n\n" +
				
				"You can publish transportation information by\n(i). clicking " +
				"the 'publish' button\n(ii). editing a received notification by clicking its associated 'retweet' " +
				"button or pressing it for 1 sec.\n\n" +
				
				"You can subscribe transportation information by\n(i). " +
				"clicking the 'follow/unfollow' or 'follow/unfollow by select' buttons to directly follow/unfollow specified Twitter " +
				"accounts that governed by VTI (each VTI Twitter account corresponds to an geographical area such that all " +
				"transportation information within the area that are posted to Twitter via VTI is reposted by the account)\n" +
				"(ii). click the 'route' button to define a customized route and subscribe the route. Note that at any time " +
				"only one route can be subscribed. Subscribe to a new route will automatically unsubscribe last route if it " +
				"has not been unsubscribed.\n\nFor any received notification, you can 'thumb' it up or down. This will help " +
				"VTI filter inaccurate and falsified information and improve notification data quality.\n\n" +
				
				"VTI mainly relies on user generated data. Therefore the more people use it, the more useful it becomes. So any" +
				" activity/idea that help promote the application is extremely welcome.";
		help.setText(helpText);
	}

}
