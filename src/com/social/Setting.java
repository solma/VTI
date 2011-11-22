/**
 * Copyright Quickoffice, Inc, 2005-2009
 * 
 * NOTICE: The intellectual and technical concepts contained herein are proprietary to Quickoffice, Inc. and is
 * protected by trade secret and copyright law. Dissemination of any of this information or reproduction of this
 * material is strictly forbidden unless prior written permission is obtained from Quickoffice, Inc.
 * 
 * Created: Jun 10, 2011 Author: (sg)
 * 
 */

package com.social;

/**
 * Constants for interval settings
 * 
 */
public class Setting {
	public static final String SETTING_VALUES = "SettingValues";
	public static final String REFRESH_INTERVAL = "RefreshInterval";
	public static long TEN_SECONDS = 1000 * 10;
	public static long ONE_MINUTE = TEN_SECONDS * 6;
	public static long FIVE_MINUTE = ONE_MINUTE * 5;
	public static long THIRTY_MINUTE = ONE_MINUTE * 30;
}
