package org.processmining.plugins.stochasticpetrinet.external;

public interface Allocatable {

    /**
     * @return String the name of the allocatable resource.
     */
    public String getName();

    /**
     * Returns the capacity of the allocatable entity.
     * A room can have a capacity > 1, if there can be multiple cases handles within
     * Similarly, a worker can perhaps multi-task and handle multiple cases at once.
     *
     * @return int capacity of the allocatable entity.
     */
    public int getCapacity();
}
