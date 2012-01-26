package com.vti.model;

import android.os.Parcel;
import android.os.Parcelable;

public class CTARoute implements Parcelable{

	private String rtId;
	private String rtName;
	private String rtDir;
	private boolean isBus;

	public CTARoute(String rt, String rtnm, String dir, boolean yesOrNo) {
		rtId = rt;
		rtName = rtnm;
		rtDir=dir;
		isBus=yesOrNo;
	}

	public String rtId() {
		return rtId;
	}

	public String rtName() {
		return rtName;
	}
	
	public String rtDir() {
		return rtDir;
	}
	
	public boolean isBus(){
		return isBus;
	}

	public String toString() {
		return rtId + " " + rtName + " " + rtDir;
	}
	
	private CTARoute(Parcel in) {
		readFromParcel(in);
	}
	
	public static final Parcelable.Creator<CTARoute> CREATOR = new Parcelable.Creator<CTARoute>() {
		public CTARoute createFromParcel(Parcel in) {
			return new CTARoute(in);
		}

		public CTARoute[] newArray(int size) {
			return new CTARoute[size];
		}
	};
	
	public void readFromParcel(Parcel in) {
		rtId = in.readString();
		rtName= in.readString();
		rtDir=in.readString();
		isBus=in.readInt()==0?false:true;
	}


	@Override
	public void writeToParcel(Parcel out, int arg1) {
		out.writeString(rtId);
		out.writeString(rtName);
		out.writeString(rtDir);
		out.writeInt(isBus?1:0);
	}
	
	public int describeContents() {
		return 0;
	}

}
