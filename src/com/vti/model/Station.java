package com.vti.model;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.android.maps.GeoPoint;
import com.vti.utils.GeoCoder;

import android.os.Parcel;
import android.os.Parcelable;

public class Station implements Parcelable{
	private String routeName;
	private String mode;
	private String stationName;
	private GeoPoint coordinates;
	private String vtiAccount;
	
	public Station(String routeId, String name){
		routeName=routeId;
		stationName=name;
		final String[] colors={"Blue","Red","Green","Yellow","Brown","Purple","Orange"};
		if(routeName.toLowerCase().contains("line")){
			mode="Subway";
			//assign vtiAccount
			vtiAccount=stationName;
			vtiAccount="vti_"+vtiAccount.replaceAll("-", "_").replaceAll("/","_").replaceAll(" ","_");
			if(vtiAccount.length()>20)
				vtiAccount=vtiAccount.substring(0,20);
			
			for(int i=0;i<colors.length;i++){
				if(stationName.contains(colors[i])){
					stationName=stationName.replaceAll(colors[i], "");
					break;
				}
			}
			coordinates=GeoCoder.geocode(routeName.replaceAll(" ","+")+"+"+stationName+"+Chicago");
		}else{
			mode="bus";
			vtiAccount="";
			coordinates=GeoCoder.geocode(stationName.replaceAll("&","+and+").replace(" ", "")+"+Chicago");
		}
	}
	
	private Station(Parcel in) {
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<Station> CREATOR = new Parcelable.Creator<Station>() {
		public Station createFromParcel(Parcel in) {
			return new Station(in);
		}

		public Station[] newArray(int size) {
			return new Station[size];
		}
	};
	
	public String getMode(){
		return mode;
	}
	
	public String getRouteName(){
		return routeName;
	}
	
	public String getStationName(){
		return stationName;
	}
	
	public GeoPoint getGeoPoint(){
		return coordinates;
	}
	
	public String getVTIAccount(){
		return vtiAccount;
	}
	

	public void readFromParcel(Parcel in) {
		this.routeName = in.readString();
		this.mode= in.readString();
		this.stationName=in.readString();
		this.coordinates=(GeoPoint)in.readSerializable();
		this.vtiAccount = in.readString();
	}


	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeString(routeName);
		out.writeString(mode);
		out.writeString(stationName);
		out.writeSerializable((Serializable)coordinates);
		out.writeString(vtiAccount);
	}
	
	public int describeContents() {
		return 0;
	}
}

