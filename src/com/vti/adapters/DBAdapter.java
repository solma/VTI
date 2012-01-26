/**
 * Copyright 2011 Sol Ma
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.vti.adapters;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.vti.utils.Log;

public class DBAdapter
{
	
	private static final String TAG = DBAdapter.class.getSimpleName();

	
	public static final String KEY_TWITID = "_id";
	public static final String KEY_TIMESTAMP="timestamp";
	public static final String KEY_PROFILE_NAME = "profile_name";
	public static final String KEY_PROFILE_IMAGE_URL = "profile_image";
	public static final String KEY_TWIT_MESSAGE = "message";
	public static final String KEY_UP_THUMBS = "up_thumbs";
	public static final String KEY_DOWN_THUMBS = "down_thumbs";
	
	private static final String DATABASE_NAME = "vti";
	//legacy name
	private static final String DATABASE_TABLE = "notifications";
	
	private static final String CTA_ROUTE="cta_route";
	private static final String CTA_STOP="cta_stop";
	
	private static final int DATABASE_VERSION = 1;


	private static final String CREATE_NOTIFICATION_TABLE = 
			"create table "+ DATABASE_TABLE +" ("+KEY_TWITID+" integer primary key , "+ KEY_TIMESTAMP + " integer not null, " 
			+ KEY_PROFILE_NAME+" text not null, "+KEY_PROFILE_IMAGE_URL+" text not null, " + KEY_TWIT_MESSAGE+" text not null, "
			+ KEY_UP_THUMBS+" integer not null, "+ KEY_DOWN_THUMBS+ " integer not null) ;";
	
	
	private final Context context;

	private final DatabaseHelper DBHelper;
	private final CTATrackerDatabaseHelper CTATrackerDBHelper;
	private SQLiteDatabase db;

	public CTATrackerDatabaseHelper getCTATrackerDBHelper(){
		return CTATrackerDBHelper;
	}
	
	public DBAdapter(final Context ctx)
	{
		context = ctx;
		DBHelper = new DatabaseHelper(context);
		CTATrackerDBHelper=new CTATrackerDatabaseHelper(context);
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(final Context context)
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}


		@Override
		public void onCreate(final SQLiteDatabase db)
		{
			db.execSQL(CREATE_NOTIFICATION_TABLE);
		}


		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion)
		{
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS titles");
			onCreate(db);
		}
	}

	public static class CTATrackerDatabaseHelper extends SQLiteOpenHelper
	{
		//The Android's default system path of your application database.
	    private static String DB_PATH = "/data/data/com.vti/databases/";
	    private static String DB_NAME = "cta_tracker";
	    private SQLiteDatabase myDataBase; 
	    private final Context myContext;
	 
	    CTATrackerDatabaseHelper(final Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.myContext = context;
		}

		@Override
		public void onCreate(final SQLiteDatabase db){
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion){
		}
	
	  /**
	     * Creates a empty database on the system and rewrites it with your own database.
	     * */
	    public void createCTADatabase() throws IOException{
	    	boolean dbExist = checkCTADatabase();
	    	if(dbExist){
	    		Log.e(TAG, "do nothing - database already exist");
	    	}else{
	    		//By calling this method and empty database will be created into the default system path
	            //of your application so we are gonna be able to overwrite that database with our database.
	        	this.getReadableDatabase();
	        	try {
	        		Log.e(TAG, "Before copying database");
	    			copyCTADatabase();
	    		} catch (IOException e) {
	        		Log.e(TAG, "Error copying database");
	        	}
	    	}
	    }
	 
	    /**
	     * Check if the database already exist to avoid re-copying the file each time you open the application.
	     * @return true if it exists, false if it doesn't
	     */
	    private boolean checkCTADatabase(){
	    	File dbFile = new File(DB_PATH + DB_NAME);
	    	return dbFile.exists();
	    }
	 
	    /**
	     * Copies your database from your local assets-folder to the just created empty database in the
	     * system folder, from where it can be accessed and handled.
	     * This is done by transfering bytestream.
	     * */
	    private void copyCTADatabase() throws IOException{
	    	//Open your local db as the input stream
	    	InputStream myInput = myContext.getAssets().open(DB_NAME);
	    	// Path to the just created empty db
	    	String outFileName = DB_PATH + DB_NAME;
	    	//Open the empty db as the output stream
	    	OutputStream myOutput = new FileOutputStream(outFileName);
	    	//transfer bytes from the inputfile to the outputfile
	    	byte[] buffer = new byte[1024];
	    	int length;
	    	while ((length = myInput.read(buffer))>0){
	    		myOutput.write(buffer, 0, length);
	    	}
	    	//Close the streams
	    	myOutput.flush();
	    	myOutput.close();
	    	myInput.close();
	    }
	 
	    public void openDataBase() throws SQLException{
	    	//Open the database
	        String myPath = DB_PATH + DB_NAME;
	    	myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
	    }
	 
	    @Override
		public synchronized void close() {
			if (myDataBase != null)
				myDataBase.close();
			super.close();
		}

        // Add your public helper methods to access and get content from the database.
       // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
       // to you to create adapters for your views.
		// ---retrieves all the Bus Routes---
		public Cursor getAllRoutes()
		{
			return myDataBase.query(CTA_ROUTE, new String[] { "_id", "rtName", "rtDir", "isBus" }, null, null, null, null, "_id ASC");
		}
		
		// ---retrieves all the Stops of the selected Route-
		public Cursor getAllStops(String rtId, String rtDir)
		{
			return myDataBase.query(CTA_STOP, new String[] { "_id", "stpName", "stpLat", "stpLon", "rtId", "rtDir", "isBus" }, "rtId='"+rtId+"' and rtDir='"+rtDir+"'", null, null, null, "_id ASC");
		}
		
		// ---retrieves all the Stops-
		public Cursor getAllStops()
		{
			return myDataBase.query(CTA_STOP, new String[] { "_id", "stpName", "stpLat", "stpLon", "rtId", "rtDir", "isBus" }, null, null, null, null, "_id ASC");
		}
	}

	// ---opens the database---
	public DBAdapter open() throws SQLException
	{
		db = DBHelper.getWritableDatabase();
		return this;
	}


	// ---closes the database---
	public void close()
	{
		DBHelper.close();
	}
	
	// ---insert a twit into the database---
	public long insertTwit(final long twitId, final long timestamp, final String profileName, final String profileImageUri, final String twitMessage, final long upThumbs, final long downThumbs)
	{
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TWITID, twitId);
		initialValues.put(KEY_TIMESTAMP, timestamp);
		initialValues.put(KEY_PROFILE_NAME, profileName);
		initialValues.put(KEY_PROFILE_IMAGE_URL, profileImageUri);
		initialValues.put(KEY_TWIT_MESSAGE, twitMessage);
		initialValues.put(KEY_UP_THUMBS, upThumbs );
		initialValues.put(KEY_DOWN_THUMBS, downThumbs );
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	// ---update a twit into the database---
	public long updateTwit(final long twitId, final long timestamp, final String profileName, final String profileImageUri, final String twitMessage, final long upThumbs, final long downThumbs)
	{
		final ContentValues updatedValues = new ContentValues();
		updatedValues.put(KEY_TWITID, twitId);
		updatedValues.put(KEY_TIMESTAMP, timestamp);
		updatedValues.put(KEY_PROFILE_NAME, profileName);
		updatedValues.put(KEY_PROFILE_IMAGE_URL, profileImageUri);
		updatedValues.put(KEY_TWIT_MESSAGE, twitMessage);
		updatedValues.put(KEY_UP_THUMBS, upThumbs );
		updatedValues.put(KEY_DOWN_THUMBS, downThumbs );
		return db.update(DATABASE_TABLE, updatedValues, KEY_TWITID + "=" + twitId, null) ;
	}

	// ---deletes a particular twits---
	public boolean deleteTwit(final long twitId)
	{
		return db.delete(DATABASE_TABLE, KEY_TWITID + "=" + twitId, null) > 0;
	}


	// ---retrieves all the twits---
	public Cursor getAllTwits()
	{
		return db.query(DATABASE_TABLE, new String[] { KEY_TWITID, KEY_TIMESTAMP, KEY_PROFILE_NAME, KEY_PROFILE_IMAGE_URL, KEY_TWIT_MESSAGE, KEY_UP_THUMBS, KEY_DOWN_THUMBS }, null, null, null,
				null, KEY_TWITID + " DESC");
	}
	
	
}