package org.processmining.plugins.stochasticpetrinet.external;

import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.*;

/**
 * A distribution of possible allocation options for some activity/task in a process.
 * Locations could be allocated to an activity (usually we want the allocation sets to be singletons in this case)
 * Or resource sets, e.g., {nurseA, doctorB}, or only {doctorB} can be allocated to an activity.
 *
 * @author Andreas Rogge-Solti
 */
public class AllocationDistribution extends AbstractAllocation {

    private List<ProbabilisticAllocation> allocationPossibilities;
    private double cumulativeWeight = 0;

    private Set<Allocatable> allAllocateds = new HashSet<Allocatable>();


    public AllocationDistribution(AllocType type) {
        super(type);
        this.allocationPossibilities = new ArrayList<ProbabilisticAllocation>();
    }

    public void addAllocationOption(Set<Allocatable> allocation, double weight) {
        for (Allocatable alloc : allocation) {
            allAllocateds.add(alloc);
        }
        allocationPossibilities.add(new ProbabilisticAllocation(allocation, weight));
        cumulativeWeight += weight;
    }

    /**
     * randomly draws an allocation from the distribution
     */
    public Set<Allocatable> getAllocation() {
        double d = StochasticNetUtils.getRandomDouble();
        double upperThresh = 0;
        int pos = -1;

        while (d > upperThresh) {
            pos += 1;
            upperThresh += allocationPossibilities.get(pos).getWeight() / cumulativeWeight;
        }

        return allocationPossibilities.get(pos).getAllocatedEntities();
    }

    public Map<String, Double> getProbabilitiesOfAllocations() {
        Map<String, Double> probabilties = new HashMap<String, Double>();
        for (ProbabilisticAllocation alloc : allocationPossibilities) {

            probabilties.put(getString(alloc.getAllocatedEntities()), alloc.getWeight() / cumulativeWeight);
        }
        return probabilties;
    }

    public Set<Allocatable> getAllAllocatables() {
        return allAllocateds;
    }

    public Set<Allocatable> getAllocation(String allocString) {
        for (ProbabilisticAllocation alloc : allocationPossibilities) {
            if (getString(alloc.getAllocatedEntities()).equals(allocString)) {
                return alloc.getAllocatedEntities();
            }
        }
        return null;
    }
}
