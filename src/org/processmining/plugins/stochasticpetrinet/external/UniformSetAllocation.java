package org.processmining.plugins.stochasticpetrinet.external;

import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.*;

public class UniformSetAllocation extends AbstractAllocation {

    private List<Set<Allocatable>> choices;

    public UniformSetAllocation(Collection<Set<Allocatable>> choices, AllocType type) {
        super(type);
        this.choices = new ArrayList<Set<Allocatable>>(choices);
    }

    public Set<Allocatable> getAllocation() {
        int nextInt = StochasticNetUtils.getRandomInt(choices.size());
        return choices.get(nextInt);
    }

    public Map<String, Double> getProbabilitiesOfAllocations() {
        Map<String, Double> probabilties = new HashMap<String, Double>();
        for (Set<Allocatable> allocs : choices) {
            probabilties.put(super.getString(allocs), 1.0 / choices.size());
        }
        return probabilties;
    }

    public Set<Allocatable> getAllAllocatables() {
        Set<Allocatable> allocateds = new HashSet<Allocatable>();
        for (Set<Allocatable> a : choices) {
            allocateds.addAll(a);
        }
        return allocateds;
    }

    public Set<Allocatable> getAllocation(String allocString) {
        for (Set<Allocatable> a : choices) {
            if (getString(a).equals(allocString)) {
                return a;
            }
        }
        return null;
    }
}
