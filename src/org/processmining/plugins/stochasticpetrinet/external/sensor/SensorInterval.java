package org.processmining.plugins.stochasticpetrinet.external.sensor;

public class SensorInterval implements Comparable<SensorInterval>{

	private static final String SEPARATOR = ";";
	
	long startTime;
	long endTime;
	
	String locationKey;
	
	String resourceKey;
	
	public SensorInterval(long startTime, long endTime, String location, String resource){
		this.startTime = startTime;
		this.endTime = endTime;
		this.locationKey = location;
		this.resourceKey = resource;
	}
	
	public static String getHeader(){
		return "Location"+SEPARATOR+"Resource"+SEPARATOR+"startTime"+SEPARATOR+"endTime";
	}
	
	public String toString(){
		return locationKey+SEPARATOR+resourceKey+SEPARATOR+startTime+SEPARATOR+endTime;
	}

	public int compareTo(SensorInterval o) {
		int diff = new Long(startTime).compareTo(o.startTime);
		if (diff == 0){
			diff = new Long(endTime).compareTo(o.endTime);
			if (diff == 0){
				diff = resourceKey.compareTo(o.resourceKey);
				if (diff == 0){
					diff = locationKey.compareTo(o.locationKey);
				}
			}
		}
		return diff; 
	}
}
