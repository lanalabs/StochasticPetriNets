package org.processmining.plugins.stochasticpetrinet.generator;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;

/**
 * Configuration settings for the stochastic Petri net generator {@link Generator}.
 * 
 * @author Andreas Rogge-Solti
 *
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
	
	private int degreeOfParallelism;
	
	private int degreeOfSequences;
	
	private int degreeOfExclusiveChoices;
	
	private int degreeOfLoops;
	
	private DistributionType distributionType;
	
	private String name;
	
	public GeneratorConfig(){
		transitionSize = DEFAULT_TRANSITION_SIZE;
		name="generated net";
		containsLoops = false;
		degreeOfParallelism = 1;
		degreeOfExclusiveChoices = 1;
		degreeOfExclusiveChoices = 1;
		degreeOfLoops = 0;
		distributionType = DistributionType.NORMAL;
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
	public void setName(String name){
		this.name = name;
	}

	public DistributionType getDistributionType() {
		return distributionType;
	}

	public void setDistributionType(DistributionType distributionType) {
		this.distributionType = distributionType;
	}
	
}
