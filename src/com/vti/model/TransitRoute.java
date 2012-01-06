package com.vti.model;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class TransitRoute implements Parcelable{
	private String travelTime;
	private String title;
	private String modeSequence;
	private String origin;
	private String dest;
	private ArrayList<Station> transferStations;
	private String toString;
	
	private TransitRoute(Parcel in) {
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<TransitRoute> CREATOR = new Parcelable.Creator<TransitRoute>() {
		public TransitRoute createFromParcel(Parcel in) {
			return new TransitRoute(in);
		}

		public TransitRoute[] newArray(int size) {
			return new TransitRoute[size];
		}
	};

	public TransitRoute(String route){
		toString=route;
		transferStations=new ArrayList<Station>();
		String[] lines = route.split("\n");
		title=lines[0];
		dest=lines[1].trim();
		origin=lines[2].split(":")[1].trim();
		travelTime=title.substring(title.indexOf('(')+1, title.indexOf(')'));
	
		// extract modeSquences and transferStations
		int count=0;
		String routeId="";
		StringBuilder modes=new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			//System.out.println(line + "   "+ routeId + "  "+ count);
			if(line.startsWith("Bus")) modes.append("Bus ");
			if(line.startsWith("Subway")) modes.append("Subway ");
			if(line.startsWith("Walk")) modes.append("Walk ");
			if (count==0) {
				if (line.startsWith("Bus") || line.startsWith("Subway")) {
					routeId = line.substring(line.indexOf('-')+1).trim();
					count=(count+1)%4;
				}
			}else{
				//count==1 is the travel time line
				if(count>1)
					transferStations.add(new Station(routeId, line.substring(line.indexOf(' ')+1).trim()));
				count=(count+1)%4;
			}
		}
		modeSequence=modes.toString();
		//System.out.println(transferStations);
		//System.out.println();
	}
	
	public ArrayList<Station> getTransferStations(){
		return transferStations;
	}
	
	public String getModeSequence(){
		return modeSequence;
	}
	
	public String getOrigin(){
		return origin;
	}
	
	public String getDest(){
		return dest;
	}
	
	public String getTravelTime(){
		return travelTime;
	}
	
	public String getTitle(){
		return title;
	}
	
	@Override
	public String toString(){
		return toString;
	}

	
	public void readFromParcel(Parcel in) {
		this.travelTime = in.readString();
		this.title= in.readString();
		this.origin=in.readString();
		this.dest=in.readString();
		this.modeSequence = in.readString();
		in.readTypedList(transferStations, Station.CREATOR);
		this.toString=in.readString();
	}


	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeString(travelTime);
		out.writeString(title);
		out.writeString(origin);
		out.writeString(dest);
		out.writeString(modeSequence);
		out.writeList(transferStations);
		out.writeString(toString);
	}
	
	public int describeContents() {
		return 0;
	}
}

