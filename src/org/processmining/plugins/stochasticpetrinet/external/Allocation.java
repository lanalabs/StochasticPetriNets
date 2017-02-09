package org.processmining.plugins.stochasticpetrinet.external;

import java.util.Map;
import java.util.Set;

public interface Allocation {
    public enum AllocType {
        LOCATION, RESOURCE;
    }

    public AllocType getType();

    public Set<Allocatable> getAllocation();

    public Map<String, Double> getProbabilitiesOfAllocations();

    public Set<Allocatable> getAllAllocatables();

    public Set<Allocatable> getAllocation(String allocString);
}
