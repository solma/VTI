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

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;


public class VTIMain extends TabActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, SocialFeed.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("Notifications").setIndicator("Notifications"
        					//,res.getDrawable(R.drawable.ic_tab_notification)
        					).setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        intent = new Intent().setClass(this, RouteSubscription.class);
        spec = tabHost.newTabSpec("Routes").setIndicator("Routes"
        		 		//,res.getDrawable(R.drawable.ic_tab_notification)
        		 		).setContent(intent);
        tabHost.addTab(spec);

        
        intent = new Intent().setClass(this, Tracker.class);
        spec = tabHost.newTabSpec("Tracker").setIndicator("Tracker"
                          //,res.getDrawable(R.drawable.ic_tab_songs)
                      	).setContent(intent);
        tabHost.addTab(spec);
		

        tabHost.setCurrentTab(0);
    }
}