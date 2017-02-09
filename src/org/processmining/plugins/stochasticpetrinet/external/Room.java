package org.processmining.plugins.stochasticpetrinet.external;

/**
 * A representation of a room which can be allocated to activities.
 * In a model this can be represented as a place with the number of tokens as specified by capacity.
 */
public class Room extends Location {

    private int capacity;

    public Room() {
        super("");
    }

    public Room(String name, int capacity) {
        super(name);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
