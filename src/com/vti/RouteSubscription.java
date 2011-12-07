package com.vti;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
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
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.vti.adapters.URLAdapter;

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
	private Geocoder geocoder;
	private Button directionButton;
	private Button subscribeButton;
	private ImageButton switchButton;
	private RadioButton priv;
	private RadioButton pub;
	private EditText from;
	private EditText to;
	
	private ArrayList<GeoPoint> route;
	private Context context;
	
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.route); // bind the layout to the activity
		//TODO: this is not right
		context = getApplicationContext();

		// create a map view
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setStreetView(true);
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

		directionButton = (Button) findViewById(R.id.direction);
		directionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if(priv.isChecked()){
					if (calculatePriRoute(buildURL(from.getText().toString(),to.getText().toString(), false))) {
						drawPriRoute(route, Color.GREEN, mapView);
						mapView.getController().animateTo(route.get(0));
					} else{
						Log.e(TAG, ROUTE_QUERY_FAILURE);
						Toast.makeText(context, ROUTE_QUERY_FAILURE, Toast.LENGTH_SHORT).show();
					}
				}
					
					
			}
		});

		subscribeButton = (Button) findViewById(R.id.subscribe);
		subscribeButton.setOnClickListener(new OnClickListener() {
			public void onClick(final View v) {
				if(priv.isChecked()){
					if (calculatePriRoute(buildURL(from.getText().toString(),to.getText().toString(),false))) {
						if (subscribeToRoute(route)) {
							Toast.makeText(context,
										"Successuflly subscribe to route <From = "
												+ from.getText().toString()
												+ " To = "
												+ to.getText().toString() + ">",
										Toast.LENGTH_SHORT).show();
						} else
							Toast.makeText(context, ROUTE_SUBSCRIBE_FAILURE, Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(context,ROUTE_QUERY_FAILURE,Toast.LENGTH_SHORT).show();
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

	protected boolean subscribeToRoute(ArrayList<GeoPoint> route) {
		Boolean ret = true;
		return ret;
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
		urlString.append(URLAdapter.encode(src));
		urlString.append("&daddr=");// to
		urlString.append(URLAdapter.encode(dest));
		urlString.append("&ie=UTF8&0&om=0");
		if(!pub)
			urlString.append("&output=kml");
		else
			urlString.append("&dirflg=r&output=html");
		Log.d(TAG, "URL=" + urlString.toString());
		return urlString.toString();
	}
	
	private boolean calculatePubRoute(String urlString) {
		URL url = null;
		InputStream is = null;
		DataInputStream dis;
		String s;
		try {
			url = new URL(urlString);
			is = url.openStream(); // throws an IOException
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				System.out.println(s);
			}
			is.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
				route = new ArrayList<GeoPoint>();
				for (int i = 0; i < pairs.length; i++) {
					lngLat = pairs[i].split(",");
					GeoPoint gp = new GeoPoint(
							(int) (Double.parseDouble(lngLat[1]) * 1E6),
							(int) (Double.parseDouble(lngLat[0]) * 1E6));
					Log.d(TAG, "pair:" + pairs[i]);
					route.add(gp);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
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
		mapOverlays.add(new RouteOverLay(startGP, startGP, 1));
			
		for (i=0;i<route.size()-1;i++) {
			mapOverlays.add(new RouteOverLay(route.get(i), route.get(i+1), 2, color));
			Log.d(TAG, "point:" + route.get(i));
		}
		// use the default color
		mapOverlays.add(new RouteOverLay(route.get(i), route.get(i), 3));
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
	private int mRadius = 6;
	private int mode = 0;
	private int defaultColor;
	private String text = "";
	private Bitmap img = null;

	public RouteOverLay(GeoPoint gp1, GeoPoint gp2, int mode) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
		defaultColor = 999; // no defaultColor
	}

	public RouteOverLay(GeoPoint gp1, GeoPoint gp2, int mode, int defaultColor) {
		this.gp1 = gp1;
		this.gp2 = gp2;
		this.mode = mode;
		this.defaultColor = defaultColor;
	}

	public void setText(String t) {
		this.text = t;
	}

	public void setBitmap(Bitmap bitmap) {
		this.img = bitmap;
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
			// mode=1&#65306;start
			if (mode == 1) {
				if (defaultColor == 999)
					paint.setColor(Color.BLUE);
				else
					paint.setColor(defaultColor);
				RectF oval = new RectF(point.x - mRadius, point.y - mRadius,
						point.x + mRadius, point.y + mRadius);
				// start point
				canvas.drawOval(oval, paint);
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
			/* mode=3&#65306;end */
			else if (mode == 3) {
				/* the last path */
				if (defaultColor == 999)
					paint.setColor(Color.GREEN);
				else
					paint.setColor(defaultColor);
				Point point2 = new Point();
				projection.toPixels(gp2, point2);
				paint.setStrokeWidth(5);
				paint.setAlpha(120);
				canvas.drawLine(point.x, point.y, point2.x, point2.y, paint);
				RectF oval = new RectF(point2.x - mRadius, point2.y - mRadius,
						point2.x + mRadius, point2.y + mRadius);
				/* end point */
				paint.setAlpha(255);
				canvas.drawOval(oval, paint);
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
