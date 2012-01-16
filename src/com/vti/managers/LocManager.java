package com.vti.managers;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.vti.Constants;
import com.vti.utils.Log;

public class LocManager {
	private static final String TAG=LocManager.class.getSimpleName();
	private static final int UPDATE_RATE = (int) (Constants.ONE_MINUTE * 2);
	private Context context;
	private Location latestLocation;
	private LocationManager locationMgr;
	
	public Location getLatestLocation() {
		for(String provider: locationMgr.getProviders(true)){
			Location location=locationMgr.getLastKnownLocation(provider);
			if(location!=null){				
				latestLocation = isBetterLocation(location, latestLocation) == true ? location: latestLocation;
				Log.d(TAG,latestLocation.getLatitude()+" "+latestLocation.getLongitude());
			}
		}
		// if the lastest location is too stale, the return null;
		if(System.currentTimeMillis()-latestLocation.getTime()>Constants.MINTIME)
			return null;
		return latestLocation;
	}
	
	public Location getLastLocation(){
		return latestLocation;
	}
	
	public LocationManager getLocationManager(){
		return locationMgr;
	}
	
	public LocManager(Context ctxt) {
		this.context = ctxt;
		// latestLocation=null;
		latestLocation = new Location(LocationManager.GPS_PROVIDER);
		// Acquire a reference to the system Location Manager
		locationMgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	public boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > UPDATE_RATE;
		boolean isSignificantlyOlder = timeDelta < -UPDATE_RATE;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

}
