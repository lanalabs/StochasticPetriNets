package org.processmining.plugins.stochasticpetrinet.external;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.utils.datastructures.Triple;

public class AllocationBasedNetGenerator {
	
	public static final String PLACE_CASE_PREFIX = "p_case_";
	
	public static long seed = 5L;
	
	protected static Random random = new Random(seed);
	protected static RealDistribution arrivalDistribution;

	/**
	 * 
	 * @param base
	 * @param allocations
	 * @param resources
	 * @param numCases
	 * @param meanTimeBetweenArrivals
	 * @return
	 */
	public static Object[] generateNet(StochasticNet base, PetrinetModelAllocations allocations, Set<Allocatable> resources, int numCases, double meanTimeBetweenArrivals){
		
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
			insertCasePath(caseId, arrivalTime, net, baseMarking, base, semantics, allocations, allocatablePlaces, initialMarking);
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
	 */
	private static void insertCasePath(int caseId, long arrivalTime, StochasticNet net, Marking baseMarking, StochasticNet base,
			StochasticNetSemantics semantics, PetrinetModelAllocations allocations, Map<Allocatable, Place> allocatablePlaces, Marking initialMarking) {
			
		// pick a path through original model and add it to the target model!
		semantics.setCurrentState(baseMarking);
		Collection<Transition> transitions = semantics.getExecutableTransitions();
		
		Map<Transition, Long> transitionRemainingTimes = new HashMap<Transition, Long>();
		
		long currentTime = 0;
		
		PNSimulatorConfig config = new PNSimulatorConfig(1, net.getTimeUnit());

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
		
		while (transitions.size() > 0) {
			Triple<Transition, Long, Double> transitionToFire = PNSimulator.pickTransition(semantics, transitions, transitionRemainingTimes, null, base, config, currentTime, 0L, false, false);
			
			TimedTransition origTransition = (TimedTransition) transitionToFire.getFirst();
			
			// wire this transition in the new net:
			Set<Allocation> allocs = allocations.getAllocations(origTransition);
			
			TimedTransition newTransition = null;
			
			if (allocs != null){
				// wire transition to allocated resources
				Transition newStartTransition = net.addImmediateTransition(origTransition.getLabel()+"+START");
				newTransition = net.addTimedTransition(origTransition.getLabel(), origTransition.getDistributionType(), origTransition.getDistributionParameters());
				
				net.addArc(currentTracePlace, newStartTransition);
				
				currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId+"_"+(++stepTransition));
				net.addArc(newStartTransition, currentTracePlace);
				net.addArc(currentTracePlace, newTransition);
						
				for (Allocation alloc : allocs){
					for (Allocatable allocatable : alloc.getAllocation()){
						if (allocatablePlaces.containsKey(allocatable)){
							Place allocatedPlace = allocatablePlaces.get(allocatable);
							net.addArc(allocatedPlace, newStartTransition); // consume resource token when transition starts
							net.addArc(newTransition, allocatedPlace); // release resource token when transition ends
						} else {
							System.err.println("Debug me! Why is there no place for this allocatable resource??");
						}
					}
				}
			} else {
				newTransition = net.addTimedTransition(origTransition.getLabel(), origTransition.getDistributionType(), origTransition.getDistributionParameters());
				net.addArc(currentTracePlace, newTransition);	
			}
			currentTracePlace = net.addPlace(PLACE_CASE_PREFIX+caseId+"_"+(++stepTransition));
			net.addArc(newTransition, currentTracePlace);
			
			try {
				semantics.executeExecutableTransition(transitionToFire.getFirst());
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
			}
			transitions = semantics.getExecutableTransitions();
		}
		
		
		
		
	}
	
	
	
//	/// simulation stuff:
//	/**
//	 * 
//	 * @param transitions all enabled transitions to pick from.
//	 * @param petriNet the underlying Petri net of the simulation.
//	 * @param config the configuration of the simulation. See {@link PNSimulatorConfig}. Contains the selection policy!
//	 * @param startOfTransition the absolute time of the current marking's last state.
//	 * @param constraint all sampled durations should be greater than the constraint
//	 * @param usePositiveTimeContraint the simulation might start in the middle of one trace, after some time has passed.
//	 * 		  In this case, we don't want to generate samples that are in the past.  (the parameter traceStart sets the constraint's value)
//
// 	 * 
// 	 * @return The transition that is picked as the next one to fire with its duration in the current marking 
//	 */
//	public static Triple<Transition, Long, Double> pickTransition(Semantics<Marking,Transition> semantics, Collection<Transition> transitions, Map<Transition, Long> transitionRemainingTimes, PetrinetGraph petriNet, PNSimulatorConfig config, long startOfTransition, long constraint, boolean usePositiveTimeContraint) {
//		if (petriNet instanceof StochasticNet && PNSimulator.transitionsContainTimingInfo(transitions)){
//			// sanity check of the semantics, to make sure that only immediate transitions, or timed transitions are competing for the right to fire next!
//			boolean allImmediate = PNSimulator.getOnlyImmediateTransitions(transitions, true);
//			boolean allTimed = PNSimulator.getOnlyImmediateTransitions(transitions, false);
//			
//			
//			// either transitions are all immediate -> pick one randomly according to their relative weights... 
//			if (allImmediate){
//				Pair<Integer, Double> indexAndProbability = pickTransitionAccordingToWeights(transitions, new Date(constraint), semantics);
//				Transition t = PNSimulator.getTransitionWithIndex(transitions,indexAndProbability.getFirst());
//				transitionRemainingTimes.put(t,0l);
//				return new Triple<Transition, Long, Double>(t, 0l, indexAndProbability.getSecond());
//			} // or they are all timed -> pick according to firing semantics...
//			else if (allTimed){
//				double probability = 1;
//				// select according to selection policy:
//				if (config.getExecutionPolicy().equals(ExecutionPolicy.GLOBAL_PRESELECTION)){
//					Pair<Integer, Double> indexAndProbability = pickTransitionAccordingToWeights(transitions, new Date(constraint), semantics);
//					// restrict the set of enabled transitions to the randomly picked one:
//					Transition t = PNSimulator.getTransitionWithIndex(transitions,indexAndProbability.getFirst());
//					transitions = new LinkedList<Transition>();
//					transitions.add(t);
//					probability = indexAndProbability.getSecond();
//				}
//				
//				// compute mean durations (indirectly proportional to firing rates) and assume exponential distributions for computing the probability (we don't want to do costly integration now)
//				double cumulativeRates = 0;
//				
//				// select according to race policy:
//				// they are all timed: (truly concurrent or racing for shared tokens)
//				SortedMap<Long,Transition> times = new TreeMap<>();
//				Map<Transition, Double> firingRates = new HashMap<>();
//				for (Transition transition :transitions){
//					if (usePositiveTimeContraint){
//						// calculate minimum transition time that is necessary for transition to be satisfying the constraint (resulting in time bigger than traceStart)
//						double samplingConstraint = Math.max(0, (constraint-startOfTransition)/config.getUnitFactor().getUnitFactorToMillis());
//						long now = System.currentTimeMillis();
//						long transitionRemainingTime = getTransitionRemainingTime(transition, config.getUnitFactor(), startOfTransition, samplingConstraint);
//						if (transitionRemainingTime+startOfTransition < constraint){
//							transitionRemainingTime = constraint-startOfTransition;
//							transitionRemainingTimes.put(transition, transitionRemainingTime);
//						}
//						long millis = System.currentTimeMillis()-now;
//						if (millis > 100){
//							System.out.println("sampling took: "+millis+"ms. constraint "+samplingConstraint+", transition: "+transition.getLabel()+" type: "+((TimedTransition)transition).getDistributionType());
//						} 
//						// make sure transition duration is bigger than constraint (sometimes floating point arithmetic might sample values that are overflowing, or just about the constraint.
//						if (!transitionRemainingTimes.containsKey(transition) && transitionRemainingTime+startOfTransition < constraint){
//							transitionRemainingTimes.put(transition, (long) (samplingConstraint*config.unitFactor.getUnitFactorToMillis())+1);
//							System.out.println("distribution ("+transition.getLabel()+") with constraint: "+samplingConstraint+", mean: "+((TimedTransition)transition).getDistribution().getNumericalMean()+" (Rounding produced Infinity)!!");
//						}
//						times.put(transitionRemainingTime, transition);
//					} else {
//						// only allow positive durations:
//						times.put(getTransitionRemainingTime(transition, config.unitFactor, startOfTransition, 0), transition);
//					}
//					double rate = getFiringrate(transition);
//					firingRates.put(transition, rate);
//					cumulativeRates += rate;
//					
//				}
//				Transition nextTransition = times.get(times.firstKey());
//				probability *= firingRates.get(nextTransition) / cumulativeRates;
//				return new Triple<Transition, Long, Double>(nextTransition, transitionRemainingTimes.get(nextTransition), probability);
//			} else {
//				// semantics should make sure, that only the transitions of the highest priority are enabled in the current marking!
//				throw new IllegalArgumentException("Stochastic semantics bug! There should either be only immediate or only timed activities enabled!");
//			}
//		} else {
//			// pick randomly:
//			int randomPick = random.nextInt(transitions.size());
//			Transition t = getTransitionWithIndex(transitions, randomPick);
//			return new Triple<Transition, Long, Double>(t,getTransitionRemainingTime(t,config.unitFactor, startOfTransition, 0), 1.0/transitions.size());
//		}
//	}
//	
//	
//	public static Pair<Integer, Double> pickTransitionAccordingToWeights(Collection<Transition> transitions, Date currentTime, Semantics<Marking, Transition> semantics) {
//		double[] weights = new double[transitions.size()];
//		double cumulativeWeights = 0;
//		int i = 0;
//		for (Transition transition : transitions){
//			TimedTransition tt = (TimedTransition) transition;
//			double weight = tt.getWeight();
//			weights[i++] = weight;
//			cumulativeWeights += weight;
//		}
//		int index = StochasticNetUtils.getRandomIndex(weights, random);
//		double probability = weights[index] / cumulativeWeights;
//		return new Pair<Integer,Double>(index,probability);
//	}
}
