package com.vti.utils;

import java.io.IOException;

import org.jsoup.Jsoup;

import android.util.Log;

import com.google.android.maps.GeoPoint;

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

}
