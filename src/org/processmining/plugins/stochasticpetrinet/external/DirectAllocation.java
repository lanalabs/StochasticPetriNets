package org.processmining.plugins.stochasticpetrinet.external;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class DirectAllocation implements Allocation{
	
	private Set<Allocatable> allocation;

	public DirectAllocation(Set<Allocatable> allocation){
		this.allocation = allocation;
	}

	public Set<Allocatable> getAllocation() {
		return ImmutableSet.copyOf(allocation);
	}

}
