package org.processmining.plugins.stochasticpetrinet.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.external.Allocation.AllocType;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.utils.datastructures.Triple;

public class AllocationBasedNetGenerator {
	
	public static final String PLACE_CASE_PREFIX = "p_case_";
	
	protected static RealDistribution arrivalDistribution;

	/**
	 * 
	 * @param base
	 * @param allocations
	 * @param resources
	 * @param numCases
	 * @param meanTimeBetweenArrivals
	 * @param noise the noise represents the probability that something goes NOT according to process knowledge
	 *        (examples are a swap in activities, a change in a duration distribution, a change in the allocated resource set) 
	 * @return
	 */
	public static Object[] generateNet(StochasticNet base, PetrinetModelAllocations allocations, Set<Allocatable> resources, int numCases, double meanTimeBetweenArrivals, double noise){
		
		arrivalDistribution = new ExponentialDistribution(meanTimeBetweenArrivals);
		
		// create an empty net:
		StochasticNet net = new StochasticNetImpl("generated unfolded net for "+numCases+" cases");
		net.setTimeUnit(base.getTimeUnit());
		net.setExecutionPolicy(base.getExecutionPolicy());
		
		
		
		Marking initialMarking = new Marking();
		// add resource places:
		Map<Allocatable, Place> allocatablePlaces = new HashMap<Allocatable, Place>();
		for (Allocatable resource : resources){
			Place p = net.addPlace("p_"+resource.getName());
			allocatablePlaces.put(resource, p);
			initialMarking.add(p, resource.getCapacity());
		}
		
		Marking baseMarking = StochasticNetUtils.getInitialMarking(null, base);
		StochasticNetSemantics semantics = new StochasticNetSemanticsImpl();
		semantics.initialize(base.getTransitions(), baseMarking);
		
		long arrivalTime = 0;
		
		// go through all cases and add their flows:
		for (int caseId = 1; caseId <= numCases; caseId++){
			insertCasePath(caseId, arrivalTime, net, baseMarking, base, semantics, allocations, allocatablePlaces, initialMarking, noise);
			arrivalTime += (long) (arrivalDistribution.sample());

		}
		
		return new Object[]{net,initialMarking}; 
	}

	/**
	 * 
	 * PLEASE NOTE: This is a prototype! 
	 * Parallelism in the base model not yet supported!
	 * 
	 * @param caseId the new case id (usually just an incremental number for experiments)
	 * @param arrivalTime the relative arrival time of the case 
	 * @param net the net to add the case as a lane to
	 * @param baseMarking the initial marking in the base model that will be unfolded
	 * @param base the model that will be unfolded (choices are resolved to a single trace) NOTE!! Parallelism not yet supported! 
	 * @param semantics the semantics of the base net (it will be played out into the new model) 
	 * @param allocations PetrinetAllocations possible allocations per activity
	 * @param allocatablePlaces map of resources to their places in the new model
	 * @param initialMarking the initial marking in the model -> this will be adjusted to contain the case token of the added start place 
	 * @param noise the noise represents the probability that something goes NOT according to process knowledge
	 *        (examples are a swap in activities, a change in a duration distribution, a change in the allocated resource set) 
	 */
	private static void insertCasePath(int caseId, long arrivalTime, StochasticNet net, Marking baseMarking, StochasticNet base,
			StochasticNetSemantics semantics, PetrinetModelAllocations allocations, Map<Allocatable, Place> allocatablePlaces, Marking initialMarking, double noise) {
			
		int stepTransition = 1;
		
		// initial place
		Place currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId);
		initialMarking.add(currentTracePlace);
		
