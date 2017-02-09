package org.processmining.plugins.stochasticpetrinet.external.sensor;

public class SensorInterval implements Comparable<SensorInterval> {

    public static final String SEPARATOR = ";";

    private long startTime;
    private long endTime;

    private String locationKey;

    private String resourceKey;

    public SensorInterval(long startTime, long endTime, String location, String resource) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationKey = location;
        this.resourceKey = resource;
    }

    public static String getHeader() {
        return "Location" + SEPARATOR + "Resource" + SEPARATOR + "startTime" + SEPARATOR + "endTime";
    }

    public String toString() {
        return locationKey + SEPARATOR + resourceKey + SEPARATOR + startTime + SEPARATOR + endTime;
    }

    public int compareTo(SensorInterval o) {
        int diff = new Long(startTime).compareTo(o.startTime);
        if (diff == 0) {
            diff = new Long(endTime).compareTo(o.endTime);
            if (diff == 0) {
                diff = resourceKey.compareTo(o.resourceKey);
                if (diff == 0) {
                    diff = locationKey.compareTo(o.locationKey);
                }
            }
        }
        return diff;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public void setLocationKey(String locationKey) {
        this.locationKey = locationKey;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (endTime ^ (endTime >>> 32));
        result = prime * result + ((locationKey == null) ? 0 : locationKey.hashCode());
        result = prime * result + ((resourceKey == null) ? 0 : resourceKey.hashCode());
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SensorInterval other = (SensorInterval) obj;
        if (endTime != other.endTime)
            return false;
        if (locationKey == null) {
            if (other.locationKey != null)
                return false;
        } else if (!locationKey.equals(other.locationKey))
            return false;
        if (resourceKey == null) {
            if (other.resourceKey != null)
                return false;
        } else if (!resourceKey.equals(other.resourceKey))
            return false;
        if (startTime != other.startTime)
            return false;
        return true;
    }

}
