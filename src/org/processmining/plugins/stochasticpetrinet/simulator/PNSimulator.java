package org.processmining.plugins.stochasticpetrinet.simulator;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * Very plain simulator only used for evaluation of the evaluation of the mining of stochastic Petri nets 
 * and the repair-log plug-in. Works with simple stochastic Petri nets.
 * 
 * ProM is no simulator!! By all means use CPNTools (http://cpntools.org) if you 
 * want to have a feature-rich stable simulation and analysis tool for all kinds of Petri nets.
 * 
 * @author Andreas Rogge-Solti
 */
public class PNSimulator {
	public static final String TRANSITION_COMPLETE = "complete";
	public static final String LIFECYCLE_TRANSITION = "lifecycle:transition";
	public static final String TIME_TIMESTAMP = "time:timestamp";
	public static final String CONCEPT_SIMULATED = "concept:simulated";
	public static final String CONCEPT_INSTANCE = "concept:instance";
	public static final String CONCEPT_NAME = "concept:name";

	Random random = new Random(new Date().getTime());

	RealDistribution arrivalDistribution;
	
	/**
	 * Each transition has it's remaining time stored, once it becomes active in a marking. 
	 * These values are all lowered, by the time elapsed in a marking.
	 */
	Map<Transition, Long> transitionRemainingTimes;
	/**
	 * Stores the time of the last step in the simulation.
	 */
	long lastFiringTime;
	
	Marking oneMarking;

	public PNSimulator(){
		transitionRemainingTimes = new HashMap<Transition, Long>();
	}
	
	/**
	 * Asks the user to specify configuration parameters for the simulation.
	 * {@link PNSimulatorConfig}
	 * @param context
	 * @param petriNet
	 * @param semantics
	 * @return
	 */
	public XLog simulate(UIPluginContext context, PetrinetGraph petriNet, Semantics<Marking, Transition> semantics) {
		PNSimulatorConfigUI ui = new PNSimulatorConfigUI(petriNet);
		return simulate(context, petriNet, semantics, ui.getConfig(context), StochasticNetUtils.getInitialMarking(context, petriNet));
	}

	/**
	 * Performs a simulation according to a given configuration {@link PNSimulatorConfig}
	 * @param context {@link UIPluginContext} ProM plugin context
	 * @param petriNet a petri net (preferable with stochastic timing information 
	 * @param semantics a {@link Semantics} according to which simulation should be performed
	 * @param config {@link PNSimulatorConfig}
	 * @return
	 */
	public XLog simulate(UIPluginContext context, PetrinetGraph petriNet,
			Semantics<Marking, Transition> semantics, PNSimulatorConfig config, Marking initialMarking) {
		Marking finalMarking = null;
		if (context != null){
			finalMarking = StochasticNetUtils.getFinalMarking(context, petriNet);
		}
		return simulate(context, petriNet, semantics, config, initialMarking, finalMarking);
	}

	/**
	 * Performs a simulation according to a given configuration {@link PNSimulatorConfig}
	 * @param context {@link UIPluginContext} ProM plugin context
	 * @param petriNet a petri net (preferable with stochastic timing information 
	 * @param semantics a {@link Semantics} according to which simulation should be performed
	 * @param config {@link PNSimulatorConfig}
	 * @param initialMarking
	 * 
	 * @param finalMarking
	 * @return
	 */
	public XLog simulate(UIPluginContext context, PetrinetGraph petriNet,
			Semantics<Marking, Transition> semantics, PNSimulatorConfig config, Marking initialMarking, Marking finalMarking) {
		XLog log = null;
		if (initialMarking == null || initialMarking.isEmpty()){
			if (context!=null){
				context.log("No initial marking found! Trying to use a default one...");
			}
			StochasticNetUtils.getDefaultInitialMarking(petriNet);
		}
		if (oneMarking == null){
			oneMarking = new Marking(petriNet.getPlaces());
		}
		if (config != null) {
			arrivalDistribution = new ExponentialDistribution(config.arrivalRate);
			transitionRemainingTimes = new HashMap<Transition, Long>();
			random.setSeed(config.seed);
			
			XAttributeMap attributeMap = new XAttributeMapImpl();
			attributeMap.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, config.logName + " (simulated from "
					+ petriNet.getLabel() + ")"));
			log = XFactoryRegistry.instance().currentDefault().createLog(attributeMap);

			Date traceStart = new Date();
			
			// show progress to the user:
			if (context != null){
				context.log("Generating "+config.numberOfTraces+" traces...");
				context.getProgress().setMinimum(0);
				context.getProgress().setMaximum(config.numberOfTraces);
			}

			if (!config.deterministicBoundedStateSpaceExploration){
				// do a trace by trace simulation (assumed independence from each other..)
				for (int i = 0; i < config.numberOfTraces; i++) {
					if (context != null){
						context.getProgress().setValue(i);
					}
	
					traceStart = getNextArrivalDate(traceStart, config.unitFactor);
					//Map<Place, List<Long>> placeTimes = new HashMap<Place, List<Long>>();
					//updatePlaceTimes(initialMarking, traceStart, placeTimes);
					semantics.initialize(petriNet.getTransitions(), initialMarking);
					
					XTrace trace = (XTrace) simulateOneTrace(petriNet, semantics, config, initialMarking, traceStart.getTime(), traceStart.getTime(), i, false, finalMarking);
					log.add(trace);
				}
			} else {
				// explore the state space of the structural model with a breadth first search, 
				// but produce only valid traces that correctly finish within a certain threshold.
				long time = System.currentTimeMillis();
				
				XTrace trace = createTrace(1);
				LinkedList<Pair<Pair<XTrace,Marking>,Long>> statesToVisit = new LinkedList<Pair<Pair<XTrace,Marking>,Long>>();
				statesToVisit.add(new Pair<Pair<XTrace, Marking>,Long>(new Pair<XTrace,Marking>(trace, initialMarking),time));
				semantics.initialize(petriNet.getTransitions(), initialMarking);
				Marking endPlaces = getEndPlaces(petriNet);
				if (petriNet.getLabel().equals("s00000633##s00004419")){
					throw new IllegalArgumentException("Too many states!");
				}
				addAllDifferentTracesToLog(log, statesToVisit, semantics, new HashMap<String,Set<Integer>>(), config, endPlaces);
			}
			
			if (context != null){
				context.log(config.numberOfTraces+" traces generated successfully.");
			}
		}
		return log;
		
		
	}
	private Marking getEndPlaces(PetrinetGraph petriNet) {
		Marking endPlaces = new Marking();
		for (Place p : petriNet.getPlaces()){
			if (petriNet.getOutEdges(p).size() == 0){
				endPlaces.add(p);
			}
		}
		return endPlaces;
	}

	private void addAllDifferentTracesToLog(XLog log, LinkedList<Pair<Pair<XTrace, Marking>,Long>> statesToVisit,
			Semantics<Marking, Transition> semantics, Map<String,Set<Integer>> numberOfDecisionTransitions, PNSimulatorConfig config, Marking endPlaces) {
		
		while (!statesToVisit.isEmpty()){
			Pair<Pair<XTrace, Marking>,Long> currentStateWithTime = statesToVisit.removeFirst();
			Pair<XTrace, Marking> currentState = currentStateWithTime.getFirst();
			long time = currentStateWithTime.getSecond();
			XTrace prefix = currentState.getFirst();
			Marking currentMarking = currentState.getSecond();
			
//			if (prefix.size() > config.maxEventsInOneTrace*10){
//				throw new IllegalArgumentException("Petri net contains a potential lifelock!");
//			}
			if (!config.allowUnbounded && !isOneBounded(currentMarking)){
				throw new IllegalArgumentException("Petri net is not 1-bounded!");
			}
			if (statesToVisit.size() > config.maxEventsInOneTrace*10){
				throw new IllegalArgumentException("Too many states!");
			}
			if (log.size() >config.maxEventsInOneTrace){
				throw new IllegalArgumentException("Too many states!");
			}
			
			if (isFinal(currentMarking, endPlaces)){
				// ensure proper naming:
				String instance = String.valueOf(log.size());
				XConceptExtension.instance().assignName(prefix, "tr_"+instance);
				for (XEvent e : prefix){
					XConceptExtension.instance().assignInstance(e, instance);	
				}
				log.add(prefix);
			} else {
				// explore all executable transitions:
				semantics.setCurrentState(currentMarking);
				Collection<Transition> executableTransitions = semantics.getExecutableTransitions();
				if (executableTransitions.size() == 0){
					throw new IllegalArgumentException("Petri net contains a deadlock!");
				}
				for (Transition t : executableTransitions){
//					String markingTransitionCombination = currentMarking.toString()+"_"+t.getLabel()+t.getId();
//					if (!numberOfDecisionTransitions.containsKey(markingTransitionCombination)){
//						numberOfDecisionTransitions.put(markingTransitionCombination, new HashSet<Integer>());
//					}
					
					int numberOfTimesAlreadyInTrace = 0;
					
					for (XEvent event : prefix){
						if (XConceptExtension.instance().extractName(event).equals(t.getLabel())){
							numberOfTimesAlreadyInTrace++;
						}
					}
					
					if (numberOfTimesAlreadyInTrace >= 2){
						// old version: numberOfDecisionTransitions.get(markingTransitionCombination).size() > 5
						
						// do not explore this transition further...
					} else {
//						numberOfDecisionTransitions.get(markingTransitionCombination).add(prefix.size());
						semantics.setCurrentState(currentMarking);
						XTrace clone = (XTrace) prefix.clone();
						if (!t.isInvisible()){
							if (!prefix.isEmpty()){
								long lastEventTime = XTimeExtension.instance().extractTimestamp(prefix.get(prefix.size()-1)).getTime();
								time = Math.max(time, lastEventTime);
							}
								
							time += (long)(config.unitFactor.getUnitFactorToMillis()*StochasticNetUtils.sampleWithConstraint((TimedTransition) t, 0.1));
							XEvent e = createSimulatedEvent(t.getLabel(), time, XConceptExtension.instance().extractName(clone));
							clone.add(e);
						}
						try {
							semantics.executeExecutableTransition(t);
						} catch (IllegalTransitionException e1) {
							e1.printStackTrace();
						}
						statesToVisit.addLast(new Pair<Pair<XTrace,Marking>,Long>(new Pair<XTrace, Marking>(clone, semantics.getCurrentState()),time));
					}
				}
			}
		}
	}

	private boolean isOneBounded(Marking currentMarking) {
		return currentMarking.isLessOrEqual(oneMarking);
	}

	private boolean isFinal(Marking currentMarking, Marking endPlaces) {
		return currentMarking.isLessOrEqual(endPlaces);
	}

	/**
	 * Performs a simple simulation of the Petri net (mostly used for {@link StochasticNet}s, but can also simulate a PN without stochastic annotations) 
	 * See {@link #simulateTraceEnd(PetrinetGraph, Semantics, PNSimulatorConfig, Marking, Date, int, Map, boolean)} for an implementation that does not 
	 * generate costly XIDs required for XES log files.
	 * 
	 * @param petriNet {@link PetrinetGraph} the model
	 * @param semantics {@link Semantics} the semantics 
	 * @param config {@link PNSimulatorConfig} the configuration {@link PNSimulatorConfig}
	 * @param initialMarking {@link Marking} the initial Marking
	 * @param traceStart long the date time to start the trace 
	 * @param constraint long the date time that all simulated events should be greater than
	 * @param i int trace id
	 * @param useTimeConstraint boolean stores whether created events are constrained to be later than traceStart
	 * @param finalMarking Marking a final marking can be set to terminate the simulation, when it is reached... ignored, if null
	 * @return 
	 */
	public Object simulateOneTrace(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
			PNSimulatorConfig config, Marking initialMarking, long traceStart, long constraint, int i, boolean useTimeConstraint, Marking finalMarking) {
		XTrace trace = createTrace(i);
		
		transitionRemainingTimes = new HashMap<Transition, Long>();
		lastFiringTime = traceStart;
		
		Collection<Transition> transitions = semantics.getExecutableTransitions();
		int eventsProduced = 0;

		Marking currentMarking = semantics.getCurrentState();
		while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace && !currentMarking.equals(finalMarking)) {
//			System.out.println("events produced: "+eventsProduced);
			try {
				Pair<Transition, Long> transitionAndDuration = pickTransition(transitions, petriNet, config, lastFiringTime, constraint, useTimeConstraint);
				long firingTime = lastFiringTime+transitionRemainingTimes.get(transitionAndDuration.getFirst());
				
				// fire first transition the list:
				semantics.executeExecutableTransition(transitionAndDuration.getFirst());
				
				Collection<Transition> afterwardsEnabledTransitions = semantics.getExecutableTransitions();
				

				updateTransitionMemoriesAfterFiring(config, transitions, transitionAndDuration, firingTime-lastFiringTime, afterwardsEnabledTransitions, semantics);
				
				// Now, create an event according to the marking and duration of the transition:
				lastFiringTime = firingTime;
								
				if (useTimeConstraint && !transitionAndDuration.getFirst().isInvisible() && firingTime < constraint){
					System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
				}
				insertEvent(i, trace, transitionAndDuration, firingTime);
				
				// before proceeding with the next transition, we update the enabled transitions: 
				transitions = afterwardsEnabledTransitions;
				currentMarking = semantics.getCurrentState();
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
				break;
			}
		}
		return getReturnObject(trace,lastFiringTime);
	}

	protected Object getReturnObject(XTrace trace, long lastFiringTime2) {
		return trace;
	}

	protected XTrace createTrace(int i) {
		XAttributeMap traceAttributes = new XAttributeMapImpl();
		traceAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, String.valueOf(i)));
		XTrace trace = new XTraceImpl(traceAttributes);
		return trace;
	}

	protected void insertEvent(int i, XTrace trace, Pair<Transition, Long> transitionAndDuration, long firingTime) {
		if (!transitionAndDuration.getFirst().isInvisible()){
			XEvent e = createSimulatedEvent(transitionAndDuration.getFirst().getLabel(), firingTime, String.valueOf(i));
			trace.insertOrdered(e);
		}
	}
	
	public void updateTransitionMemoriesAfterFiring(PNSimulatorConfig config, Collection<Transition> transitionsEnabledInMarking,
			Pair<Transition, Long> transitionAndDuration, long elapsedTimeInCurrentMarking, Collection<Transition> afterwardsEnabledTransitions, Semantics<Marking,Transition> semantics) {
		transitionRemainingTimes.remove(transitionAndDuration.getFirst());
		switch (config.executionPolicy) {
			case GLOBAL_PRESELECTION :
				// only one transition is allowed. (no transition count-downs should be used at all...)
				assert transitionRemainingTimes.isEmpty();
				break;
			case RACE_RESAMPLING :
				// reset all clocks after firing.
				transitionRemainingTimes.clear();
				break;
			case RACE_ENABLING_MEMORY :
				Collection<Transition> dormantTransitions = afterwardsEnabledTransitions;
				if (semantics instanceof StochasticNetSemanticsImpl){
					dormantTransitions = ((StochasticNetSemanticsImpl) semantics).getEnabledTransitions();
				}
				// reset timers of all disabled transitions (not in the set of enabled (possibly dormant) transitions
				Object[] transitions = transitionRemainingTimes.keySet().toArray();
				for (Object t : transitions) {
					// entering a state where timed transitions are competing...
					if (!dormantTransitions.contains(t)){
						// transition got disabled in current marking: needs to be resampled next time, it becomes enabled
						transitionRemainingTimes.remove(t);
					}
					// if current marking is not vanishing, time has passed, and we reduce the clocks of the transitions still enabled
					if (transitionRemainingTimes.containsKey(t)){
						transitionRemainingTimes.put((Transition) t, transitionRemainingTimes.get(t) - elapsedTimeInCurrentMarking);	
					}
				}
				break;
			case RACE_AGE_MEMORY:
				if (elapsedTimeInCurrentMarking > 0) {
					// reduce clocks of all enabled transitions 
					for (Transition t : transitionsEnabledInMarking) {
						if (!t.equals(transitionAndDuration.getFirst())) {
							transitionRemainingTimes.put(t, transitionRemainingTimes.get(t) - elapsedTimeInCurrentMarking);
						}
					}
				}
				break;
		}
	}
	
