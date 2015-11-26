package org.processmining.plugins.stochasticpetrinet.external;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class PetrinetModelAllocations {
	
	private Petrinet net;
	
	private Map<Transition, Set<Allocation>> allocations;
	
	public PetrinetModelAllocations(Petrinet net){
		this.net = net;
		allocations = new HashMap<Transition, Set<Allocation>>();
	}

	public void addAllocation(Transition transition, Allocation allocation){
		if (!net.getTransitions().contains(transition)){
			throw new IllegalArgumentException("Transition not in net!!");
		}
		if (!allocations.containsKey(transition)){
			allocations.put(transition, new HashSet<Allocation>());
		}
		allocations.get(transition).add(allocation);
	}
	
	public Set<Allocation> getAllocations(Transition transition){
		return allocations.get(transition);
	}
}
