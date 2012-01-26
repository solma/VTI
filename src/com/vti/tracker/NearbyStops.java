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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.vti.Constants;
import com.vti.R;
import com.vti.adapters.DBAdapter;
import com.vti.adapters.DBAdapter.CTATrackerDatabaseHelper;
import com.vti.managers.LocManager;
import com.vti.model.CTAStop;
import com.vti.utils.Log;


public class NearbyStops extends ListActivity {
	private static final String TAG = NearbyStops.class.getSimpleName();
	private ArrayList<CTAStop> allStops=new ArrayList<CTAStop>();
	private LocationUpdateListener ll;
	private LocManager locMgr;
	//private ArrayList<CTABusStop> allTrainStops=new ArrayList<CTATrainStop>();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "On Create");
		locMgr=new LocManager(getApplicationContext());
		ll=new LocationUpdateListener();
		ll.setLocManager(locMgr);
		for(String provider: locMgr.getLocationManager().getProviders(true)){
			locMgr.getLocationManager().requestLocationUpdates(provider, Constants.MINTIME, Constants.MINDISTANCE, ll);
			break; //only one provider would do
		}
		final Location curLoc=locMgr.getLatestLocation();
		if(null!=curLoc){
			show(curLoc);
		}
	}
	
	protected void onDestroy(){
		if(null!=ll){
			//remove this listener
			locMgr.getLocationManager().removeUpdates(ll);
		}
	}
	
	private void show(Location curLoc){
		getNearbyStops(curLoc);
	    String [] values;
	    final int MAX_SIZE=20;
    	if(allStops.size()>0){
    		Collections.sort(allStops, new StopComparator());
    		int size=allStops.size();
	    	values=new String[size>MAX_SIZE?MAX_SIZE:size];
	    	DecimalFormat df = new DecimalFormat("#.##");
			for(int i=0;i<values.length;i++){
				CTAStop stop=allStops.get(i);
				values[i]=stop.rtId()+"\t\t"+stop.rtDir()+"\n"+stop.stpName()+"\n"+df.format(stop.dist())+" miles";
			}
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(NearbyStops.this,
					R.layout.tracker_stop_row, R.id.stopName, values);   
	    	setListAdapter(adapter);
	    }
	}
	
	/*
	 * @return return distance between two geo points in miles
	 */
	public static double distBetween(double lat1, double lng1, double lat2,
			double lng2) {
		final double EARTHRADIUS=3958.75;
		//final double MILETOKM=1.609344;

		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double dist = (EARTHRADIUS) * c;

		return dist;
	}
	
	/**
	 * @return nearby stops 
	 */
	private void getNearbyStops(Location cur){
		final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
		final int MAX_DISTANCE=3;
		CTATrackerDatabaseHelper myDbHelper = dbAdapter.getCTATrackerDBHelper();
		Cursor cursor=null;
		double lat1=cur.getLatitude();
		double lon1=cur.getLongitude();
		
	 	try {
	 		myDbHelper.openDataBase();
	 		cursor = myDbHelper.getAllStops();
	 		//int count=0;
			while (cursor.moveToNext()) {
				double lat2=Double.parseDouble(cursor.getString(2));
				double lon2=Double.parseDouble(cursor.getString(3));
				double dist=distBetween(lat1, lon1, lat2, lon2);
				if(dist<MAX_DISTANCE){
					CTAStop stop=new CTAStop(cursor.getString(0), cursor.getString(1), cursor.getString(2), 
							cursor.getString(3), cursor.getString(4), cursor.getString(5),Boolean.valueOf(cursor.getString(6)));
					stop.setDist(dist);
					allStops.add(stop);
					//count++;
				}
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
        b.putString("rtId", stop.rtId());
        b.putString("stpId", stop.stpId());
        b.putInt("isBus", stop.isBus()?1:0);
        navIntent.putExtras(b);
		startActivity(navIntent);	
	}
	
	class LocationUpdateListener implements LocationListener{
		LocManager locMgr;
		@Override
		public void onLocationChanged(Location update) {
			Log.e(TAG, "receive a new Location");
			Location curLoc=locMgr.getLastLocation();
			show(curLoc);
			return;
		}
		
		public void setLocManager(LocManager locManager){
			locMgr=locManager;
		}

		@Override
		public void onProviderDisabled(String arg0) {}

		@Override
		public void onProviderEnabled(String arg0) {}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
	}
	
	class StopComparator implements Comparator<CTAStop> {
	    @Override
	    public int compare(CTAStop o1, CTAStop o2) {
	    	double dist=o1.dist()-o2.dist();
	    	if(dist==0){
	    		return 0;
	    	}else{
	    		if(dist>0) return 1;
	    		else return -1;
	    	}
	    }

	}

}

