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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import twitter4j.Twitter;
import android.app.Dialog;
import android.content.Context;
import com.vti.utils.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vti.Constants;
import com.vti.R;
import com.vti.SocialFeed;
import com.vti.managers.AccountManager;
import com.vti.managers.DrawableManager;
import com.vti.model.Twit;
import com.vti.utils.CustomEventHandler;
import com.vti.utils.CustomEventListener;

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
	private CustomEventListener callback;
	/**
	 * @param context
	 * @param textViewResourceId
	 * @param objects
	 */
	public TwitAdapter(final Context context, List<Twit> socialFeed, CustomEventListener callback) {
		super();
		this.socialFeed = socialFeed;
		this.context = context;
		this.callback=callback;
		
		AccountManager authMgr = new AccountManager(context);
		if (!authMgr.isAccountEmpty()){
			twitter = authMgr.getTwitterFactory().getInstance(); 
		}
		try {
			voterName=twitter.getScreenName();
			Log.d(TAG, twitter.getOAuthAccessToken().toString());
			Log.d(TAG, voterName);
		} catch (Exception e) {
			Log.d(TAG, Log.stack2string(e));
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

		final TextView twitMessage = (TextView) row.findViewById(R.id.twitMessage);
		twitMessage.setText(twit.getTwitMessage());
		
		final TextView timestamp=(TextView) row.findViewById(R.id.time);
		timestamp.setText(calLatency(twit.getTimestamp()));
		
		final ImageButton editPublishButton = (ImageButton) row
				.findViewById(R.id.retweet);
		editPublishButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(final View v){
				callback.onCustomLongClick(v, twit.getTwitMessage());
			}
		});
		
		row.setOnLongClickListener(new CustomEventHandler(callback,position,twit.getTwitMessage()));
		// set up/down Thumbs button visible
		final ImageButton upThumbs_button = (ImageButton) row
				.findViewById(R.id.upThumbsPic);
		final ImageButton downThumbs_button = (ImageButton) row
				.findViewById(R.id.downThumbsPic);
		upThumbs_button.setVisibility(View.VISIBLE);
		downThumbs_button.setVisibility(View.VISIBLE);

		final TextView upThumbs = (TextView) row.findViewById(R.id.upThumbsNum);
		//upThumbs.setText(Long.toString(twit.getUpThumbs()));
		final TextView downThumbs = (TextView) row.findViewById(R.id.downThumbsNum);
		//upThumbs.setText(Long.toString(twit.getDownThumbs()));

		// update the # of upThumb votes and the # of downThumb votes
		upThumbs_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				InetAddress addr = null;
				SocketAddress sockaddr = null;
				PrintWriter out = null;
				//BufferedReader in = null;
				Socket clientSocket = new Socket();
				if(twit.getAlreadyVotedUp())
					Toast.makeText(context, Constants.REPEAT_VOTE, Toast.LENGTH_SHORT ).show();
				else{
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
						out = new PrintWriter(clientSocket.getOutputStream(),
								true);

						//in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						/**
						 * Integer timeout in milliseconds for blocking accept
						 * or read/receive operations (but not write/send
						 * operations). A timeout of 0 means no timeout.
						 */
						//while (in.readLine() != null);
						//Log.e(TAG, "After readLine.");
						out.print(twit.getTwitMessage() + "," + voterName + ",up");
						out.close();
						clientSocket.close();
						twit.increaseUpThumbs();
						//upThumbs.setText(String.valueOf(twit.getUpThumbs()));
						twit.setAlreadyVotedUp();
						Toast.makeText(context, Constants.VOTE_SUCCESS,	Toast.LENGTH_SHORT).show();
					} catch (IOException e) {
						Log.d(TAG, Log.stack2string(e));
					}
				}
			}
		});

		downThumbs_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				InetAddress addr = null;
				SocketAddress sockaddr = null;
				PrintWriter out = null;
				//BufferedReader in = null;
				Socket clientSocket = new Socket();
				if(twit.getAlreadyVotedDown())
					Toast.makeText(context, Constants.REPEAT_VOTE,Toast.LENGTH_SHORT ).show();
				else{
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
						out = new PrintWriter(clientSocket.getOutputStream(),
								true);
						out.print(twit.getTwitMessage() + "," + voterName + ",down");
						out.close();
						clientSocket.close();
						twit.increaseDownThumbs();
						//downThumbs.setText(String.valueOf(twit.getDownThumbs()));
						twit.setAlreadyVotedDown();
						Toast.makeText(context, Constants.VOTE_SUCCESS,	Toast.LENGTH_SHORT).show();
					} catch (IOException e) {
						Log.d(TAG, Log.stack2string(e));
					}
				}
			}
		});

		return row;
	}

	private String calLatency(long time){
		long diff=(int)((System.currentTimeMillis()-time)/Constants.ONE_MINUTE); //difference in minutes
		int days, hours, minutes;
		days=(int)(diff/60/24);
		hours=(int)((diff-days*60*24)/60);
		minutes=(int)diff%60;
		if(days>0)
			return days+" days "+hours+" hours "+minutes+" minutes ago";
		else
			if(hours>0)
				return hours+" hours "+minutes+" minutes ago";
			else
				return minutes+" minutes ago";
	}

	public int getCount() {
		return socialFeed.size();
	}

	public Object getItem(final int index) {

		return socialFeed.get(index);
	}

	public long getItemId(final int index) {
		return index;
	}
}
