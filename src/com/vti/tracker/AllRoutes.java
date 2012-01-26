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
import java.util.Collections;
import java.util.Comparator;

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
import com.vti.utils.Log;


public class AllRoutes extends ListActivity {
	private static final String TAG = AllRoutes.class.getSimpleName();
	private ArrayList<CTARoute> allRoutes;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		allRoutes=new ArrayList<CTARoute>();
		getAllRoutes();
		
		if(allRoutes.size()>0){
			Collections.sort(allRoutes, new RouteComparator());
			String[] values = new String[allRoutes.size()];
			for(int i=0;i<values.length;i++){
				if(allRoutes.get(i).isBus()){
					values[i]=allRoutes.get(i).rtId()+"\t\t"+allRoutes.get(i).rtName();
				}else{
					values[i]=allRoutes.get(i).rtName()+" line";
				}
			}
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.tracker_route_row, R.id.routeName, values);
			setListAdapter(adapter);
		}
	}
	
	protected void onDestroy(){
		super.onDestroy();
	}
	
	/**
	 * 
	 * @return All Routes in the DB
	 */
	private void getAllRoutes(){
		final DBAdapter dbAdapter = new DBAdapter(getApplicationContext());
		CTATrackerDatabaseHelper myDbHelper = dbAdapter.getCTATrackerDBHelper();
		Cursor cursor=null;
	 	try {
	 		myDbHelper.openDataBase();
	 		cursor = myDbHelper.getAllRoutes();
			while (cursor.moveToNext()) {
				allRoutes.add(new CTARoute(cursor.getString(0), cursor.getString(1),
						      cursor.getString(2), Boolean.valueOf(cursor.getString(3))) );
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
		final CTARoute route = allRoutes.get(position);
    	final Intent navIntent = new Intent(getApplicationContext(),RouteSelectDirection.class);
        Bundle b = new Bundle(); 
        b.putParcelable("route", route);
        navIntent.putExtras(b);
		startActivity(navIntent);
	}
	
	class RouteComparator implements Comparator<CTARoute> {
	    @Override
	    public int compare(CTARoute o1, CTARoute o2) {
	    	if(o1.isBus()&&!o2.isBus()){ //o1 is bus and o2 is train
	    		return 1;
	    	}else{
	    		if(!o1.isBus()&&o2.isBus()){  //o2 is bus and o1 is train
	    	   		return -1;
	    		}else{
	    			if(!o1.isBus()&&!o2.isBus()){ //both train routes
		    	   		return o1.rtId().compareTo(o2.rtId());
		    		}else{ //both bus routes  //2>110
		    			int r1=toInt(o1.rtId());
		    			int r2=toInt(o2.rtId());
		    			if(r1>0||r2>0){
		    				return r1-r2;
		    			}else{
		    				return o1.rtId().compareTo(o2.rtId());
		    			}
		    		}
	    		}
	    	}
	    }

	    public int toInt(String str){
	    	  int ret=0;
	    	  try{
	    		  ret=Integer.parseInt(str);
	    	  }catch(Exception e){
	    		  try{
	    			  ret=Integer.parseInt(str.substring(0,str.length()-1));
	    		  }catch(Exception ex){
	      		  }
	    	  }
	    	  return ret;
	    }
	        

	}

}

