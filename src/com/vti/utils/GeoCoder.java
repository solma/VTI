package com.vti.utils;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.vti.Constants;

public class GeoCoder {
	private static final String TAG=GeoCoder.class.getSimpleName();
	
	public static GeoPoint geocode(String address){
		String url="http://maps.googleapis.com/maps/api/geocode/xml?address="+address+"&sensor=true";
		String[] laln=new String[2];
		try {
			Log.e(TAG, address);
			org.jsoup.nodes.Document doc=Jsoup.connect(url).get();
			laln[0]=doc.select("result > geometry > location > lat").first().text();
			laln[1]=doc.select("result > geometry > location > lng").first().text();
			return new GeoPoint((int) (Double.parseDouble(laln[0]) * 1E6),
					(int) (Double.parseDouble(laln[1]) * 1E6));
			} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * @return the VTI account corresponds to the geopoint
	 */
	public static String reverseGeocode(GeoPoint p){
		int row, col;
		row=(int) ((p.getLatitudeE6()-Constants.SOUTH*1.0E6)/Constants.ZONE_LATITUDE);
		col=(int) ((p.getLongitudeE6()-Constants.WEST*1.0E6)/Constants.ZONE_LONGITUDE);
		Log.d(TAG,"Subscribing account:  vti_zone_"+row+col);
		if(row>=0&&row<=9&&col>=0&&col<=9)
			return "vti_zone_"+row+col;
		else
			return null;
	}
	


}
