package org.processmining.plugins.stochasticpetrinet.external.interaction;

import cern.colt.Arrays;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNUnfoldedSimulator;

import java.util.HashMap;
import java.util.Map;


public class Activity implements Comparable<Activity> {

    private XEvent startEvent;
    private XEvent endEvent;

    private String name;

    private Map<String, Long> realDurations; // core duration that all resources must be together.

    public Activity(XEvent startEvent, XEvent endEvent, String name) {
        this.startEvent = startEvent;
        this.endEvent = endEvent;
        this.name = name;
        this.realDurations = new HashMap<String, Long>();
    }

    /**
     * Shifts an activity by a given time (positive or negative value to be added/subtracted from both start and end.
     *
     * @param time
     */
    public void shift(long time) {
        XTimeExtension.instance().assignTimestamp(this.startEvent, getStartTime() + time);
        XTimeExtension.instance().assignTimestamp(this.endEvent, getEndTime() + time);
    }

    public XEvent getStartEvent() {
        return startEvent;
    }

    public void setStartEvent(XEvent startEvent) {
        this.startEvent = startEvent;
    }

    public XEvent getEndEvent() {
        return endEvent;
    }

    public void setEndEvent(XEvent endEvent) {
        this.endEvent = endEvent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return XTimeExtension.instance().extractTimestamp(startEvent).getTime();
    }

    public long getEndTime() {
        return XTimeExtension.instance().extractTimestamp(endEvent).getTime();
    }

    public String getLocation() {
        XAttribute location = startEvent.getAttributes().get(PNSimulator.LOCATION_ROOM);
        return location == null ? "" : location.toString();
    }

    /**
     * For an activity, this returns the associated handling resources (optionally)
     * + the current instance (always last in the array)
     *
     * @return String[]  of resources participating in this activity.
     */
    public String[] getResources() {
        String resources = XOrganizationalExtension.instance().extractResource(startEvent);
        if (resources == null) {
            resources = getResourceId(startEvent);
            if (resources == null) {
                resources = "";
            }
        } else {
            String caseId = getResourceId(startEvent);
            resources += PNUnfoldedSimulator.RESOURCE_SEPARATOR + caseId;
        }
        return PNUnfoldedSimulator.getResources(resources);
    }

    private String getResourceId(XEvent startEvent) {
        return "pat" + XConceptExtension.instance().extractInstance(startEvent);
    }

    public void setRealDuration(String resource, long realDuration) {
        this.realDurations.put(resource, realDuration);
    }

    public long getRealDuration(String resource) {
        if (this.realDurations.containsKey(resource)) {
            return this.realDurations.get(resource);
        }
        // fall back to full duration
        return getDuration();
    }

    public long getDuration() {
        return getEndTime() - getStartTime();
    }

    public String toString() {
        return "Activity [startTime=" + getStartTime() + ", endTime=" + getEndTime() + ", name=" + name + ", resources=" + Arrays.toString(getResources()) + ", loc: " + getLocation() + "]";
    }

    public int compareTo(Activity o) {
        int result = Long.compare(getStartTime(), o.getStartTime());
        if (result == 0) {
            result = Long.compare(getEndTime(), o.getEndTime());
        }
        if (result == 0) {
            result = name.compareTo(o.name);
        }
        return result;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endEvent == null) ? 0 : endEvent.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((realDurations == null) ? 0 : realDurations.hashCode());
        result = prime * result + ((startEvent == null) ? 0 : startEvent.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Activity other = (Activity) obj;
        if (getStartTime() != other.getStartTime()) {
            return false;
        }
        if (getEndTime() != other.getEndTime()) {
            return false;
        }
        if (!name.equals(other.name)) {
            return false;
        }
        if (!java.util.Arrays.deepEquals(getResources(), other.getResources())) {
            return false;
        }
        if (!startEvent.getID().equals(other.startEvent.getID())) {
            return false;
        }
        if (!endEvent.getID().equals(other.endEvent.getID())) {
            return false;
        }
        return true;
    }

}
