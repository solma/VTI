package com.vti.model;


public class CTAStop {

	private String stpId;
	private String stpName;
	private String stpLat;
	private String stpLon;
	private String rtId;
	private String rtDir;
	private boolean isBus;
	private double dist;//used in nearby stops
	
	public CTAStop(String id, String name, String lat, String lon, String routeId, String dir, boolean yesOrNo) {
		stpId = id;
		stpName = name;
		stpLat=lat;
		stpLon=lon;
		rtId=routeId;
		rtDir=dir;
		isBus=yesOrNo;
	}
	
	@Override
	public boolean equals(Object o){
		return stpId.equals(((CTAStop) o).stpId());
	}
	
	public void setDist(double d){
		dist=d;
	}
	
	public double dist(){
		return dist;
	}
	
	public String stpLat(){
		return stpLat;
	}
	
	public String stpLon(){
		return stpLon;
	}
	
	public String rtId(){
		return rtId;
	}
	public String rtDir(){
		return rtDir;
	}

	public String stpId() {
		return stpId;
	}

	public String stpName() {
		return stpName;
	}
	
	public boolean isBus() {
		return isBus;
	}

	public String toString() {
		return stpId + " " + stpName+ " "+ stpLat+ " "+ stpLon +" "+ rtId+ "  "+ rtDir ;
	}

}
