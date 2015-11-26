package org.processmining.plugins.stochasticpetrinet.external;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A distribution of possible allocation options for some activity/task in a process.
 * Locations could be allocated to an activity (usually we want the allocation sets to be singletons in this case)
 * Or resource sets, e.g., {nurseA, doctorB}, or only {doctorB} can be allocated to an activity.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class AllocationDistribution implements Allocation{

	private static Random random = new Random(1);
	
	private List<ProbabilisticAllocation> allocationPossibilities;
	private double cumulativeWeight = 0;
	

	public AllocationDistribution(){
		this.allocationPossibilities = new ArrayList<ProbabilisticAllocation>();
	}
	
	public void addAllocationOption(Set<Allocatable> allocation, double weight){
		allocationPossibilities.add(new ProbabilisticAllocation(allocation, weight));
		cumulativeWeight += weight;
	}
	
	/**
	 * randomly draws an allocation from the distribution
	 */
	public Set<Allocatable> getAllocation() {
		double d = random.nextDouble();
		double upperThresh = 0;
		int pos = -1;
		
		while (d > upperThresh){
			pos += 1;
			upperThresh += allocationPossibilities.get(pos).getWeight() / cumulativeWeight;
		}
		
		return allocationPossibilities.get(pos).getAllocatedEntities();
	}
	
	
	
}
