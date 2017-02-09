package org.processmining.plugins.stochasticpetrinet.external;

import java.util.Set;

public class ProbabilisticAllocation {
    private Set<Allocatable> allocatedEntities;
    private double weight;

    public ProbabilisticAllocation(Set<Allocatable> allocatedEntities, double weight) {
        this.allocatedEntities = allocatedEntities;
        this.weight = weight;
    }

    public Set<Allocatable> getAllocatedEntities() {
        return allocatedEntities;
    }

    public void setAllocatedEntities(Set<Allocatable> allocatedEntities) {
        this.allocatedEntities = allocatedEntities;
    }

    public double getWeight() {
        return weight;
    }
}
