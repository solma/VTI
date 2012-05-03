package com.vti;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.vti.managers.LocManager;
import com.vti.managers.TwitterManager;
import com.vti.model.Station;
import com.vti.model.TransitRoute;
import com.vti.utils.GeoCoder;
import com.vti.utils.Log;
import com.vti.utils.PercentEncode;

public class RouteSubscription extends MapActivity {
	/** 
	 * Warning Messages
	 */
	private static final String TAG = RouteSubscription.class.getSimpleName();
	private static final String ROUTE_QUERY_FAILURE="Cannot find a route given the From-To pair.";
	private static final String ROUTE_SUBSCRIBE_FAILURE="Failed to subscribe to the route.";
	private static final String WAIT_MESSAGE="Calculating routes and relevant VTI Twitter accounts......";
	/**
	 * UI widgets
	 */
	private MapController mapController;
	private MapView mapView;
	private Button unsubscribeButton;
	private Button subscribeButton;
	private ImageButton switchButton;
	private RadioButton priv;
	//private RadioButton pub;
	private EditText from;
	private EditText to;
	private ImageButton fromSpinner;
	private ImageButton toSpinner;
	
	private Context context;
	private TwitterManager twitterMgr;
	private LocManager locMgr;
	private MapUpdateHandler mHandler;
	private VTIAccountsUpdateHandler vHandler;
	
	private ArrayList<String> fromHistory;
	private ArrayList<String> toHistory;
	private ArrayList<String> vtiAccounts;
	
