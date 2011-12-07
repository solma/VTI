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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vti.Constants;
import com.vti.R;
import com.vti.managers.AccountManager;
import com.vti.managers.DrawableManager;
import com.vti.managers.FeedManager;
import com.vti.model.Twit;

/**
 * @author (sg)
 * 
 */
public class TwitAdapter extends BaseAdapter {
	private static final String TAG = TwitAdapter.class.getSimpleName();

	private List<Twit> socialFeed;
	private Context context;
	private Twitter twitter;
	private String voterName;
	private DrawableManager drawableManager = new DrawableManager();

	/**
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public TwitAdapter(final Context context, List<Twit> socialFeed) {
		super();
		this.socialFeed = socialFeed;
		this.context = context;
		
		AccountManager authMgr = new AccountManager(context);
		if (!authMgr.isAccountEmpty()){
			twitter = authMgr.getTwitterFactory().getInstance(); 
		}
		try {
			voterName=twitter.getScreenName();
			Log.d(TAG, twitter.getOAuthAccessToken().toString());
			Log.d(TAG, voterName);
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View,
	 * android.view.ViewGroup)
	 */

	public View getView(final int position, final View convertView,
			final ViewGroup parent) {
		View row = convertView;
		if (row == null) {
			final LayoutInflater vi = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = vi.inflate(R.layout.list_item, null);
		}

		final Twit twit = (Twit) getItem(position);

		final ImageView profileImage = (ImageView) row
				.findViewById(R.id.profileImage);
		drawableManager.fetchDrawableOnThread(twit.getImageUrl(), profileImage);

		final TextView profileName = (TextView) row
				.findViewById(R.id.profileName);
		profileName.setText(twit.getProfileName());
		//Log.d(TAG, "Profile name= " + twit.getProfileName() + " Message= "+ twit.getTwitMessage());

		final TextView twitMessage = (TextView) row
				.findViewById(R.id.twitMessage);
		twitMessage.setText(twit.getTwitMessage());

		// set up/down Thumbs button visible
		final ImageButton upThumbs_button = (ImageButton) row
				.findViewById(R.id.upThumbsPic);
		final ImageButton downThumbs_button = (ImageButton) row
				.findViewById(R.id.downThumbsPic);
		upThumbs_button.setVisibility(View.VISIBLE);
		downThumbs_button.setVisibility(View.VISIBLE);

		final TextView upThumbs = (TextView) row.findViewById(R.id.upThumbsNum);
		upThumbs.setText(Long.toString(twit.getUpThumbs()));
		final TextView downThumbs = (TextView) row
				.findViewById(R.id.downThumbsNum);
		upThumbs.setText(Long.toString(twit.getDownThumbs()));

		// update the # of upThumb votes and the # of downThumb votes
		upThumbs_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				InetAddress addr = null;
				SocketAddress sockaddr = null;
				PrintWriter out = null;
				BufferedReader in = null;
				Socket clientSocket = new Socket();

				try {
					addr = InetAddress.getByName(Constants.SERVER_IP);
					sockaddr = new InetSocketAddress(addr,
							Constants.SERVER_PORT);
				} catch (UnknownHostException e) {
					Log.e(TAG, "Unknow Host Exception: cannot resolve "
							+ Constants.SERVER_IP);
					return;
				}
				// set connection time out
				try {
					clientSocket.connect(sockaddr, Constants.THREE_SECONDS);
				} catch (IOException e) {
					Log.e(TAG, "Time out when connect to server");
					Toast.makeText(context, Constants.VOTE_ERROR,
							Toast.LENGTH_SHORT).show();
					return;
				}
				try {
					out = new PrintWriter(clientSocket.getOutputStream(), true);

					in = new BufferedReader(new InputStreamReader(clientSocket
							.getInputStream()));
					/**
					 * Integer timeout in milliseconds for blocking accept or
					 * read/receive operations (but not write/send operations).
					 * A timeout of 0 means no timeout.
					 */
					while (in.readLine() != null)
						;
					Log.e(TAG, "After readLine.");
					out.print(twit.getTwitId()+","+voterName+",up");
					out.close();
					clientSocket.close();
					twit.increaseUpThumbs();
					upThumbs.setText(String.valueOf(twit.getUpThumbs()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		downThumbs_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				InetAddress addr = null;
				SocketAddress sockaddr = null;
				PrintWriter out = null;
				BufferedReader in = null;
				Socket clientSocket = new Socket();

				try {
					addr = InetAddress.getByName(Constants.SERVER_IP);
					sockaddr = new InetSocketAddress(addr,
							Constants.SERVER_PORT);
				} catch (UnknownHostException e) {
					Log.e(TAG, "Unknow Host Exception: cannot resolve "
							+ Constants.SERVER_IP);
					return;
				}
				// set connection time out
				try {
					clientSocket.connect(sockaddr, Constants.THREE_SECONDS);
				} catch (IOException e) {
					Log.e(TAG, "Time out when connect to server");
					Toast.makeText(context, Constants.VOTE_ERROR,
							Toast.LENGTH_SHORT).show();
					return;
				}
				try {
					out = new PrintWriter(clientSocket.getOutputStream(), true);

					in = new BufferedReader(new InputStreamReader(clientSocket
							.getInputStream()));
					/**
					 * Integer timeout in milliseconds for blocking accept or
					 * read/receive operations (but not write/send operations).
					 * A timeout of 0 means no timeout.
					 */
					while (in.readLine() != null)
						;
					Log.e(TAG, "After readLine.");
					out.print(twit.getTwitId()+","+voterName+",down");
					out.close();
					clientSocket.close();
					twit.increaseDownThumbs();
					downThumbs.setText(String.valueOf(twit.getDownThumbs()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		return row;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getCount()
	 */

	public int getCount() {
		return socialFeed.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItem(int)
	 */

	public Object getItem(final int index) {

		return socialFeed.get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.widget.Adapter#getItemId(int)
	 */

	public long getItemId(final int index) {
		return index;
	}
}
