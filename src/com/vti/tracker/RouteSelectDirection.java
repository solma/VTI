package com.vti.tracker;

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

import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.vti.R;
import com.vti.RouteSubscription;
import com.vti.VTIMain;
import com.vti.model.CTARoute;
import com.vti.utils.Log;
import com.vti.utils.PercentEncode;


public class RouteSelectDirection extends ListActivity {
	private static final String TAG = RouteSelectDirection.class.getSimpleName();
	private CTARoute route;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    Bundle b = getIntent().getExtras(); 
	    route = b.getParcelable("route");
	    String [] values=route.rtDir().split(";");
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.tracker_route_direction_row, R.id.dir, values);
			setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final String item = (String) getListAdapter().getItem(position);
    	final Intent navIntent = new Intent(getApplicationContext(),AllStops.class);
        Bundle b = new Bundle(); 
        b.putParcelable("route", route);
        b.putString("direction", item);
        navIntent.putExtras(b);
		startActivity(navIntent);
	}

}

