package org.processmining.plugins.stochasticpetrinet.external;

import org.apache.commons.math3.util.FastMath;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.*;

public class PoissonAllocation extends AbstractAllocation {
    private Allocatable[] choices;

    private double dropRate;

    private double maxProb;

    /**
     * Creates an allocation between certain resources with a simple rule: the probability
     * to pick the second one is dropRate times the probability of the first one, and so on.
     *
     * @param allocatableChoices choices to pick from.
     * @param dropRate           a factor (between 0 and 1) that makes the next choice less likely than the previous in the list
     * @param type
     */
    public PoissonAllocation(Collection<Allocatable> allocatableChoices, double dropRate, AllocType type) {
        super(type);
        this.choices = new ArrayList<Allocatable>(allocatableChoices).toArray(new Allocatable[allocatableChoices.size()]);
        this.dropRate = dropRate;
        this.maxProb = getMaxProb(dropRate, choices.length);
    }

    private double getMaxProb(double dropRate2, int length) {
        double sum = 0;
        for (int i = 0; i < length; i++) {
            sum += FastMath.pow(dropRate2, i);
        }
        return sum;
    }

    public Set<Allocatable> getAllocation() {
        double next = StochasticNetUtils.getRandomDouble() * maxProb;
        double sum = 0;
        int i = 0;
        for (; i < choices.length && sum < next; i++) {
            sum += FastMath.pow(dropRate, i);
        }
        Set<Allocatable> result = new HashSet<Allocatable>();
        result.add(choices[i - 1]);
        return result;
    }

    public Map<String, Double> getProbabilitiesOfAllocations() {
        Map<String, Double> probabilties = new HashMap<String, Double>();
        int i = 0;
        for (Allocatable alloc : choices) {
            double prob = FastMath.pow(dropRate, i++) / maxProb;
            if (prob > 0) {
                probabilties.put(alloc.getName(), prob);
            }
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