	private ArrayList<GeoPoint> priRoute;
	private TransitRoute pubRoute;
	private ArrayList<TransitRoute> transitRoutes;

	
	public void onCreate(Bundle bundle) {
		// initialize
		super.onCreate(bundle);
		setContentView(R.layout.route); 
		Log.d(TAG, "On Create");
		context = getApplicationContext();
		twitterMgr=new TwitterManager(getApplicationContext());
		
		
		// Restore preferences
		int i;
		String tmp = getSharedPreferences(Constants.ROUTE_PREFERENCE_FILE, 0).getString(Constants.FROM_HISTORY, null);
		//from history
		fromHistory=new ArrayList<String>();
		if(tmp!=null){
			String[] fields=tmp.split(Constants.DELIMITER);
			for(i=0;i<fields.length;i++)
				fromHistory.add(fields[i]);
		}
		//to history
		toHistory=new ArrayList<String>();
		tmp = getSharedPreferences(Constants.ROUTE_PREFERENCE_FILE, 0).getString(Constants.TO_HISTORY, null);
		if(tmp!=null){
			String[] fields=tmp.split(Constants.DELIMITER);
			for(i=0;i<fields.length;i++)
				toHistory.add(fields[i]);
		}
		// vti accounts
		vtiAccounts=new ArrayList<String>();
		tmp = getSharedPreferences(Constants.ROUTE_PREFERENCE_FILE, 0).getString(Constants.LAST_VTI_ACCOUNTS, null);
		if(tmp!=null){
			String[] fields=tmp.split(Constants.DELIMITER);
			for(i=0;i<fields.length;i++)
				vtiAccounts.add(fields[i]);
		}	
		
		// create a map view
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setStreetView(true);
		mapView.setTraffic(true);
		mapController = mapView.getController();
		mapController.setZoom(13); // Zoom 1 is world view
		locMgr = new LocManager(context);

		
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		String bestProvider=locMgr.getLocationManager().getBestProvider(criteria, true);
		//Location lastKnownLocation=locationManager.getLastKnownLocation(bestProvider);
		mHandler=new MapUpdateHandler();
		if(bestProvider==null){
			bestProvider=LocationManager.GPS_PROVIDER;
			Toast.makeText(context, Constants.LOCATION_DISABLED, Toast.LENGTH_LONG).show();
		}
		locMgr.getLocationManager().requestLocationUpdates(bestProvider, Constants.MINTIME, Constants.MINDISTANCE, mHandler);

		priv=(RadioButton) findViewById(R.id.priv);
		//pub=(RadioButton) findViewById(R.id.pub);

		from = (EditText) findViewById(R.id.from_text);
		to = (EditText) findViewById(R.id.to_text);

		unsubscribeButton = (Button) findViewById(R.id.unsubscribe);
		unsubscribeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if(!isOnline()){
					Toast.makeText(context, Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
					return;
				}
				unsubscribeLastRoute();
			}
		});

		subscribeButton = (Button) findViewById(R.id.subscribe);
		subscribeButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if(!isOnline()){
					Toast.makeText(context, Constants.INTERNET_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
					return;
				}
				String origin;
				String fromText=from.getText().toString().trim();
				String toText=to.getText().toString().trim();
				int idx, size;
				if(!"Current Location".equals(fromText))
					origin=fromText+",Chicago";
				else{
					Location loc=locMgr.getLatestLocation();
					if(loc==null){
						Toast.makeText(context, Constants.LOCATION_NOT_AVAILABLE, Toast.LENGTH_SHORT).show();
						return;
					}
					origin=GeoCoder.reverseGeocode(loc.getLatitude(),loc.getLongitude());
					if(origin==null){
						Toast.makeText(context, Constants.REVERSE_GEOCODE_ERROR, Toast.LENGTH_LONG).show();
						return;
					}
				}
				String dest=to.getText().toString().trim()+",Chicago";
				if(priv.isChecked()){
					unsubscribeLastRoute();
					new SubscribePrivateRouteAsyncTask().execute(origin, dest);
					//subscribePriRoute(origin, dest);
				}else{
					unsubscribeLastRoute();
					new SubscribeTransitRouteAsyncTask().execute(origin, dest);
				}
				if(!"Current Location".equals(fromText)){
					idx=fromHistory.indexOf(fromText);
					size=fromHistory.size();
					if(idx>=0){//not a new input
						fromHistory.remove(idx);
						fromHistory.add(fromText);
					}else{//a new input
						if (size < Constants.HISTORY_SIZE) { //not reach capacity
							fromHistory.add(fromText);
						} else {
							fromHistory.remove(0);
							fromHistory.add(fromText);
						}
					}
				}
				idx=toHistory.indexOf(toText);
				size=toHistory.size();
				if(idx>=0){//not a new input
					toHistory.remove(idx);
					toHistory.add(toText);
				}else{//a new input
					if (size < Constants.HISTORY_SIZE) { //not reach capacity
						toHistory.add(toText);
					} else {
						toHistory.remove(0);
						toHistory.add(fromText);
					}
				}
				// bind vHandler
				vHandler=new VTIAccountsUpdateHandler();
				locMgr.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.MINTIME, Constants.MINDISTANCE, vHandler);
			}
		});
		
		switchButton = (ImageButton) findViewById(R.id.switchtext);
		switchButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v){
				String fromText=from.getText().toString();
				String toText=to.getText().toString();
				to.setText(fromText);
				from.setText(toText);
			}
		});
		
		fromSpinner = (ImageButton) findViewById(R.id.from_spinner);
		fromSpinner.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				handleSpinner("from");
			}
		});
		
		toSpinner = (ImageButton) findViewById(R.id.to_spinner);
		toSpinner.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				handleSpinner("to");
			}
		});
	}
	
	@Override
    protected void onPause() {
		super.onPause();
		try{
			locMgr.getLocationManager().removeUpdates(mHandler);
		}catch(IllegalArgumentException e){
		}
    }
 
	@Override
    protected void onResume() {
		super.onResume();
		//add the listener again
        locMgr.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.MINTIME, Constants.MINDISTANCE, mHandler);
    }
	
	protected void onDestroy(){
		super.onDestroy();
		try{
			locMgr.getLocationManager().removeUpdates(mHandler);
		}catch(IllegalArgumentException e){
		}
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		try{
			locMgr.getLocationManager().removeUpdates(mHandler);
		}catch(IllegalArgumentException e){
		}
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(Constants.ROUTE_PREFERENCE_FILE, 0);
		SharedPreferences.Editor editor = settings.edit();
		StringBuilder write=new StringBuilder();
		int i;
		//edit fromHistory
		write.delete(0, write.length());
		for(i=0;i<fromHistory.size();i++){
			write.append(fromHistory.get(i));
			if(i<fromHistory.size()-1)
				write.append(Constants.DELIMITER);
		}
		editor.putString(Constants.FROM_HISTORY, write.toString());
		//edit toHistory
		write.delete(0, write.length());
		for(i=0;i<toHistory.size();i++){
			write.append(toHistory.get(i));
			if(i<toHistory.size()-1)
				write.append(Constants.DELIMITER);
		}
		editor.putString(Constants.TO_HISTORY, write.toString());
		//edit vti accounts
		write.delete(0, write.length());
		Object[] itr=vtiAccounts.toArray();
		for(i=0;i<itr.length;i++){
			write.append(itr[i]);
			if(i<vtiAccounts.size()-1)
				write.append(Constants.DELIMITER);
		}
		editor.putString(Constants.LAST_VTI_ACCOUNTS, write.toString());

		// Commit the edits!
		editor.commit();
	}
	
	protected void handleSpinner(final String id){
		final CharSequence[] items;
		int i,size;
		if(id.equals("from")){
			 size=fromHistory.size();
			 items= new CharSequence[size+1];
			 items[0]="Current Location";
			 for(i=1;i<=size;i++)
					items[i]=fromHistory.get(size-i);
		}
		else{
			size=toHistory.size();
			items=new CharSequence[size];
			for(i=0;i<size;i++)
				items[i]=toHistory.get(size-i-1);
		}
		final String fromText=from.getText().toString();
		final String toText=to.getText().toString();
		
		AlertDialog select=new AlertDialog.Builder(RouteSubscription.this)
        .setTitle("Please select from one of the following")
        .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	Log.e(TAG, "select is "+whichButton);
   				if(id.equals("from"))
   					from.setText(items[whichButton]);
   				else
   					to.setText(items[whichButton]);
            }
        })
        .setPositiveButton("Select", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
             }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
   				if(id.equals("from"))
   					from.setText(fromText);
   				else
   					to.setText(toText);
            }
        })
       .create();
		select.show();
	}
	
	protected void selectTransitRoute(int id){
		Log.e(TAG, "id="+id);
		if(id>=0)
			pubRoute=transitRoutes.get(id);
		else // e.g. id==-1, when click the cancel button
			pubRoute=null;
	}
	
	protected ArrayList<String> getVTIAccounts(){
		return vtiAccounts;
	}
	
	private void showTransitRouteDetail(int id) {
		//TODO: better to make a dynamic view
		final Dialog dialog=new Dialog(RouteSubscription.this);
		dialog.setTitle("Route Details");
		dialog.setContentView(R.layout.transit_route_detail);
		TextView detailBox=(TextView)dialog.findViewById(R.id.transitRouteDetail);
		detailBox.setText(transitRoutes.get(id).toString());
		Button backButton=(Button)dialog.findViewById(R.id.back);
		backButton.setOnClickListener(new OnClickListener(){
			public void onClick(final View v){
				dialog.cancel();
			}
		});
		dialog.show();
	} 
	
	private void unsubscribeLastRoute(){
		try{
			locMgr.getLocationManager().removeUpdates(vHandler);
		}catch(IllegalArgumentException e){
		}

		if(vtiAccounts.size()>0){
			new UnFollowAsyncTask().execute();
		}
	}
	
	private void subscribeTransitRoute(ArrayList<String> routes){
		if (routes.size() > 0) {
			transitRoutes=new ArrayList<TransitRoute>();
			//default choice
			final CharSequence[] items = new CharSequence[routes.size()];
			for(int i=0;i<routes.size();i++){
				transitRoutes.add(new TransitRoute(routes.get(i)));
				items[i]=transitRoutes.get(i).getTitle();
			}
			pubRoute=transitRoutes.get(0);
			// Step1: select a route
			AlertDialog select=new AlertDialog.Builder(RouteSubscription.this)
            .setTitle("Please select one of the following routes")
            .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	//Log.e(TAG, "id="+whichButton);
                	showTransitRouteDetail(whichButton);
                	selectTransitRoute(whichButton);
                }
            })
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
       				// Step 2: draw and subscribe a route
       				Log.e(TAG, "Before draw the pubRoute");
       				drawAndSubscribeTransitRoute(pubRoute, Color.BLUE, mapView);
                 }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	selectTransitRoute(whichButton);
                }
            })
           .create();
			select.show();
		} else{
			Toast.makeText(context,ROUTE_QUERY_FAILURE,Toast.LENGTH_LONG).show();
		}
	}

	private boolean subscribePriRoute(String origin, String dest) {
		String accountName;
		if (calculatePriRoute(buildURL(origin,dest,false))) {
			drawPriRoute(priRoute, Color.BLUE, mapView);
			for(int i=0;i<priRoute.size();i++){
				accountName=GeoCoder.determineVTIAccount(priRoute.get(i));
				if(accountName!=null&&!vtiAccounts.contains(accountName)) 
					vtiAccounts.add(accountName);
			}
			Log.e(TAG,"In subscribePriRoute "+vtiAccounts.toString());
			return true;
		} else{
			return false;
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return true;
	}
	
	/*
	 * @param prive identifies the travel mode
	 */
	private String buildURL(String src, String dest, boolean pub){
		// connect to map web service
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.google.com/maps?f=d&hl=en");
		urlString.append("&saddr=");// from
		urlString.append(PercentEncode.encode(src));
		urlString.append("&daddr=");// to
		urlString.append(PercentEncode.encode(dest));
		urlString.append("&ie=UTF8&0&om=0");
		if(!pub)
			urlString.append("&output=kml");
		else
			urlString.append("&dirflg=r&output=html");
		Log.d(TAG, "URL=" + urlString.toString());
		return urlString.toString();
	}
	
	 /*
     * @return all transit routes between two addresses
     * each route is represented by a string
     */

	private ArrayList<String> calculateTransitRoutes(String src, String dest){
		final String delimiter="VTI_BREAK"; 
		ArrayList<String> routes=new ArrayList<String>();
		ArrayList<String> routeTravelTime=new ArrayList<String>();

		org.jsoup.nodes.Document doc,doc1;
		StringBuilder route = new StringBuilder();
		StringBuilder tmp; 
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.google.com/m/directions?");
		urlString.append("&saddr=");// from
		urlString.append(PercentEncode.encode(src));
		urlString.append("&daddr=");// to
		urlString.append(PercentEncode.encode(dest));
		urlString.append("&ie=UTF8&0&om=0");
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdfTime = new SimpleDateFormat("h:mm a");
		Date now=Calendar.getInstance().getTime();
		tmp=urlString;
		
		
		// fetch the first route, to determine the # of alternative routes
		int i;
		tmp.append("&dirflg=r&ri=0&output=html&date="
				+ sdfDate.format(now) + "&time="
				+ sdfTime.format(now).replaceAll(" ", ""));
		Log.e(TAG, tmp.toString());
		try {
			doc = Jsoup.connect(tmp.toString()).get();
			Element directions = doc.select("p").get(2);
			// System.out.println(Jsoup.parse(directions.html()));
			doc1 = Jsoup.parse(Jsoup.parse(directions.html())
					.toString().replaceAll("<br />", delimiter));
			// System.out.println(doc1);
			String[] steps = doc1.text().split(delimiter);
			for (i=0;i<steps.length;i++) {
				if (steps[i].startsWith(" Alternative routes:"))
					break;
				route.append(steps[i].trim() + "\n");
			}
			for(i=i+1;i<steps.length;i++) {
				if(steps[i].contains("-"))// not empty line
					routeTravelTime.add(steps[i].trim());
			}
		}catch (IOException e) {
			Log.d(TAG, Log.stack2string(e));
		}
		// save the first route
		StringBuilder firstRoute=new StringBuilder(route);
		//System.out.println(routeTravelTime.size());
		
		//fetch the alternative routes
		int numberofAlternativeRoutes=routeTravelTime.size(), j;
		for(i=1;i<=numberofAlternativeRoutes;i++){
			tmp=urlString;
			route.delete(0, route.length());
			tmp.append("&dirflg=r&ri="+i+"&output=html&date="
					+ sdfDate.format(now) + "&time="
					+ sdfTime.format(now).replaceAll(" ", ""));
			try {
				doc = Jsoup.connect(tmp.toString()).get();
				Element directions = doc.select("p").get(2);
				// System.out.println(Jsoup.parse(directions.html()));
				doc1 = Jsoup.parse(Jsoup.parse(directions.html())
						.toString().replaceAll("<br />", "delimiter"));
				// System.out.println(doc1);
				String[] steps = doc1.text().split("delimiter");
				for (j=0; j<steps.length;j++) {
					//System.out.println(steps[j]);
					if (steps[j].startsWith(" Alternative routes:")) break;
					route.append(steps[j].trim() + "\n");
				}
				//i==1, need to retrieve the travel time of the first route
				if(i==1)
					routes.add(steps[j+1].trim()+"\n"+firstRoute.toString());
				routes.add(routeTravelTime.get(i-1)+"\n"+route.toString());
			} catch (IOException e) {
				Log.d(TAG, Log.stack2string(e));
			}
		}
		return routes;
	}

	private boolean calculatePriRoute(String urlString) {
		Document doc = null;
		HttpURLConnection urlConnection = null;
		URL url = null;
		try {
			url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.connect();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(urlConnection.getInputStream());

			if (doc.getElementsByTagName("GeometryCollection").getLength() > 0) {
				// String path =
				// doc.getElementsByTagName("GeometryCollection").item(0).getFirstChild().getFirstChild().getNodeName();
				String path = doc.getElementsByTagName("GeometryCollection")
						.item(0).getFirstChild().getFirstChild()
						.getFirstChild().getNodeValue();
				Log.d(TAG, "path=" + path);
				String[] pairs = path.split(" ");
				// lngLat[0]=longitude, lngLat[1]=latitude, lngLat[2]=height
				String[] lngLat = pairs[0].split(",");
				priRoute = new ArrayList<GeoPoint>();
				for (int i = 0; i < pairs.length; i++) {
					lngLat = pairs[i].split(",");
					GeoPoint gp = new GeoPoint(
							(int) (Double.parseDouble(lngLat[1]) * 1E6),
							(int) (Double.parseDouble(lngLat[0]) * 1E6));
					Log.d(TAG, "pair:" + pairs[i]);
					priRoute.add(gp);
				}
			}
		} catch (Exception e) {
			Log.d(TAG, Log.stack2string(e));
			return false;
		}
		return true;
	}
	
	private void drawAndSubscribeTransitRoute(TransitRoute route, int color, MapView mMapView) {
		GeoPoint originPoint=GeoCoder.geocode(PercentEncode.encode(route.getOrigin()));
		GeoPoint destPoint=GeoCoder.geocode(PercentEncode.encode(route.getDest()));
		//remove the old route overlays
		List<Overlay> mapOverlays=mMapView.getOverlays();
		Iterator<Overlay> itr=mapOverlays.iterator();
		while(itr.hasNext()){
			Overlay ins=itr.next();
			if(ins instanceof RouteOverLay){
				itr.remove();
			}
		}
		// add new rotue over lays
		mapOverlays.add(new RouteOverLay(originPoint, originPoint, 1, context));
		ArrayList<Station> stations=route.getTransferStations();
		GeoPoint stationPoint, lastPoint=originPoint;
		String zone_id;
		int mode;
		for (Station station: stations) {
			Log.e(TAG,station.getRouteName()+"  "+station.getStationName());
			if(station.getMode().equals("Subway")){ 			//mode=8 ->train
				mode=8; 
				/**
				 * subscribe to the zone area the station belongs to
				 */ 
				/*
				GeoPoint rp=GeoCoder.geocode(PercentEncode.encode(station.getRouteName()+" "+station.getStationName()+" Chicago"));
				zone_id=null;
				if(rp!=null){
					zone_id=GeoCoder.determineVTIAccount(rp);
					if(zone_id!=null){
						twitterMgr.follow(new String[]{zone_id});
						if(!vtiAccounts.contains(zone_id))
							vtiAccounts.add(zone_id);
					}
				}
				*/
				// subscribe to the train station and the train route the station belongs to
				if(!vtiAccounts.contains(station.getVTIAccount())){
					vtiAccounts.add(station.getVTIAccount());
					twitterMgr.follow(new String[]{station.getVTIAccount()});
					String trainRouteAccountName="vti_"+station.getRouteName().replaceAll(" ","").toLowerCase();
					if(!vtiAccounts.contains(trainRouteAccountName)){
						vtiAccounts.add(trainRouteAccountName);
						twitterMgr.follow(new String[]{trainRouteAccountName});
					}
				}
				Log.e(TAG, station.getRouteName()+"after following the train station account");
			}
			else mode=9; //mode=9 -> bus;
			stationPoint=station.getGeoPoint();
			if(stationPoint!=null){
				mapOverlays.add(new RouteOverLay(stationPoint, stationPoint, mode, context));
				mapOverlays.add(new RouteOverLay(lastPoint, stationPoint, 2, color, context));
				lastPoint=stationPoint;
			}
			// follow the station
		}
		// use the default color
		mapOverlays.add(new RouteOverLay(lastPoint, destPoint, 2, color, context));
		mapOverlays.add(new RouteOverLay(destPoint, destPoint, 1, context));
		mapView.getController().animateTo(originPoint);
		Toast.makeText(context,"Successuflly subscribe to route <From = "+ from.getText().toString()+ ", To = "+ to.getText().toString() + ">",Toast.LENGTH_LONG).show();
	}

	private void drawPriRoute(ArrayList<GeoPoint> route, int color, MapView mMapView) {
		int i;
		//remove the old route overlays
		List<Overlay> mapOverlays=mMapView.getOverlays();
		Iterator<Overlay> itr=mapOverlays.iterator();
		while(itr.hasNext()){
			Overlay ins=itr.next();
			if(ins instanceof RouteOverLay){
				itr.remove();
			}
		}
		// add new route over lays
		GeoPoint startGP=route.get(0); 
		mapOverlays.add(new RouteOverLay(startGP, startGP, 1, context));
			
		for (i=0;i<route.size()-1;i++) {
			mapOverlays.add(new RouteOverLay(route.get(i), route.get(i+1), 2, color, context));
			//Log.d(TAG, "point:" + route.get(i));
		}
		// use the default color
		mapOverlays.add(new RouteOverLay(route.get(i), route.get(i), 1, context));
		mapView.getController().animateTo(priRoute.get(0));
	}
	
	private boolean isOnline() {
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	private class FollowAsyncTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... params) {
			Log.e(TAG, "Follow do in background"+vtiAccounts.toString());
			twitterMgr.follow(vtiAccounts.toArray(new String[1]));
			return null;
		}
	};
	
	private class UnFollowAsyncTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(final Void... params) {
			Log.e(TAG, "UnFollow do in background"+vtiAccounts.toString());
			twitterMgr.unfollow(vtiAccounts.toArray(new String[1]));
			vtiAccounts.clear();
			return null;
		}
	};
	
	private class SubscribePrivateRouteAsyncTask extends AsyncTask<String, Void, Boolean> {
		private ProgressDialog dialog = new ProgressDialog(RouteSubscription.this);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage(WAIT_MESSAGE);
			dialog.show();
		}
		
		@Override
		protected Boolean doInBackground(final String... params) {
			return subscribePriRoute(params[0], params[1]);
		}
		
		@Override
		protected void onPostExecute(Boolean success){
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			if(success){
				new FollowAsyncTask().execute();
				Toast.makeText(context,"Successuflly subscribe to route <From = "+ from.getText().toString()+ ", To = "+ to.getText().toString() + ">",Toast.LENGTH_LONG).show();
			}
			else
				Toast.makeText(context,ROUTE_QUERY_FAILURE,Toast.LENGTH_LONG).show();
		}
	};
	
	private class SubscribeTransitRouteAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {
		private ProgressDialog dialog = new ProgressDialog(RouteSubscription.this);
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			dialog.setMessage(WAIT_MESSAGE);
			dialog.show();
		}
		
		@Override
		protected ArrayList<String> doInBackground(final String... params) {
			return calculateTransitRoutes(params[0], params[1]);
		}
		
		@Override
		protected void onPostExecute(ArrayList<String> routes){
			if (dialog.isShowing()) {
				dialog.dismiss();
			}
			subscribeTransitRoute(routes);
		}
	};

	private class MapUpdateHandler implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			Log.e(TAG, "Within MapUpdatehanlder onLocationChanged: " + location.toString());
			int lat = (int) (location.getLatitude() * 1E6);
			int lng = (int) (location.getLongitude() * 1E6);
			GeoPoint point = new GeoPoint(lat, lng);
			mapController.animateTo(point); // mapController.setCenter(point);

			//add a location overlay and remove the old location overlay
			List<Overlay> mapOverlays=mapView.getOverlays();
			for(Overlay ol: mapOverlays){
				if(ol instanceof LocationOverlay){
					mapOverlays.remove(ol);
					break;
				}
			}
			mapOverlays.add(new LocationOverlay(point, context));
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
	
	private class VTIAccountsUpdateHandler implements LocationListener {
		private int count;
		public VTIAccountsUpdateHandler(){
			count=0;
		}
		
		@Override
		public void onLocationChanged(Location location) {
			Log.e(TAG, "Within VTIAccountUpdatehanlder onLocationChanged: " + location.toString());
			String account=GeoCoder.determineVTIAccount(new GeoPoint((int)(location.getLatitude()*1.0E6),(int)(location.getLongitude()*1.0E6)));
			int idx=vtiAccounts.indexOf(account);
			//current Twitter account is not the first one in the VTIAccounts
			//unfollow the previous account, e.g. given vtiAccounts=<zone_35, zone_36>, once I am in zone_36, unfollow zone_35.
			if(idx>0){
				count++;
				if(count==3){// need at least 3 consecutive gps report within the same zone to assure the position 
					for(int i=0;i<idx;i++)
						twitterMgr.unfollow(new String[]{vtiAccounts.remove(0)});
					Log.e(TAG,"Unfollow account");
					count=0;
				}
			}else
				count=0;
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
}


class RouteOverLay extends Overlay {
	private GeoPoint gp1;
	private GeoPoint gp2;
	/*
	 * @desc 
	 * mode=1:  draw the origin and start point of a private route
	 * mode=2:  draw the lines of a private/transit path
	 * mode=9: 	draw the bus transfer station of a transit route
	 * mode=8:  draw the train transfer station of a transit route
	 */
	private int mode = 0;
	private int defaultColor;
	private Context context;

	public RouteOverLay(GeoPoint gp1, GeoPoint gp2, int mode, Context ctxt) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
		defaultColor = 999; // no defaultColor
		this.context=ctxt;
	}

	public RouteOverLay(GeoPoint gp1, GeoPoint gp2, int mode, int defaultColor, Context ctxt) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
		this.defaultColor = defaultColor;
		this.context=ctxt;
	}
	
	public int getMode() {
		return mode;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
		Projection projection = mapView.getProjection();
		if (shadow == false) {
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			Point point = new Point();
			projection.toPixels(gp1, point);
			Bitmap bmp;
			// mode=1&#65306;start
			if (mode == 1) { 
				if (defaultColor == 999)
					paint.setColor(Color.GREEN);
				else
					paint.setColor(defaultColor);
				//RectF oval = new RectF(point.x - mRadius, point.y - mRadius, point.x + mRadius, point.y + mRadius);
				//canvas.drawOval(oval, paint);
				bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.placemark);
				canvas.drawBitmap(bmp, point.x, point.y, paint);
			}
			// mode=2&#65306;path
			else if (mode == 2) {
				if (defaultColor == 999)
					paint.setColor(Color.RED);
				else
					paint.setColor(defaultColor);
				Point point2 = new Point();
				projection.toPixels(gp2, point2);
				paint.setStrokeWidth(5);
				paint.setAlpha(120);
				canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
			}
			else if (mode == 8 || mode ==9) {
				if (defaultColor == 999)
					paint.setColor(Color.RED);
				else
					paint.setColor(defaultColor);
				if(mode==8)
					bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.metro);
				else
					bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.bus);
				canvas.drawBitmap(bmp, point.x, point.y, paint);
			}
		}
		return super.draw(canvas, mapView, shadow, when);
	}
}

class LocationOverlay extends Overlay {
	GeoPoint curLoc;
	Context context;

	public LocationOverlay(GeoPoint loc, Context ctxt) {
		curLoc = loc;
		context=ctxt;
	}

	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
			long when) {
		super.draw(canvas, mapView, shadow);
		Paint paint = new Paint();
		Point myScreenCoords = new Point();
		// Converts lat/lng-Point to OUR coordinates on the screen.
		mapView.getProjection().toPixels(curLoc, myScreenCoords);
		paint.setStrokeWidth(5);
		paint.setARGB(255, 255, 255, 255);
		paint.setStyle(Paint.Style.STROKE);
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.current_location);
		canvas.drawBitmap(bmp, myScreenCoords.x, myScreenCoords.y, paint);
		//canvas.drawText("Here I am...", myScreenCoords.x, myScreenCoords.y, paint);
		return true;
	}
}
