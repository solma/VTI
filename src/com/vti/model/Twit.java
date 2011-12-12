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
package com.vti.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author rohit
 * 
 */
public class Twit implements Parcelable {

	private long twitId;
	private String imageUrl;
	private String profileName;
	private String twitMessage;
	private long upThumbs;
	private long downThumbs;
	private boolean alreadyVotedUp;
	private boolean alreadyVotedDown;


	public static final Parcelable.Creator<Twit> CREATOR = new Parcelable.Creator<Twit>() {
		public Twit createFromParcel(Parcel in) {
			return new Twit(in);
		}

		public Twit[] newArray(int size) {
			return new Twit[size];
		}
	};

	private Twit(Parcel in) {
		readFromParcel(in);
	}

	/**
	 * @param imageUrl
	 * @param profileName
	 * @param twitMessage
	 */
	public Twit(long twitId,String profileName, String imageUrl, String twitMessage) {
		super();
		this.twitId=twitId;
		this.imageUrl = imageUrl;
		this.profileName = profileName;
		this.twitMessage = twitMessage;
		this.upThumbs=0;
		this.downThumbs=0;
		this.alreadyVotedUp=false;
		this.alreadyVotedDown=false;
	}
	
	
	public boolean getAlreadyVotedUp(){
		return alreadyVotedUp;
	}
	
	public boolean getAlreadyVotedDown(){
		return alreadyVotedDown;
	}
	
	public void setAlreadyVotedUp(){
		alreadyVotedUp=true;
	}
	
	public void setAlreadyVotedDown(){
		alreadyVotedDown=true;
	}
	
	
	public void increaseUpThumbs(){
		upThumbs++;
	}
	
	public void increaseDownThumbs(){
		downThumbs++;
	}
		
	/**
	 * @return the twitId
	 */
	public long getTwitId() {
		return twitId;
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @return the profileName
	 */
	public String getProfileName() {
		return profileName;
	}

	/**
	 * @return the twitMessage
	 */
	public String getTwitMessage() {
		return twitMessage;
	}
	
	/**
	 * @return # of up thumbs
	 */
	public long getUpThumbs() {
		return upThumbs;
	}
	
	/**
	 * @return # of down thumbs
	 */
	public long getDownThumbs() {
		return downThumbs;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeLong(twitId);
		out.writeString(imageUrl);
		out.writeString(profileName);
		out.writeString(twitMessage);
		out.writeLong(upThumbs);
		out.writeLong(downThumbs);
		out.writeInt(alreadyVotedUp==true?1:0);
		out.writeInt(alreadyVotedDown==true?1:0);
	}

	public void readFromParcel(Parcel in) {
		this.twitId = in.readLong();
		this.imageUrl = in.readString();
		this.profileName = in.readString();
		this.profileName = in.readString();
		this.upThumbs=in.readLong();
		this.downThumbs=in.readLong();
		this.alreadyVotedUp=(in.readInt()==1?true:false);
		this.alreadyVotedDown=(in.readInt()==1?true:false);
	}

	public int describeContents() {
		return 0;
	}


}