//	/**
// 	 * Same as {@link #simulateOneTrace(PetrinetGraph, Semantics, PNSimulatorConfig, Marking, long, long, int, boolean)}, but without time-consuming XID generation for Events. 
//	 * @param petriNet
//	 * @param semantics
//	 * @param config
//	 * @param initialMarking
//	 * @param traceStart
//	 * @param constraint
//	 * @param i
//	 * @param useTimeConstraint
//	 * @return
//	 */
//	public Long simulateTraceEnd(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
//			PNSimulatorConfig config, Marking initialMarking, long traceStart, long constraint, int i, boolean useTimeConstraint) {
//		Collection<Transition> transitions = semantics.getExecutableTransitions();
//		int eventsProduced = 0;
//		SortedSet<Long> transitionTimes = new TreeSet<Long>();
//		lastFiringTime = traceStart;
//
//		while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace) {
//			try {
//				Set<Transition> transitionsEnabledBefore = null;
//				if (useTimeConstraint){
//					transitionsEnabledBefore = new HashSet<Transition>(transitionRemainingTimes.keySet());
//				}
//				Pair<Transition, Long> transitionAndDuration = pickTransition(transitions, petriNet, config, lastFiringTime, constraint, useTimeConstraint);
//				long firingTime = lastFiringTime+transitionRemainingTimes.get(transitionAndDuration.getFirst());
//				
//				// fire first transition the list:
//				semantics.executeExecutableTransition(transitionAndDuration.getFirst());
//				
//				Collection<Transition> afterwardsEnabledTransitions = semantics.getExecutableTransitions();
//
//				updateTransitionMemoriesAfterFiring(config, transitions, transitionAndDuration, firingTime-lastFiringTime, afterwardsEnabledTransitions, semantics);
//				
//				// Now, create an event according to the marking and duration of the transition:
//				lastFiringTime = firingTime;
//								
//				if (useTimeConstraint && firingTime < constraint && !transitionsEnabledBefore.contains(transitionAndDuration.getFirst()) && !transitionAndDuration.getFirst().isInvisible()){
//					System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
//				}
//				if (!transitionAndDuration.getFirst().isInvisible()){
//					transitionTimes.add(firingTime);
//				}
//				transitions = afterwardsEnabledTransitions;
//			} catch (IllegalTransitionException e) {
//				e.printStackTrace();
//				break;
//			}
//		}
//		return transitionTimes.last();
//	}
	

	private XEvent createSimulatedEvent(String name, long firingTime, String instance) {
		XAttributeMap eventAttributes = new XAttributeMapImpl();
		eventAttributes.put(LIFECYCLE_TRANSITION, new XAttributeLiteralImpl(LIFECYCLE_TRANSITION,
				TRANSITION_COMPLETE));
		eventAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, name));
		eventAttributes.put(CONCEPT_INSTANCE, new XAttributeLiteralImpl(CONCEPT_INSTANCE, instance));
		eventAttributes.put(CONCEPT_SIMULATED, new XAttributeBooleanImpl(CONCEPT_SIMULATED, true));
		eventAttributes.put(TIME_TIMESTAMP, new XAttributeTimestampImpl(TIME_TIMESTAMP, firingTime));
		XEvent e = XFactoryRegistry.instance().currentDefault().createEvent(eventAttributes);
		return e;
	}

