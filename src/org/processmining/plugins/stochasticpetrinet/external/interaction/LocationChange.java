package org.processmining.plugins.stochasticpetrinet.external.interaction;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LocationChange implements Comparable<LocationChange> {

    private String resource;
    private long duration;

    private long lastDuration;

    private List<String> areasPassed;

    public LocationChange(String resource) {
        this.resource = resource;
        areasPassed = new ArrayList<String>();
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<String> getAreasPassed() {
        return areasPassed;
    }

    public void setAreasPassed(List<String> areasPassed) {
        this.areasPassed = areasPassed;
    }

    public int compareTo(LocationChange o) {
        return Long.compare(getFullDuration(), o.getFullDuration());
    }

    public void setLastActivityDuration(long lastDuration) {
        this.lastDuration = lastDuration;
    }

    public long getFullDuration() {
        return this.lastDuration + this.duration;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Color getColor() {
        return new Color(resource.hashCode());
    }

}
