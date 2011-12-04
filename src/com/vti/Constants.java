
package com.vti;

/**
 * Constants for interval settings
 * 
 */
public class Constants {
	// Program Internal Messages
	public static final String RATE_SERVER="67.167.207.236";
	public static final String SETTING_VALUES = "SettingValues";
	public static final String REFRESH_INTERVAL = "RefreshInterval";
	public static final String VOTE_ERROR = "Failed to vote because cannot connect to the server.";
	
	// Time Units
	public static int FIVE_SECONDS= 1000 * 5;
	public static long TEN_SECONDS = 1000 * 10;
	public static long ONE_MINUTE = TEN_SECONDS * 6;
	public static long FIVE_MINUTE = ONE_MINUTE * 5;
	public static long THIRTY_MINUTE = ONE_MINUTE * 30;
	
	// Twitter Application Info.
	public static final String CONSUMER_KEY="UJxOUdtJm8p3wEOFatp1Q";
	public static final String CONSUMER_SECRET ="6wIgL90ZKeWPk7G1y0QfztkSm13NiD2Rk3v5Lf7XAg";
	//account name that creates this application
	public static final String CONSUMER_ACCOUNT = "Sol_Ma";
	public static final String CALLBACK_URL = "VTI://twitter";
}