//	/**
//	 * 
//	 * @param t
//	 * @return
//	 */
//	private Date getFiringTime(Transition t, long duration, Map<Place, List<Long>> placeTimes) {
//		// use max time of incoming places as base and add transition's duration:
//		long startTime = getTransitionStartTime(t, placeTimes, true);
//		long firingTime = startTime + duration;
//		Date firingDate = new Date(firingTime);
//
//		// add new timed tokens
//		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = t.getGraph().getOutEdges(t);
//		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : outEdges) {
//			Place p = (Place) outEdge.getTarget();
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(firingTime);
//		}
//		return firingDate;
//	}

//	/**
//	 * @param t Transition to fire
//	 * @param placeTimes times of the tokens on the places
//	 * @return
//	 */
//	public long getTransitionStartTime(Transition t, Map<Place, List<Long>> placeTimes, boolean removeFromPlaceTimes) {
//		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = t.getGraph().getInEdges(t);
//		long maxTime = 0;
//		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : inEdges) {
//			Place p = (Place) inEdge.getSource();
//			long placeTime = 0;
//			if (placeTimes.containsKey(p)){
//				placeTime = placeTimes.get(p).get(0);
//			}
//			maxTime = Math.max(maxTime, placeTime);
//			if (removeFromPlaceTimes && placeTimes.containsKey(p)){
//				placeTimes.get(p).remove(0);
//			}
//		}
//		return maxTime;
//	}

	/**
	 * Checks if the transition has already a running task (depending on the memory policy, this can be true) and returns that value, or
	 * samples a new value freshly from the distribution.
	 * 
	 * @param t the transition for which the remaining duration (in ms) will be determined 
	 * @param unitFactor the scaling factor to get from the distribution parameters to milliseconds
	 * @param positiveConstraint a constraint that might restrict sample values (left-truncates the distribution) 
	 * @return long milliseconds that the transition has to wait until it will fire. 
	 */
	private long getTransitionRemainingTime(Transition t, TimeUnit unitFactor, double positiveConstraint) {
		// only sample for transitions, that have no memory of their previous enabled periods (stored in the transition clocks)
		if (transitionRemainingTimes.containsKey(t)){
			return transitionRemainingTimes.get(t);
		} else {
			long duration;
			if (t instanceof TimedTransition) {
				TimedTransition timedT = (TimedTransition) t;
				switch (timedT.getDistributionType()) {
					case IMMEDIATE :
						duration = 0;
						break;
					default :
						double sample = StochasticNetUtils.sampleWithConstraint(timedT, positiveConstraint);
						if (sample < positiveConstraint){
							System.out.println("debug me!");
						}
						duration =  (long) (sample * unitFactor.getUnitFactorToMillis());
				}
			} else {
				// untimed net, just progress one unit in time.
				duration = (long) unitFactor.getUnitFactorToMillis();
			}
			transitionRemainingTimes.put(t,duration);
			return duration;
		}
	}

