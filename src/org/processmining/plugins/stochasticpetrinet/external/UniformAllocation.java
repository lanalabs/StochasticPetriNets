package org.processmining.plugins.stochasticpetrinet.external;

import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UniformAllocation extends AbstractAllocation {
    private Allocatable[] choices;

    public UniformAllocation(Set<Allocatable> uniformAllocatableChoices, AllocType type) {
        super(type);
        this.choices = uniformAllocatableChoices.toArray(new Allocatable[uniformAllocatableChoices.size()]);
    }

    public Set<Allocatable> getAllocation() {
        int nextInt = StochasticNetUtils.getRandomInt(choices.length);
        Set<Allocatable> result = new HashSet<Allocatable>();
        result.add(choices[nextInt]);
        return result;
    }

    public Map<String, Double> getProbabilitiesOfAllocations() {
        Map<String, Double> probabilties = new HashMap<String, Double>();
        for (Allocatable alloc : choices) {
            probabilties.put(alloc.getName(), 1.0 / choices.length);
        }
        return probabilties;
    }

    public Set<Allocatable> getAllAllocatables() {
        Set<Allocatable> allocateds = new HashSet<Allocatable>();
        for (Allocatable a : choices) {
            allocateds.add(a);
        }
        return allocateds;
    }

    public Set<Allocatable> getAllocation(String allocString) {
        for (Allocatable a : choices) {
            if (a.getName().equals(allocString)) {
                Set<Allocatable> set = new HashSet<Allocatable>();
                set.add(a);
                return set;
            }
        }
        return null;
    }

}
