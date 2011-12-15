
package com.vti;

/**
 * Constants for interval settings
 * 
 */
public class Constants {
	// Server Related
	public static final String SERVER_IP="67.167.207.236";
	public static final int SERVER_PORT='V'+'T'+'I';
	public static final String VOTE_ERROR = "Failed to vote because cannot connect to the server.";
	public static final String VOTE_SUCCESS = "Vote succeed!";
	public static final String REPEAT_VOTE = "Already voted, cannot vote repeatly.";
	
	// Miscellaneous
	public static final String SETTING_VALUES = "SettingValues";
	public static final String REFRESH_INTERVAL = "RefreshInterval";
	
	
	// Time Units
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
	public static final long MINTIME=30000; 
	// the minimum distance interval for notifications, in meters
	public static final float MINDISTANCE=100;

	
	// Twitter Application Info.
	public static final String CONSUMER_KEY="UJxOUdtJm8p3wEOFatp1Q";
	public static final String CONSUMER_SECRET ="6wIgL90ZKeWPk7G1y0QfztkSm13NiD2Rk3v5Lf7XAg";
	//account name that creates this application
	public static final String CONSUMER_ACCOUNT = "Sol_Ma";
	public static final String CALLBACK_URL = "VTI://twitter";
	
	// Twitter account relevant 
	public static final int TWITTER_USER_NAME_LENGTH_LIMIT=15;
	// ZONE parameters, caclulated by vti_damon/utils/GeocodeAdapter
	public static final double ZONE_LATITUDE=14688.169999999553;
	public static final double ZONE_LONGITUDE=11434.390000000596;
	public static final double WEST=-87.7239589;
	public static final double SOUTH=41.807737;
}
