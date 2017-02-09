package org.processmining.plugins.stochasticpetrinet.external.interaction;

import org.processmining.plugins.stochasticpetrinet.external.sensor.SensorInterval;

public class Record implements Comparable<Record> {

    public static final String ARRIVING = "arriving";
    public static final String ANOMALY = "anomaly";
    public static final String IDLE = "idle";

    private String id;

    private long start;
    private long end;

    private String activity;

    private String location;

    private boolean imputed = false;

    public Record(String id, long start, long end, String activity, String location) {
        this(id, start, end, activity, location, false);
    }

    public Record(String id, long start, long end, String activity, String location, boolean imputed) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.activity = activity;
        this.location = location;
        this.imputed = imputed;
    }

    public String toString() {
        return "Record [id=" + id + ", start=" + start + ", end=" + end + ", activity=" + activity + ", location="
                + location + ", imputed=" + imputed + "]";
    }

    public String getRow() {
        return id + SensorInterval.SEPARATOR + activity + SensorInterval.SEPARATOR + start + SensorInterval.SEPARATOR + end + SensorInterval.SEPARATOR + location + SensorInterval.SEPARATOR + imputed;
    }

    public static String getHeader() {
        return "badgeId" + SensorInterval.SEPARATOR + "activity" + SensorInterval.SEPARATOR + "starttime" + SensorInterval.SEPARATOR + "endtime" + SensorInterval.SEPARATOR + "location" + SensorInterval.SEPARATOR + "imputed";
    }

    public int compareTo(Record o) {
        int order = Long.compare(start, o.start);
        if (order == 0) {
            order = Long.compare(end, o.end);
        }
        if (order == 0) {
            order = id.compareTo(o.id);
        }
        if (order == 0) {
            order = activity.compareTo(o.activity);
        }
        if (order == 0) {
            order = location.compareTo(o.location);
        }
        return order;
    }


    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activity == null) ? 0 : activity.hashCode());
        result = prime * result + (int) (end ^ (end >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (imputed ? 1231 : 1237);
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + (int) (start ^ (start >>> 32));
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Record other = (Record) obj;
        if (activity == null) {
            if (other.activity != null)
                return false;
        } else if (!activity.equals(other.activity))
            return false;
        if (end != other.end)
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (imputed != other.imputed)
            return false;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        if (start != other.start)
            return false;
        return true;
    }

    public long getStartTime() {
        return start;
    }

    public String getId() {
        return id;
    }

    public String getActivity() {
        return activity;
    }

    public long getEndTime() {
        return end;
    }

    public String getLocation() {
        return location;
    }

    public boolean temporallyContains(Record r) {
        return start <= r.start && end >= r.end;
    }

    public boolean isActivityRecord() {
        return location != null && !location.isEmpty() && !location.equals(ARRIVING) && !location.equals(ANOMALY) && !activity.equals(IDLE);
    }

    public long getIntersectTime(Record r2) {
        return Math.max(0, Math.min(end, r2.end) - Math.max(start, r2.start));
    }

    public void setStartTime(long start) {
        this.start = start;
    }

    public boolean isAnomaly() {
        return ANOMALY.equals(activity);
    }


}
