package com.vti.utils;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vti.utils.Log;

import com.google.android.maps.GeoPoint;
import com.vti.Constants;

public class GeoCoder {
	private static final String TAG=GeoCoder.class.getSimpleName();
	
	public static GeoPoint geocode(String address){
		String url="http://maps.googleapis.com/maps/api/geocode/xml?address="+address+"&sensor=true";
		String[] laln=new String[2];
		try {
			Log.e(TAG, url);
			org.jsoup.nodes.Document doc=Jsoup.connect(url).get();
			laln[0]=doc.select("result > geometry > location > lat").first().text();
			laln[1]=doc.select("result > geometry > location > lng").first().text();
			//out of the boundary, then return null
			Double lat=Double.parseDouble(laln[0]);
			Double ln=Double.parseDouble(laln[1]);
			if(lat<=Constants.NORTH&&lat>=Constants.SOUTH&&ln>=Constants.WEST&&ln<=Constants.EAST)
				return new GeoPoint((int) (lat * 1E6),(int) (ln * 1E6));
			else
				return null;
			} catch (IOException e) {
				Log.d(TAG, Log.stack2string(e));
		}
		return null;
	}
	
	/*
	 * @return reverseGeocode
	 */
	public static String reverseGeocode(double lat, double ln){
		String url="http://maps.googleapis.com/maps/api/geocode/xml?address="+lat+","+ln+"&sensor=true";
		String address=null;
		try {
			Document doc=Jsoup.connect(url).get();
			if(doc!=null){
				address=doc.select("result > formatted_address").first().text();
				Log.e(TAG, address);
			}
		} catch (Exception e) {
			Log.d(TAG, Log.stack2string(e));
		}
		return address;
	}
	
	/*
	 * @return the VTI account corresponds to the geopoint
	 */
	public static String determineVTIAccount(GeoPoint p){
		if(p==null)
			return null;
		// only have 25 zones......
		int row, col, zoneId;
		row=(int) ((p.getLatitudeE6()-Constants.SOUTH*1.0E6)/Constants.ZONE_LATITUDE/2);
		col=(int) ((p.getLongitudeE6()-Constants.WEST*1.0E6)/Constants.ZONE_LONGITUDE/2);
		zoneId=row*5+col;
		Log.e(TAG,"belongs to account:  vti_zone_"+zoneId+"   (row="+row+",col="+col+")");
		if(zoneId<25&&zoneId>=0)
			if(zoneId<10) return "vti_zone_0"+zoneId;
			else return "vti_zone_"+zoneId;
		else
			return null;
	}
	


}
