
package com.vti;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import android.util.Log;

/**
 * Constants for interval settings
 * 
 */
public class Constants {
	private static final String TAG=Constants.class.getSimpleName();
	/**
	 * Internet related
	 */
	public static String SERVER_IP="67.167.207.236";
	{
		URL serverIP;
		String inputLine, ip=null;
		try {
			serverIP = new URL("http://www.cs.uic.edu/~sma/VTI/serverIP.txt");
			BufferedReader in = new BufferedReader(new InputStreamReader(serverIP.openStream()));
			while ((inputLine = in.readLine()) != null)
				ip = inputLine;
			in.close();
		} catch (Exception e) {
			Log.e(TAG, "fail to update server IP");
		} finally{
			if(ip!=null)
				SERVER_IP=ip;
		}
	}
	public static final String ACCOUNTS_LIST="http://www.cs.uic.edu/~sma/VTI/accounts.xml";
	public static final int SERVER_PORT='V'+'T'+'I';
	public static final String VOTE_ERROR = "Failed to vote because cannot connect to the server.";
	public static final String VOTE_SUCCESS = "Vote succeed!";
	public static final String REPEAT_VOTE = "Already voted, cannot vote repeatly.";
	public static final String INTERNET_NOT_AVAILABLE="Need Internet connection.";
	public static final String ACCOUNT_LIST_NOT_AVAILABLE="VTI account list is not available for the moment, please try again later.";
	
	/**
	 * SharedPreference files and keys names
	 */
	public static final String SETTING_VALUES = "SettingValues";
	public static final String REFRESH_INTERVAL = "RefreshInterval";
	// OAuth authorization 
	public static final String AUTHORIZATION_PREFERENCE_FILE = "OAuthAccessTokens";
	public static final String ACCESS_TOKEN = "AccessToken";
	public static final String TOKEN_SECRET = "TokenSecret";
	//route related settings
	public static final String DELIMITER="VTI_BREAK";
	public static final String FROM_HISTORY="FromHistory";
	public static final String TO_HISTORY="ToHistory";
	public static final int HISTORY_SIZE=10;
	public static final String LAST_VTI_ACCOUNTS="LastVTIAccounts";
	public static final String ROUTE_PREFERENCE_FILE="RouteSubscription";
	// other settings
	public static final String SETTING_PREFERENCE_FILE="SettingPreferenceFile";
	public static final String VOICE_NOTIFY="VoiceNotify";
	public static final String UPDATE_FREQUENCY="UpdateFrequency";
	
	/** 
	 * Time Units
	 */
	public static final int THREE_SECONDS= 1000 * 3;
	public static final long TEN_SECONDS = 1000 * 10;
	public static final long ONE_MINUTE = TEN_SECONDS * 6;
	public static final long FIVE_MINUTE = ONE_MINUTE * 5;
	public static final long THIRTY_MINUTE = ONE_MINUTE * 30;
	
	/**
	 * GPS Settings
	 */
	//  the minimum time interval for notifications, in milliseconds. This field is only used as a hint to conserve power, a
	// nd actual time between location updates may be greater or lesser than this value.
	public static final long MINTIME=ONE_MINUTE/2; 
	// the minimum distance interval for notifications, in meters
	public static final float MINDISTANCE=100;
	
	/** 
	 * Twitter Application Info.
	 */
	public static final String CONSUMER_KEY="UJxOUdtJm8p3wEOFatp1Q";
	public static final String CONSUMER_SECRET ="6wIgL90ZKeWPk7G1y0QfztkSm13NiD2Rk3v5Lf7XAg";
	//account name that creates this application
	public static final String CONSUMER_ACCOUNT = "Sol_Ma";
	public static final String CALLBACK_URL = "VTI://twitter";
	//Twitter account relevant 
	public static final int TWITTER_USER_NAME_LENGTH_LIMIT=15;
	
	/**
	 * Geographical Information
	 */
	// ZONE parameters, caclulated by vti_damon/utils/GeocodeAdapter
	public static final double ZONE_LATITUDE=14688.169999999553;
	public static final double ZONE_LONGITUDE=11434.390000000596;
	public static final double WEST=-87.7239589;
	public static final double SOUTH=41.807737;
	public static final double EAST=-87.609615;
	public static final double NORTH=41.9546187;
	public static final int EDGE_SIZE=5; //each edge is eqully partitioned into 5 sections
	
}
