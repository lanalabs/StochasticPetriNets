package org.processmining.plugins.stochasticpetrinet.external;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.external.Allocation.AllocType;

import java.util.HashSet;
import java.util.Set;

public class AllocationConfig {
    private Set<Allocatable> choicesA;
    private Set<Allocatable> choicesB;
    private int numberOfAllocatablesA;
    private int numberOfAllocatablesB;
    private double probability;
    private DistributionType distributionType;
    private AllocType type;

    public AllocationConfig(Set<Allocatable> choices, int numOfAllocatables, double probability, AllocType type) {
        this(choices, numOfAllocatables, probability, type, DistributionType.UNIFORM);
    }

    public AllocationConfig(Set<Allocatable> choices, int numOfAllocatables, double probability,
                            AllocType type, DistributionType distributionType) {
        this(choices, new HashSet<Allocatable>(), numOfAllocatables, 0, probability, type, distributionType);
    }


    public AllocationConfig(Set<Allocatable> setA, Set<Allocatable> setB, int numA, int numB, double probability, AllocType type) {
        this(setA, setB, numA, numB, probability, type, DistributionType.UNIFORM);
    }


    /**
     * Configure a cross product of two sets
     *
     * @param setA
     * @param setB
     * @param numA
     * @param numB
     * @param probability
     * @param type
     */
    public AllocationConfig(Set<Allocatable> setA, Set<Allocatable> setB, int numA, int numB, double probability, AllocType type, DistributionType distributionType) {
        this.choicesA = setA;
        this.choicesB = setB;
        this.numberOfAllocatablesA = numA;
        this.numberOfAllocatablesB = numB;
        this.probability = probability;
        this.distributionType = distributionType;
        this.type = type;

    }

    public Allocation getResultingAllocationDistribution() {
        switch (distributionType) {
            case UNIFORM: // todo: possibly add more supported distributions between options...
            default:
                Set<Set<Allocatable>> allocatableSubsets = StochasticNetUtils.generateAllSubsetsOfSize(choicesA, numberOfAllocatablesA, numberOfAllocatablesA);
                Set<Set<Allocatable>> allocatableSubsetsB = StochasticNetUtils.generateAllSubsetsOfSize(choicesB, numberOfAllocatablesB, numberOfAllocatablesB);
                Set<Set<Allocatable>> crossProduct = StochasticNetUtils.generateCrossProduct(allocatableSubsets, allocatableSubsetsB);
                return new UniformSetAllocation(crossProduct, type);
        }
    }

    public double getProbability() {
        return probability;
    }

}
