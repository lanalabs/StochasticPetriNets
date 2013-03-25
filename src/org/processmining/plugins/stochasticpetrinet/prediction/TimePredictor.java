package org.processmining.plugins.stochasticpetrinet.prediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class TimePredictor {
//	Map<Place, List<Long>> placeTimes;
	
	/**
	 * 
	 * @param model the model capturing the stochastic behavior of the net
	 * @param observedEvents the monitored partial trace (complete, i.e., no visible transition missing) 
	 * @param currentTime the time of prediction (usually later than the last event's time stamp) 
	 * @param initialMarking initial marking of the net
	 * @param useTime indicator, whether to use the current time as constraint
	 * @return
	 */
	public double predict(StochasticNet model, XTrace observedEvents, Date currentTime, Marking initialMarking, double unitFactor, boolean useTime) {
		Map<Place, List<Long>> placeTimes = new HashMap<Place, List<Long>>();
		Semantics<Marking,Transition> semantics = getCurrentState(model, initialMarking, observedEvents, placeTimes);
		Marking currentMarking = semantics.getCurrentState();
		Long lastEventTime = ((XAttributeTimestamp)observedEvents.get(observedEvents.size()-1).getAttributes().get(PNSimulator.TIME_TIMESTAMP)).getValueMillis();
//		System.out.println("Time between last event and current time: "+(currentTime.getTime()-lastEventTime)+"ms");
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig config = new PNSimulatorConfig(1,unitFactor);
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		//long now = System.currentTimeMillis(); 
		for (int i = 1; i < 1000; i++){
			Map<Place,List<Long>> pt = new HashMap<Place, List<Long>>();
			for (Place p : placeTimes.keySet()){
				List<Long> l = placeTimes.get(p);
				List<Long> newList = new ArrayList<Long>(l);
				pt.put(p, newList);
			}
			stats.addValue(simulator.simulateTraceEnd(model, semantics, config, currentMarking, currentTime, i, pt, useTime));
			semantics.setCurrentState(currentMarking);
		}
		//System.out.println("Simulated 1000 traces in "+(System.currentTimeMillis()-now)+"ms ("+(useTime?"constrained":"unconstrained")+")");
		return stats.getMean();
	}

	/**
	 * TODO: Maybe switch to alignment approach
	 * 
	 * @param model
	 * @param initialMarking
	 * @param observedEvents
	 * @return
	 */
	private Semantics<Marking,Transition> getCurrentState(StochasticNet model, Marking initialMarking, XTrace observedEvents, Map<Place, List<Long>> placeTimes) {
		Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(model);
		semantics.initialize(model.getTransitions(), initialMarking);
		Set<Marking> visitedMarkings = new HashSet<Marking>();
		for (XEvent event : observedEvents){
			String transitionName = ((XAttributeLiteral)event.getAttributes().get(PNSimulator.CONCEPT_NAME)).getValue();
			Long time = XTimeExtension.instance().extractTimestamp(event).getTime();
			boolean foundTransition = false;
			// breadth-width search for the event transition in the graph from the current marking
//			
//			foundTransition = findAndExecuteTransition(semantics, transitionName, time);
//			if (!foundTransition){
			LinkedList<Pair<Marking, Transition>> transitionQueue = new LinkedList<Pair<Marking, Transition>>();
			addAllEnabledTransitions(semantics, transitionQueue);
			while (!foundTransition && transitionQueue.size() > 0) {
				Pair<Marking, Transition> currentState = transitionQueue.poll();
				semantics.setCurrentState(currentState.getFirst());
				Transition enabledTransition = currentState.getSecond();
				try {
					if (enabledTransition.getLabel().equals(transitionName)) {
						foundTransition = true;
						executeTransition(semantics, enabledTransition, time, placeTimes);
					} else {
						semantics.executeExecutableTransition(enabledTransition);
						if (!visitedMarkings.contains(semantics.getCurrentState())) {
							addAllEnabledTransitions(semantics, transitionQueue);
							visitedMarkings.add(semantics.getCurrentState());
						}
					}
				} catch (IllegalTransitionException e) {
					e.printStackTrace();
				}
			}
			if (!foundTransition) {
				throw new IllegalArgumentException("Did not find transition for \"" + transitionName + "\".");
			}
//			}
		}
		return semantics;
	}

	private void addAllEnabledTransitions(
			Semantics<Marking, Transition> semantics, Collection<Pair<Marking, Transition>> searchState) {
		for (Transition t : semantics.getExecutableTransitions()){
//			if (t.isInvisible()){
				searchState.add(new Pair<Marking,Transition>(semantics.getCurrentState(),t));
//			}
		}
	}

//	private boolean findAndExecuteTransition(Semantics<Marking, Transition> semantics, String transitionName, Long time) {
//		for (Transition t : semantics.getExecutableTransitions()){
//			if (t.getLabel().equals(transitionName)){
//				try {
//					executeTransition(semantics, t, time);
//					return true;
//				} catch (IllegalTransitionException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return false;
//	}
	
	private void executeTransition(Semantics<Marking, Transition> semantics, Transition transition, Long time, Map<Place, List<Long>> placeTimes) throws IllegalTransitionException{
		Marking before = semantics.getCurrentState();
		semantics.executeExecutableTransition(transition);
		Marking after = semantics.getCurrentState();
		Marking oldInvalidPlaces = new Marking(before);
		oldInvalidPlaces.removeAll(after);
		Marking newPlaces = new Marking(after);
		newPlaces.removeAll(before);
		for (Place p : oldInvalidPlaces){
			if (placeTimes.containsKey(p) && placeTimes.get(p).size() > 0){
				placeTimes.get(p).remove(0);
			}
		}
		for (Place p : newPlaces){
			if (!placeTimes.containsKey(p)){
				placeTimes.put(p, new ArrayList<Long>());
			}
			placeTimes.get(p).add(time);
		}
	}
		
}
