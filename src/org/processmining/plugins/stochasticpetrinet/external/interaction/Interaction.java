package org.processmining.plugins.stochasticpetrinet.external.interaction;

import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.SortedSet;

/**
 * An interaction candidate that is created by looking at different combinations of SensorInterval s.
 *
 * @author Andreas Rogge-Solti
 */
public class Interaction implements Comparable<Interaction> {

    private int id;

    private SortedSet<String> resourceKeys;
    private String instanceKey;

    private String locationKey;

    private long startTime;
    private long endTime;

    public Interaction(String instance, SortedSet<String> resources, String locationKey, long startTime, long endTime) {
        this.instanceKey = instance;
        this.resourceKeys = resources;
        this.locationKey = locationKey;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int compareTo(Interaction o) {
        int compare = (int) (startTime - o.startTime);
        if (compare == 0) {
            compare = (int) (endTime - o.endTime);
        }
        if (compare == 0) {
            compare = instanceKey.compareTo(o.instanceKey);
        }
        if (compare == 0) {
            compare = StochasticNetUtils.compareSortedSets(resourceKeys, o.resourceKeys);
        }
        if (compare == 0) {
            compare = locationKey.compareTo(o.locationKey);
        }
        return compare;
    }

    public SortedSet<String> getResourceKeys() {
        return resourceKeys;
    }

    public String getInstanceKey() {
        return instanceKey;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


}
