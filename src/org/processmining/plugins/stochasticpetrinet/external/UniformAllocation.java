package org.processmining.plugins.stochasticpetrinet.external;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class UniformAllocation implements Allocation{
	private static Random random = new Random(1);
	
	private Allocatable[] choices;
	
	public UniformAllocation(Set<Allocatable> uniformAllocatableChoices){
		this.choices = uniformAllocatableChoices.toArray(new Allocatable[uniformAllocatableChoices.size()]);
	}

	public Set<Allocatable> getAllocation() {
		int nextInt = random.nextInt(choices.length);
		Set<Allocatable> result = new HashSet<Allocatable>();
		result.add(choices[nextInt]);
		return result;
	}
	
}
