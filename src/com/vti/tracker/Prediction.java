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
import java.util.Calendar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.ListActivity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.vti.Constants;
import com.vti.R;
import com.vti.utils.Log;


public class Prediction extends ListActivity {
	private static final String TAG = Prediction.class.getSimpleName();
	private String rtId;
	private String stpId;
	private boolean isBus;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e(TAG, "On Create");
	    Bundle b = getIntent().getExtras();
	    rtId=b.getString("rtId");
	    stpId = b.getString("stpId");
	    isBus= b.getInt("isBus")>0?true:false;
	    ArrayList<String> values;
		if(!isOnline()){
			Toast.makeText(getApplicationContext(), Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
			return;
		}
	    if(isBus){
	    	Log.e(TAG, rtId+ "   "+ stpId);
	    	values=getBusPredictions(rtId, stpId);
	    }else{
	    	values=getTrainPredictions(stpId);
	    }
	    Log.e(TAG, values.toString());
	    if(values.size()>0){
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					R.layout.tracker_stop_prediction_row, R.id.prediction,
					values.toArray(new String[1]));
			setListAdapter(adapter);
		}else{
			Toast.makeText(getApplicationContext(), "Please try later.", Toast.LENGTH_SHORT).show();
		}
	}
	
	private boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	private ArrayList<String> getBusPredictions(String rtId, String stpId){
		String url="http://www.ctabustracker.com/bustime/api/v1/getpredictions?key="+Constants.CTA_BUS_TRACKER_KEY+"&rt="+rtId+"&stpid="+stpId;
		ArrayList<String> ret=new ArrayList<String>();
		try {
			Document doc=Jsoup.connect(url).get();
			Elements prds=doc.select("bustime-response > prd ");
			for(Element ele: prds){
				int diff=(int) ((string2Long(ele.select("prdtm").text())-(string2Long(ele.select("tmstmp").text())))/1000/60);
				ret.add(String.valueOf(diff+" minutes"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * @param: stpid: the stop id, return all available results 
	 */
	private ArrayList<String> getTrainPredictions(String stpId){
		String url="http://lapi.transitchicago.com/api/1.0/ttarrivals.aspx?key="+Constants.CTA_TRAIN_TRACKER_KEY+"&stpid="+stpId;
		ArrayList<String> ret=new ArrayList<String>();
		try {
			Document doc=Jsoup.connect(url).get();
			long tmst=string2Long(doc.select("ctatt > tmst").text());
			Elements arrivals=doc.select("ctatt > eta > arrT");
			for(Element ele: arrivals){
				int diff=(int) ((string2Long(ele.text())-tmst)/1000/60);
				ret.add(String.valueOf(diff)+" minutes");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	
	/**
	 * @param arrT, e.g. "20120123 21:43:18" for train predication and "20120124 22:33" for bus prediction
	 * @return time 
	 */
	private long string2Long(String arrT){
		if(arrT.length()<17){
			arrT=arrT+":00";
		}
		Calendar cal=Calendar.getInstance();
		int year=Integer.parseInt(arrT.substring(0, 4));
		int month=Integer.parseInt(arrT.substring(4, 6));
		int date=Integer.parseInt(arrT.substring(6, 8));
		int hourOfDay=Integer.parseInt(arrT.substring(9, 11));
		int minute=Integer.parseInt(arrT.substring(12, 14));
		int second=Integer.parseInt(arrT.substring(15, 17));
		cal.set(year, month, date, hourOfDay, minute, second);
		return cal.getTimeInMillis();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
	}

}

