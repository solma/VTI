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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vti.R;
import com.vti.adapters.DBAdapter;
import com.vti.adapters.DBAdapter.CTATrackerDatabaseHelper;
import com.vti.model.CTARoute;
import com.vti.model.CTAStop;
import com.vti.utils.Log;


public class AllStops extends ListActivity {
	private static final String TAG = AllStops.class.getSimpleName();
	private CTARoute route;
	private String dir;
	private ArrayList<CTAStop> allStops=new ArrayList<CTAStop>();
	//private ArrayList<CTABusStop> allTrainStops=new ArrayList<CTATrainStop>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "On Create");
	    Bundle b = getIntent().getExtras(); 
	    route = b.getParcelable("route");
	    dir = b.getString("direction");
	    String [] values;
    	getAllStops(route.rtId(), dir);
    	if(allStops.size()>0){
	    	values=new String[allStops.size()];
			for(int i=0;i<values.length;i++){
				values[i]=allStops.get(i).stpName();
			}
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.tracker_stop_row, R.id.stopName, values);
		    setListAdapter(adapter);
	    }
	}
	
	/**
	 * @return All stops of the selected directed route in DB
	 */
	private void getAllStops(String rtId, String dir){
		final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
		CTATrackerDatabaseHelper myDbHelper = dbAdapter.getCTATrackerDBHelper();
		Cursor cursor=null;
	 	try {
	 		myDbHelper.openDataBase();
	 		cursor = myDbHelper.getAllStops(rtId, dir);
			while (cursor.moveToNext()) {
				allStops.add(new CTAStop(cursor.getString(0), cursor.getString(1), cursor.getString(2), 
						cursor.getString(3), cursor.getString(4), cursor.getString(5),Boolean.valueOf(cursor.getString(6))) );
			}
	 	}catch(SQLException e){
	 		Log.d(TAG,Log.stack2string(e));
	 	}finally {
			if (null != cursor) {
				cursor.close();
			}
			myDbHelper.close();
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Intent navIntent = new Intent(getApplicationContext(),Prediction.class);
        Bundle b = new Bundle();
        final CTAStop stop = allStops.get(position);
        b.putString("rtId", route.rtId());
        b.putString("stpId", stop.stpId());
        b.putInt("isBus", stop.isBus()?1:0);
        navIntent.putExtras(b);
		startActivity(navIntent);	
	}

}

