package org.processmining.plugins.stochasticpetrinet.external;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DirectAllocation extends AbstractAllocation {

    private Set<Allocatable> allocation;

    public DirectAllocation(Set<Allocatable> allocation, AllocType type) {
        super(type);
        this.allocation = allocation;
    }

    public Set<Allocatable> getAllocation() {
        return ImmutableSet.copyOf(allocation);
    }

    public Map<String, Double> getProbabilitiesOfAllocations() {
        Map<String, Double> probabilities = new HashMap<String, Double>();
        probabilities.put(getString(getAllocation()), 1.0);
        return probabilities;
    }

    public Set<Allocatable> getAllAllocatables() {
        return allocation;
    }

    public Set<Allocatable> getAllocation(String allocString) {
        if (getString(allocation).equals(allocString)) {
            return allocation;
        }
        return null;
    }
}
