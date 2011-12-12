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
import android.content.Context;
import android.content.DialogInterface;
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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.vti.managers.AccountManager;
import com.vti.managers.TwitterManager;
import com.vti.model.Station;
import com.vti.model.TransitRoute;
import com.vti.utils.GeoCoder;
import com.vti.utils.PercentEncode;

public class RouteSubscription extends MapActivity {
	private static final String TAG = RouteSubscription.class.getSimpleName();
	private static final String ROUTE_QUERY_FAILURE="Cannot find a route given the From-To pair.";
	private static final String ROUTE_SUBSCRIBE_FAILURE="Failed to subscribe to the route.";
	
	/**
	 * UI widgets
	 */
	private MapController mapController;
	private MapView mapView;
	private LocationManager locationManager;
	private Button unsubscribeButton;
	private Button subscribeButton;
	private ImageButton switchButton;
	private RadioButton priv;
	private RadioButton pub;
	private EditText from;
	private EditText to;
	private Spinner fromSpinner;
	private Spinner toSpinner;
	
	private ArrayList<GeoPoint> priRoute;
	private TransitRoute pubRoute;
	private ArrayList<TransitRoute> transitRoutes;
	private Context context;
	private ArrayList<String> vtiAccounts;
	private TwitterManager twitterManager;
	
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.route); // bind the layout to the activity
		context = getApplicationContext();
		twitterManager=new TwitterManager(getApplicationContext());
		vtiAccounts=new ArrayList<String>();
		
		// create a map view
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setStreetView(true);
		mapView.setTraffic(true);
		mapController = mapView.getController();
		mapController.setZoom(14); // Zoom 1 is world view
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		String bestProvider=locationManager.getBestProvider(criteria, true);
		Location lastKnownLocation=locationManager.getLastKnownLocation(bestProvider);
		
		locationManager.requestLocationUpdates(bestProvider, 0, 0, new GeoUpdateHandler());

		priv=(RadioButton) findViewById(R.id.priv);
		pub=(RadioButton) findViewById(R.id.pub);
		

		from = (EditText) findViewById(R.id.from_text);
		to = (EditText) findViewById(R.id.to_text);

		unsubscribeButton = (Button) findViewById(R.id.unsubscribe);
		unsubscribeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				unsubscribeLastRoute();
			}
		});

		subscribeButton = (Button) findViewById(R.id.subscribe);
		subscribeButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				String origin=from.getText().toString().trim()+",Chicago";
				String dest=to.getText().toString().trim()+",Chicago";
				if(priv.isChecked()){
					subscribePriRoute(origin, dest);
				}else{
					subscribeTransitRoute(origin, dest);
				}
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
	
	protected void showTransitRouteDetail(int id) {
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
	
	protected void unsubscribeLastRoute(){
		final AccountManager authMgr = twitterManager.getOAuthMgr();
		if (!authMgr.isAccountEmpty()) {
			for(String account: vtiAccounts)
				twitterManager.unfollow(account);
		}
	}
	
	protected boolean subscribeTransitRoute(String origin, String dest){
		ArrayList<String> routes = calculateTransitRoutes(origin, dest);
		if (routes.size() > 0) {
			transitRoutes=new ArrayList<TransitRoute>();
			final CharSequence[] items = new CharSequence[routes.size()];
			for(int i=0;i<routes.size();i++){
				transitRoutes.add(new TransitRoute(routes.get(i)));
				items[i]=transitRoutes.get(i).getTitle();
			}
			//default choice
			pubRoute=transitRoutes.get(0);
			// Step1: select a route
			AlertDialog select=new AlertDialog.Builder(RouteSubscription.this)
            .setTitle("Please select one of the following routes")
            .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
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
			
			return true;
		} else{
			Toast.makeText(context,ROUTE_QUERY_FAILURE,Toast.LENGTH_SHORT).show();
			return false;
		}
	}

	protected void subscribePriRoute(String origin, String dest) {
		if (calculatePriRoute(buildURL(origin,dest,false))) {
			drawPriRoute(priRoute, Color.BLUE, mapView);
			ArrayList<String> vtiAccounts=new ArrayList<String>();
			String streetName, streetNumber;
			Element e;
			for(GeoPoint point: priRoute){
				streetName=streetNumber="";
				Double la=(Double)(point.getLatitudeE6()/1.0E6);
				Double ln=(Double)(point.getLongitudeE6()/1.0E6);
				String url="http://maps.googleapis.com/maps/api/geocode/xml?address="+la+"+"+ln+"&sensor=true";
				try {
					org.jsoup.nodes.Document doc=Jsoup.connect(url).get();
					if(doc!=null){
						streetName=doc.select("result > address_component > type:matches(route)").parents().first().select("long_name").text();
						e=doc.select("result > address_component > type:matches(street_number)").first();
						if(e!=null)
							streetNumber=e.parents().first().select("long_name").text();
						Log.d(TAG,streetName+" "+streetNumber);
					}
					else
						Log.d(TAG,la+" "+ln);
				} catch (IOException ex) {
					Log.d(TAG,la+" "+ln);
					ex.printStackTrace();
				}
			}
			Toast.makeText(context,
							"Successuflly subscribe to route <From = "
									+ from.getText().toString()
									+ " To = "
									+ to.getText().toString() + ">",
							Toast.LENGTH_SHORT).show();

		} else
			Toast.makeText(context,ROUTE_QUERY_FAILURE,Toast.LENGTH_SHORT).show();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	private String buildURL(GeoPoint src, GeoPoint dest, boolean pub){
		// connect to map web service
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.google.com/maps?f=d&hl=en");
		urlString.append("&saddr=");// from
		urlString.append(Double.toString((double) src.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString.append(Double.toString((double) src.getLongitudeE6() / 1.0E6));
		urlString.append("&daddr=");// to
		urlString.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString.append(Double.toString((double) dest.getLongitudeE6() / 1.0E6));
		urlString.append("&ie=UTF8&0&om=0");
		if(!pub)
			urlString.append("&output=kml");
		else
			urlString.append("&dirflg=r&output=html");
		Log.d(TAG, "URL=" + urlString.toString());
		return urlString.toString();
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
			e.printStackTrace();
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
				e.printStackTrace();
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
			e.printStackTrace();
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
		int mode;
		for (Station station: stations) {
			Log.e(TAG,station.getRouteName()+"  "+station.getStationName());
			if(station.getMode().equals("Subway")){ 			//mode=8 ->train
				mode=8; 
				// only subscribe to train station
				twitterManager.follow(station.getVTIAccount());
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
		// add new rotue over lays
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

	public class GeoUpdateHandler implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			Log.e(TAG, "onLocationChanged with location " + location.toString());
			int lat = (int) (location.getLatitude() * 1E6);
			int lng = (int) (location.getLongitude() * 1E6);
			GeoPoint point = new GeoPoint(lat, lng);
			mapController.animateTo(point); // mapController.setCenter(point);
			
			/*
			List<Address> addresses;
			try {
				addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);
				for (Address address : addresses) {
					Log.e(TAG, address.getAddressLine(0));
				}
			} catch (IOException e) {
				Log.e("LocateMe", "Could not get Geocoder data", e);
				e.printStackTrace();
			} 
			*/

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
		Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.placemark);
		canvas.drawBitmap(bmp, myScreenCoords.x, myScreenCoords.y, paint);
		//canvas.drawText("Here I am...", myScreenCoords.x, myScreenCoords.y, paint);
		return true;
	}
}