//	private void updatePlaceTimes(Collection<Place> places, Date time, Map<Place, List<Long>> placeTimes) {
//		for (Place p : places) {
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(time.getTime());
//		}
//	}

	/**
	 * 
	 * @param transitions all enabled transitions to pick from.
	 * @param petriNet the underlying Petri net of the simulation.
	 * @param config the configuration of the simulation. See {@link PNSimulatorConfig}. Contains the selection policy!
	 * @param startOfTransition the absolute time of the current marking's last state.
	 * @param constraint all sampled durations should be greater than the constraint
	 * @param usePositiveTimeContraint the simulation might start in the middle of one trace, after some time has passed.
	 * 		  In this case, we don't want to generate samples that are in the past.  (the parameter traceStart sets the constraint's value)

 	 * 
 	 * @return The transition that is picked as the next one to fire with its duration in the current marking 
	 */
	private Pair<Transition, Long> pickTransition(Collection<Transition> transitions, PetrinetGraph petriNet, PNSimulatorConfig config, long startOfTransition, long constraint, boolean usePositiveTimeContraint) {
		if (petriNet instanceof StochasticNet && transitionsContainTimingInfo(transitions)){
			// sanity check of the semantics, to make sure that only immediate transitions, or timed transitions are competing for the right to fire next!
			boolean allImmediate = getOnlyImmediateTransitions(transitions, true);
			boolean allTimed = getOnlyImmediateTransitions(transitions, false);
			
			
			// either transitions are all immediate -> pick one randomly according to their relative weights... 
			if (allImmediate){
				int index = pickTransitionAccordingToWeights(transitions);
				Transition t = getTransitionWithIndex(transitions,index);
				transitionRemainingTimes.put(t,0l);
				return new Pair<Transition, Long>(t, 0l);
			} // or they are all timed -> pick according to firing semantics...
			else if (allTimed){
				// select according to selection policy:
				if (config.executionPolicy.equals(ExecutionPolicy.GLOBAL_PRESELECTION)){
					int index = pickTransitionAccordingToWeights(transitions);
					// restrict the set of enabled transitions to the randomly picked one:
					Transition t = getTransitionWithIndex(transitions,index);
					transitions = new LinkedList<Transition>();
					transitions.add(t);
				}
				// select according to race policy:
				// they are all timed: (truly concurrent or racing for shared tokens)
				SortedMap<Long,Transition> times = new TreeMap<Long, Transition>();
				for (Transition transition :transitions){
					if (usePositiveTimeContraint){
						// calculate minimum transition time that is necessary for transition to be satisfying the constraint (resulting in time bigger than traceStart)
						double samplingConstraint = Math.max(0, (constraint-startOfTransition)/config.unitFactor.getUnitFactorToMillis());
						long now = System.currentTimeMillis();
						long transitionRemainingTime = getTransitionRemainingTime(transition, config.unitFactor, samplingConstraint);
						if (transitionRemainingTime+startOfTransition < constraint){
							transitionRemainingTime = constraint-startOfTransition;
							transitionRemainingTimes.put(transition, transitionRemainingTime);
						}
						long millis = System.currentTimeMillis()-now;
						if (millis > 100){
							System.out.println("sampling took: "+millis+"ms. constraint "+samplingConstraint+", transition: "+transition.getLabel()+" type: "+((TimedTransition)transition).getDistributionType());
						} 
						// make sure transition duration is bigger than constraint (sometimes floating point arithmetic might sample values that are overflowing, or just about the constraint.
						if (!transitionRemainingTimes.containsKey(transition) && transitionRemainingTime+startOfTransition < constraint){
							transitionRemainingTimes.put(transition, (long) (samplingConstraint*config.unitFactor.getUnitFactorToMillis())+1);
							System.out.println("distribution ("+transition.getLabel()+") with constraint: "+samplingConstraint+", mean: "+((TimedTransition)transition).getDistribution().getNumericalMean()+" (Rounding produced Infinity)!!");
						}
						times.put(transitionRemainingTime, transition);
					} else {
						// only allow positive durations:
						times.put(getTransitionRemainingTime(transition, config.unitFactor, 0), transition);
					}
				}
				Transition nextTransition = times.get(times.firstKey());
				return new Pair<Transition, Long>(nextTransition, transitionRemainingTimes.get(nextTransition));
			} else {
				// semantics should make sure, that only the transitions of the highest priority are enabled in the current marking!
				throw new IllegalArgumentException("Stochastic semantics bug! There should either be only immediate or only timed activities enabled!");
			}
		} else {
			// pick randomly:
			int randomPick = random.nextInt(transitions.size());
			Transition t = getTransitionWithIndex(transitions, randomPick);
			return new Pair<Transition, Long>(t,getTransitionRemainingTime(t,config.unitFactor, 0));
		}
	}

	private boolean transitionsContainTimingInfo(Collection<Transition> transitions) {
		boolean allTimed = true;
		for (Transition t : transitions){
			allTimed &= t instanceof TimedTransition;
		}
		return allTimed;
	}
	
	/**
	 * Checks whether all transitions in set are immediate (if flag onlyImmediate is true)
	 * or whether all transitions in set are timed (if flag onlyImmediate is false).
	 * Example: To check, whether all transitions in the set <b>trans<b> are immediate, call: getOnlyImmediateTransitions(trans,true);
	 *  
	 * @param transitions set of transitions to check, whether they belong to the same type (immediate, or not immediate)
	 * @param immediate flag
	 * @return 
	 */
	private boolean getOnlyImmediateTransitions(Collection<Transition> transitions, boolean immediate) {
		boolean allSame = true;
		for (Transition transition : transitions){
			if (transition instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) transition;
				allSame = allSame && (immediate?tt.getDistributionType().equals(DistributionType.IMMEDIATE):!tt.getDistributionType().equals(DistributionType.IMMEDIATE));
			} else {
				return !immediate;
			}
		}
		return allSame;
	}

	public int pickTransitionAccordingToWeights(Collection<Transition> transitions) {
		double[] weights = new double[transitions.size()];
		int i = 0;
		for (Transition transition : transitions){
			TimedTransition tt = (TimedTransition) transition;
			weights[i++] = tt.getWeight();
		}
		int index = StochasticNetUtils.getRandomIndex(weights, random);
		return index;
	}

	private Transition getTransitionWithIndex(Collection<Transition> transitions, int index) {
		Transition t = null;
		Iterator<Transition> iterator = transitions.iterator();
		for (int i = 0; i < index + 1; i++) {
			t = iterator.next();
		}
		return t;
	}

	private Date getNextArrivalDate(Date lastTime, TimeUnit unitFactor) {
		return new Date(lastTime.getTime() + (long) (arrivalDistribution.sample() * unitFactor.getUnitFactorToMillis()));
	}

}
