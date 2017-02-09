package org.processmining.plugins.stochasticpetrinet.generator;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;

/**
 * Configuration settings for the stochastic Petri net generator {@link Generator}.
 *
 * @author Andreas Rogge-Solti
 */
public class GeneratorConfig {

    public static int DEFAULT_TRANSITION_SIZE = 50;

    /**
     * Stores how many transitions should be added to the net
     */
    private int transitionSize;

    /**
     * Stores whether loops should be added to the net as well.
     */
    private boolean containsLoops;

    /**
     * desired degree of sequences in percent
     * range: 0-100
     * real percentage values will be computed based on other degrees
     * such that these degrees are rather weights.
     */
    private int degreeOfParallelism;

    /**
     * desired degree of sequences in percent
     * range: 0-100
     */
    private int degreeOfSequences;

    /**
     * degree of exclusive choices in percent
     * range: 0-100
     */
    private int degreeOfExclusiveChoices;

    private int degreeOfLoops;

    private DistributionType distributionType;

    private String name;

    private TimeUnit timeUnit;

    private ExecutionPolicy executionPolicy;

    /**
     * specifies whether immediate transitions should be visible and are recorded in the log
     */
    private boolean immedateTransitionsInvisible;

    /**
     * A start transition that signals the start of a process (useful for measuring the duration of the first real activity
     */
    private boolean createDedicatedImmediateStartTransition;

    /**
     * Allows to introduce parallelism only in later stage of generation (to avoid huge parts of the model to be in parallel.)
     */
    private boolean parallelismOnlyInParts;

    public GeneratorConfig() {
        transitionSize = DEFAULT_TRANSITION_SIZE;
        name = "generated net";
        containsLoops = false;
        degreeOfParallelism = 1;
        degreeOfExclusiveChoices = 1;
        degreeOfSequences = 1;
        degreeOfLoops = 0;
        distributionType = DistributionType.NORMAL;
        timeUnit = TimeUnit.MINUTES;
        executionPolicy = ExecutionPolicy.RACE_ENABLING_MEMORY;
        immedateTransitionsInvisible = true;
        createDedicatedImmediateStartTransition = false;
        parallelismOnlyInParts = false;
    }

    public int getTransitionSize() {
        return transitionSize;
    }

    public void setTransitionSize(int nodeSize) {
        this.transitionSize = nodeSize;
    }

    public boolean isContainsLoops() {
        return containsLoops;
    }

    public void setContainsLoops(boolean containsLoops) {
        this.containsLoops = containsLoops;
    }

    public int getDegreeOfParallelism() {
        return degreeOfParallelism;
    }

    public void setDegreeOfParallelism(int degreeOfParallelism) {
        this.degreeOfParallelism = degreeOfParallelism;
    }

    public int getDegreeOfSequences() {
        return degreeOfSequences;
    }

    public void setDegreeOfSequences(int degreeOfSequences) {
        this.degreeOfSequences = degreeOfSequences;
    }

    public int getDegreeOfExclusiveChoices() {
        return degreeOfExclusiveChoices;
    }

    public void setDegreeOfExclusiveChoices(int degreeOfExclusiveChoices) {
        this.degreeOfExclusiveChoices = degreeOfExclusiveChoices;
    }

    public int getDegreeOfLoops() {
        return degreeOfLoops;
    }

    public void setDegreeOfLoops(int degreeOfLoops) {
        this.degreeOfLoops = degreeOfLoops;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DistributionType getDistributionType() {
        return distributionType;
    }

    public void setDistributionType(DistributionType distributionType) {
        this.distributionType = distributionType;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public ExecutionPolicy getExecutionPolicy() {
        return executionPolicy;
    }

    public void setExecutionPolicy(ExecutionPolicy executionPolicy) {
        this.executionPolicy = executionPolicy;
    }

    public boolean isImmedateTransitionsInvisible() {
        return immedateTransitionsInvisible;
    }

    public void setImmedateTransitionsInvisible(boolean immedateTransitionsInvisible) {
        this.immedateTransitionsInvisible = immedateTransitionsInvisible;
    }

    public boolean isCreateDedicatedImmediateStartTransition() {
        return createDedicatedImmediateStartTransition;
    }

    public void setCreateDedicatedImmediateStartTransition(boolean createDedicatedImmediateStartTransition) {
        this.createDedicatedImmediateStartTransition = createDedicatedImmediateStartTransition;
    }

    public boolean isParallelismOnlyInParts() {
        return parallelismOnlyInParts;
    }

    public void setParallelismOnlyInParts(boolean parallelismOnlyInParts) {
        this.parallelismOnlyInParts = parallelismOnlyInParts;
    }


}
