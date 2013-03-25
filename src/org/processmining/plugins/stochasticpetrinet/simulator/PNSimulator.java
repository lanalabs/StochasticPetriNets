package org.processmining.plugins.stochasticpetrinet.simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
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
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * Very plain simulator only used for quick&dirty evaluation of the repair-log
 * plug-in. Works with simple stochastic Petri nets.
 * 
 * ProM is no simulator!! By all means use CPNTools (http://cpntools.org) if you 
 * want to have a feature-rich stable simulation and analysis tool for all kinds of Petri Nets
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
	 * Asks the user to specify configuration parameters for the simulation.
	 * {@link PNSimulatorConfig}
	 * @param context
	 * @param petriNet
	 * @param semantics
	 * @return
	 */
	public XLog simulate(UIPluginContext context, PetrinetGraph petriNet, Semantics<Marking, Transition> semantics) {
		PNSimulatorConfigUI ui = new PNSimulatorConfigUI();
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
		XLog log = null;
		if (config != null) {
			arrivalDistribution = new ExponentialDistribution(config.arrivalRate);
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
			
			for (int i = 0; i < config.numberOfTraces; i++) {
				
				if (context != null){
					context.getProgress().setValue(i);
				}

				traceStart = getNextArrivalDate(traceStart, config.unitFactor);
				Map<Place, List<Long>> placeTimes = new HashMap<Place, List<Long>>();
				updatePlaceTimes(initialMarking, traceStart, placeTimes);
				semantics.initialize(petriNet.getTransitions(), initialMarking);
				
				XTrace trace = simulateOneTrace(petriNet, semantics, config, initialMarking, traceStart, i, placeTimes, false);
				log.add(trace);
			}
			if (context != null){
				context.log(config.numberOfTraces+" traces generated successfully.");
			}
		}
		return log;
	}

	/**
	 * 
	 * @param petriNet the model
	 * @param semantics the semantics 
	 * @param config the configuration {@link PNSimulatorConfig}
	 * @param initialMarking the initial Marking
	 * @param traceStart the date to start
	 * @param i trace id
	 * @param placeTimes already initialized Map of places and times
	 * @param useTime stores whether created events are constrained to be later than traceStart
	 * @return
	 */
	public XTrace simulateOneTrace(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
			PNSimulatorConfig config, Marking initialMarking, Date traceStart, int i, Map<Place, List<Long>> placeTimes, boolean useTime) {
		XAttributeMap traceAttributes = new XAttributeMapImpl();
		traceAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, String.valueOf(i)));
		XTrace trace = new XTraceImpl(traceAttributes);
		
		Collection<Transition> transitions = semantics.getExecutableTransitions();
		int eventsProduced = 0;

		while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace) {
			try {
				List<Pair<Transition, Long>> transitionAndDurations = pickTransitions(transitions, petriNet, config, traceStart, placeTimes, useTime);
				
				for (Pair<Transition,Long> transitionAndDuration : transitionAndDurations){
					semantics.executeExecutableTransition(transitionAndDuration.getFirst());
					Date firingTime = getFiringTime(transitionAndDuration.getFirst(), transitionAndDuration.getSecond(), placeTimes);
					if (useTime && !transitionAndDuration.getFirst().isInvisible() && firingTime.getTime() < traceStart.getTime()){
						System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
					}
					if (!transitionAndDuration.getFirst().isInvisible()){
						XEvent e = createSimulatedEvent(transitionAndDuration.getFirst().getLabel(), firingTime, String.valueOf(i));
						trace.insertOrdered(e);
					}
				}
				transitions = semantics.getExecutableTransitions();
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
				break;
			}
		}
		return trace;
	}
	
	/**
	 * Same as {@link #simulateOneTrace(PetrinetGraph, Semantics, PNSimulatorConfig, Marking, Date, int, Map, boolean)}, but without timeconsuming XID generation. 
	 * @param petriNet
	 * @param semantics
	 * @param config
	 * @param initialMarking
	 * @param traceStart
	 * @param i
	 * @param placeTimes
	 * @param useTime
	 * @return
	 */
	public Long simulateTraceEnd(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
			PNSimulatorConfig config, Marking initialMarking, Date traceStart, int i, Map<Place, List<Long>> placeTimes, boolean useTime) {
		Collection<Transition> transitions = semantics.getExecutableTransitions();
		int eventsProduced = 0;
		SortedSet<Long> transitionTimes = new TreeSet<Long>();

		while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace) {
			try {
				List<Pair<Transition, Long>> transitionAndDurations = pickTransitions(transitions, petriNet, config, traceStart, placeTimes, useTime);
				
				for (Pair<Transition,Long> transitionAndDuration : transitionAndDurations){
					semantics.executeExecutableTransition(transitionAndDuration.getFirst());
					Date firingTime = getFiringTime(transitionAndDuration.getFirst(), transitionAndDuration.getSecond(), placeTimes);
					if (useTime && !transitionAndDuration.getFirst().isInvisible() && firingTime.getTime() < traceStart.getTime()){
						System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
					}
					if (!transitionAndDuration.getFirst().isInvisible()){
						transitionTimes.add(firingTime.getTime());
					}
				}
				transitions = semantics.getExecutableTransitions();
			} catch (IllegalTransitionException e) {
				e.printStackTrace();
				break;
			}
		}
		return transitionTimes.last();
	}
	

	private XEvent createSimulatedEvent(String name, Date firingTime, String instance) {
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

	/**
	 * 
	 * @param t
	 * @return
	 */
	private Date getFiringTime(Transition t, long duration, Map<Place, List<Long>> placeTimes) {
		// use max time of incoming places as base and add transition's duration:
		long startTime = getTransitionStartTime(t, placeTimes, true);
		long firingTime = startTime + duration;
		Date firingDate = new Date(firingTime);

		// add new timed tokens
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = t.getGraph().getOutEdges(t);
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : outEdges) {
			Place p = (Place) outEdge.getTarget();
			if (!placeTimes.containsKey(p)){
				placeTimes.put(p, new ArrayList<Long>());
			}
			placeTimes.get(p).add(firingTime);
		}
		return firingDate;
	}

	/**
	 * @param t Transition to fire
	 * @param placeTimes times of the tokens on the places
	 * @return
	 */
	public long getTransitionStartTime(Transition t, Map<Place, List<Long>> placeTimes, boolean removeFromPlaceTimes) {
		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = t.getGraph().getInEdges(t);
		long maxTime = 0;
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : inEdges) {
			Place p = (Place) inEdge.getSource();
			long placeTime = 0;
			if (placeTimes.containsKey(p)){
				placeTime = placeTimes.get(p).get(0);
			}
			maxTime = Math.max(maxTime, placeTime);
			if (removeFromPlaceTimes && placeTimes.containsKey(p)){
				placeTimes.get(p).remove(0);
			}
		}
		return maxTime;
	}

	private long getTransitionDuration(Transition t, double unitFactor, double positiveConstraint) {
		if (t instanceof TimedTransition) {
			TimedTransition timedT = (TimedTransition) t;
			switch (timedT.getDistributionType()) {
				case IMMEDIATE :
					return 0;
				default :
					double sample = StochasticNetUtils.sampleWithConstraint(timedT.getDistribution(), random, positiveConstraint);
					return (long) (sample * unitFactor);
			}
		} else {
			// untimed net, just progress one unit in time.
			return (long) unitFactor;
		}
	}

	private void updatePlaceTimes(Collection<Place> places, Date time, Map<Place, List<Long>> placeTimes) {
		for (Place p : places) {
			if (!placeTimes.containsKey(p)){
				placeTimes.put(p, new ArrayList<Long>());
			}
			placeTimes.get(p).add(time.getTime());
		}
	}

	/**
	 * 
	 * @param transitions
	 * @param petriNet
	 * @param useTime 
	 * @param traceStart 
	 * @param useDeterministicMeanTimes 
	 * @return
	 */
	private List<Pair<Transition, Long>> pickTransitions(Collection<Transition> transitions, PetrinetGraph petriNet, PNSimulatorConfig config, Date traceStart, Map<Place, List<Long>> placeTimes, boolean useTime) {
		List<Pair<Transition,Long>> selectedTransitions = new ArrayList<Pair<Transition,Long>>();
		if (petriNet instanceof StochasticNet){
			boolean allImmediate = true;
			boolean allTimed = true;
			Map<Transition,Marking> transitionsToMarking = new HashMap<Transition, Marking>();
			for (Transition transition : transitions){
				TimedTransition tt = (TimedTransition) transition;
				allImmediate = allImmediate && tt.getDistributionType().equals(DistributionType.IMMEDIATE);
				allTimed = allTimed && !tt.getDistributionType().equals(DistributionType.IMMEDIATE);
			}
			// either transitions are all immediate
			if (allImmediate){
				double[] weights = new double[transitions.size()];
				int i = 0;
				for (Transition transition : transitions){
					TimedTransition tt = (TimedTransition) transition;
					weights[i++] = tt.getWeight();
				}
				int index = StochasticNetUtils.getRandomIndex(weights, random);
				selectedTransitions.add(new Pair<Transition, Long>(getTransitionWithIndex(transitions,index), 0l));
				return selectedTransitions;
			} else if (allTimed){
				// check if they compete for the same tokens:
				for (Transition t: transitions){
					Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = t.getGraph().getInEdges(t);
					Marking in = new Marking();
					for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inEdges){
						in.add((Place) edge.getSource());
					}
					transitionsToMarking.put(t, in);
				}
				// they are all timed: (truly concurrent or racing for shared tokens)
				SortedMap<Long,Transition> times = new TreeMap<Long, Transition>();
				for (Transition transition :transitions){
					if (useTime){
						// calculate minimum transition time that is necessary for transition to be satisfying the constraint (resulting in time bigger than traceStart)
						long startOfTransition  = getTransitionStartTime(transition, placeTimes, false);
						double constraint = Math.max(0, (traceStart.getTime()-startOfTransition)/config.unitFactor);
						long now = System.currentTimeMillis();
						long transitionDuration = getTransitionDuration(transition, config.unitFactor, constraint);
						long millis = System.currentTimeMillis()-now;
						if (millis > 100){
							System.out.println("sampling took: "+millis+"ms. constraint "+constraint+", transition: "+transition.getLabel());
						}
						if (transitionDuration+startOfTransition < traceStart.getTime()){
							transitionDuration = (long) (constraint*config.unitFactor)+1;
							System.out.println("distribution ("+transition.getLabel()+") with constraint: "+constraint+", mean: "+((TimedTransition)transition).getDistribution().getNumericalMean()+" (Rounding produced Infinity)!!");
						}
						times.put(transitionDuration, transition);
					} else {
						// only allow positive durations:
						times.put(getTransitionDuration(transition, config.unitFactor, 0), transition);
					}
				}
				Marking usedPlaces = new Marking();
				// times sampled: add all places to the set
				for (Long time : times.keySet()){
					Transition t = times.get(time);
					Marking in = transitionsToMarking.get(t);
					boolean placeInConflict = false;
					for (Place p : in){
						placeInConflict = placeInConflict || usedPlaces.contains(p);
					}
					if (placeInConflict){
						// transition got inactivated by an earlier transition
					} else {
						usedPlaces.addAll(in);
						selectedTransitions.add(new Pair<Transition,Long>(t,time));
					}
				}
				return selectedTransitions;
			} else {
				throw new IllegalArgumentException("Stochastic semantics bug! There should either be only immediate or only timed activities enabled!");
			}
		} else {
			// pick randomly:
			int randomPick = random.nextInt(transitions.size());
			Transition t = getTransitionWithIndex(transitions, randomPick);
			selectedTransitions.add(new Pair<Transition, Long>(t,getTransitionDuration(t,config.unitFactor, 0)));
			return selectedTransitions;
		}
	}

	private Transition getTransitionWithIndex(Collection<Transition> transitions, int index) {
		Transition t = null;
		Iterator<Transition> iterator = transitions.iterator();
		for (int i = 0; i < index + 1; i++) {
			t = iterator.next();
		}
		return t;
	}

	private Date getNextArrivalDate(Date lastTime, double unitFactor) {
		return new Date(lastTime.getTime() + (long) (arrivalDistribution.sample() * unitFactor));
	}

}
