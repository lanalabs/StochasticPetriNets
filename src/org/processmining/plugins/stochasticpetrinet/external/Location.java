package org.processmining.plugins.stochasticpetrinet.external;

import java.util.ArrayList;
import java.util.List;

public abstract class Location extends Entity implements Allocatable {

    protected int capacity;


    protected Location parentLocation;
    protected List<Location> childLocations;

    public Location(String name) {
        this(name, 1);
    }

    public Location(String name, int capacity) {
        super(name);
        this.childLocations = new ArrayList<Location>();
        this.capacity = capacity;
    }

    public Location getParentLocation() {
        return parentLocation;
    }

    public void setParentLocation(Location parentLocation) {
        if (parentLocation != null) {
            parentLocation.removeChild(this);
        }
        this.parentLocation = parentLocation;
        parentLocation.addChild(this);
    }

    private void addChild(Location location) {
        this.childLocations.add(location);
    }

    private void removeChild(Location location) {
        this.childLocations.remove(location);
    }

    public List<Location> getChildLocations() {
        return childLocations;
    }

    public int getCapacity() {
        return this.capacity;
    }
}