		// add a timed "arrival transition" as first invisible transition:
		TimedTransition arrivalTransition = net.addTimedTransition("arrival_case_"+caseId, DistributionType.DETERMINISTIC, arrivalTime);
//		arrivalTransition.setInvisible(true);
		net.addArc(currentTracePlace, arrivalTransition);
		currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId+"_"+stepTransition);
		net.addArc(arrivalTransition, currentTracePlace);
		
		List<Transition> transitionsToExecuteInSequence = getTransitionSequence(semantics,base, baseMarking);
		applyNoise(transitionsToExecuteInSequence, noise);
		Map<Integer, Place> transitionPlaceMapping = getTransitionPlaceMapping(transitionsToExecuteInSequence, allocations, allocatablePlaces);
		applyNoise(transitionPlaceMapping, noise, convertAllocatablesToPlaces(allocations.getLocationAllocations(),allocatablePlaces));
		
		for (int pos = 0; pos < transitionsToExecuteInSequence.size(); pos++){
			TimedTransition next = (TimedTransition) transitionsToExecuteInSequence.get(pos);
			
			Place locationPlace = transitionPlaceMapping.get(pos);
			
			// wire this transition in the new net:
			Set<Allocation> allocs = allocations.getAllocations(next);
			
			TimedTransition newTransition = null;
			
			if (allocs != null){
				// wire transition to allocated resources
				Transition newStartTransition = net.addImmediateTransition(next.getLabel()+"+START");
				newTransition = net.addTimedTransition(next.getLabel(), next.getDistributionType(), next.getDistributionParameters());
				
				// TODO: delay resource availability after use (idea: use a "return to place" transition for resources and patients that represent going from one location to another.
				
				net.addArc(currentTracePlace, newStartTransition);
				
				currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId+"_"+(++stepTransition));
				net.addArc(newStartTransition, currentTracePlace);
				net.addArc(currentTracePlace, newTransition);
						
				for (Allocation alloc : allocs){
					Set<Place> allocatedPlaces = new HashSet<Place>();
					if (alloc.getType().equals(AllocType.LOCATION)){
						allocatedPlaces.add(locationPlace);
					} else {
						for (Allocatable allocatable : alloc.getAllocation()){
							if (allocatablePlaces.containsKey(allocatable)){
								allocatedPlaces.add(allocatablePlaces.get(allocatable));
							} else {
								System.err.println("Debug me! Why is there no place for this allocatable resource??");
							}
						}
					}
					for (Place allocatedPlace : allocatedPlaces){
						net.addArc(allocatedPlace, newStartTransition); // consume resource token when transition starts
						net.addArc(newTransition, allocatedPlace); // release resource token when transition ends
					}
				}
				
				
			} else {
				newTransition = net.addTimedTransition(next.getLabel(), next.getDistributionType(), next.getDistributionParameters());
				newTransition.setInvisible(next.isInvisible());
				net.addArc(currentTracePlace, newTransition);	
			}
			currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId+"_"+(++stepTransition));
			net.addArc(newTransition, currentTracePlace);
		}
	}

	

	private static List<Place> convertAllocatablesToPlaces(Set<Allocatable> locationAllocations,
			Map<Allocatable, Place> allocatablePlaces) {
		List<Place> places = new ArrayList<Place>();
		for (Allocatable alloc : locationAllocations){
			places.add(allocatablePlaces.get(alloc));
		}
		return places;
	}

	private static Map<Integer, Place> getTransitionPlaceMapping(List<Transition> transitionsToExecuteInSequence,
			PetrinetModelAllocations allocations, Map<Allocatable, Place> allocatablePlaces) {
		Map<Integer, Place> transitionToPlaceMap = new HashMap<Integer, Place>();
		for (int i = 0; i < transitionsToExecuteInSequence.size(); i++){
			Transition t = transitionsToExecuteInSequence.get(i);
			// wire this transition in the new net:
			Set<Allocation> allocs = allocations.getAllocations(t);
			if (allocs != null){
				// wire transition to allocated resources
				for (Allocation alloc : allocs){
					if (alloc.getType().equals(AllocType.LOCATION)){
						for (Allocatable allocatable : alloc.getAllocation()){
							if (allocatablePlaces.containsKey(allocatable)){
								Place allocatedPlace = allocatablePlaces.get(allocatable);
								transitionToPlaceMap.put(i, allocatedPlace);
							}
						}
					}
				}
			}
		}
		return transitionToPlaceMap;
	}

	private static void applyNoise(Map<Integer, Place> transitionPlaceMapping, double noise, List<Place> locationPlaces) {
		if (StochasticNetUtils.getRandomDouble() < noise){
			// randomly assign a location place to one part of the mapping:
			Place randomLocation = locationPlaces.get(StochasticNetUtils.getRandomInt(locationPlaces.size()));
			transitionPlaceMapping.put(StochasticNetUtils.getRandomInt(transitionPlaceMapping.size()), randomLocation);
		}
	}
	
	private static void applyNoise(List<Transition> transitionsToExecuteInSequence, double noise) {
		swapTransitionsRandomly(transitionsToExecuteInSequence, noise);
	}

	protected static void swapTransitionsRandomly(List<Transition> transitionsToExecuteInSequence, double noise) {
		if (transitionsToExecuteInSequence.size()>1 && StochasticNetUtils.getRandomDouble() < noise){
			// insert a modification 
			List<Integer> transitions = new ArrayList<Integer>();
			for (int i = 0; i < transitionsToExecuteInSequence.size(); i++){
				transitions.add(i);
			}
			int first = transitions.remove(StochasticNetUtils.getRandomInt(transitions.size()));
			int swapNeighbor = first == 0 ? 1 : first-1; 
			// perform swap
			Transition removed = transitionsToExecuteInSequence.remove(first);
			transitionsToExecuteInSequence.add(swapNeighbor, removed);
		}
	}

	private static List<Transition> getTransitionSequence(StochasticNetSemantics semantics, StochasticNet base, Marking baseMarking) {
		// pick a path through original model and add it to the target model!
		semantics.setCurrentState(baseMarking);
		List<Transition> transitionSequence = new ArrayList<Transition>();
		Map<Transition, Long> transitionRemainingTimes = new HashMap<Transition, Long>();
		Collection<Transition> transitions = semantics.getExecutableTransitions();
		long currentTime = 0;
		PNSimulatorConfig config = new PNSimulatorConfig(1, base.getTimeUnit());
		while (!transitions.isEmpty()){
			Triple<Transition, Long, Double> transitionToFire = PNSimulator.pickTransition(semantics, transitions, transitionRemainingTimes, null, base, config, currentTime, 0L, false, false);
			
			TimedTransition origTransition = (TimedTransition) transitionToFire.getFirst();
			if (!origTransition.isInvisible()){
				transitionSequence.add(origTransition);
			}
			try {
				semantics.executeExecutableTransition(origTransition);
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
			}
			transitions = semantics.getExecutableTransitions();
		}
		return transitionSequence;
	}
}
