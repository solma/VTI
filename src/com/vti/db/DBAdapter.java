/**
 * Copyright 2011 Saurabh Gangarde & Rohit Ghatol (http://code.google.com/p/droidtwit/)
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
package com.vti.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter
{
	
	private static final String TAG = DBAdapter.class.getSimpleName();

	
	public static final String KEY_TWITID = "_id";
	public static final String KEY_PROFILE_NAME = "profile_name";
	public static final String KEY_PROFILE_IMAGE_URL = "profile_image";
	public static final String KEY_TWIT_MESSAGE = "message";
	public static final String KEY_UP_THUMBS = "up_thumbs";
	public static final String KEY_DOWN_THUMBS = "down_thumbs";
	
	private static final String DATABASE_NAME = "vti";
	private static final String DATABASE_TABLE = "notifications";
	
	private static final int DATABASE_VERSION = 1;

	private static final String DATABASE_CREATE = "create table "+DATABASE_TABLE+" ("+KEY_TWITID+" integer primary key , "
			+ KEY_PROFILE_NAME+" text not null, "+KEY_PROFILE_IMAGE_URL+" text not null, " + KEY_TWIT_MESSAGE+" text not null, "
			+ KEY_UP_THUMBS+" integer not null, "+ KEY_DOWN_THUMBS+ " integer not null) ;";

	private final Context context;

	private final DatabaseHelper DBHelper;
	private SQLiteDatabase db;


	public DBAdapter(final Context ctx)
	{
		context = ctx;
		DBHelper = new DatabaseHelper(context);
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
			db.execSQL(DATABASE_CREATE);
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
	public long insertTwit(final long twitId,final String profileName, final String profileImageUri, final String twitMessage, final long upThumbs, final long downThumbs)
	{
		final ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_TWITID, twitId);
		initialValues.put(KEY_PROFILE_NAME, profileName);
		initialValues.put(KEY_PROFILE_IMAGE_URL, profileImageUri);
		initialValues.put(KEY_TWIT_MESSAGE, twitMessage);
		initialValues.put(KEY_UP_THUMBS, upThumbs );
		initialValues.put(KEY_DOWN_THUMBS, downThumbs );
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	// ---insert a twit into the database---
	public long updateTwit(final long twitId,final String profileName, final String profileImageUri, final String twitMessage, final long upThumbs, final long downThumbs)
	{
		final ContentValues updatedValues = new ContentValues();
		updatedValues.put(KEY_TWITID, twitId);
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
		return db.query(DATABASE_TABLE, new String[] { KEY_TWITID, KEY_PROFILE_NAME, KEY_PROFILE_IMAGE_URL, KEY_TWIT_MESSAGE, KEY_UP_THUMBS, KEY_DOWN_THUMBS }, null, null, null,
				null, KEY_TWITID + " DESC");
	}


	
}