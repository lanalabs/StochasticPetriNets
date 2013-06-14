package org.processmining.plugins.stochasticpetrinet.prediction;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorTraceLess;

public class TimePredictor {
//	Map<Place, List<Long>> placeTimes;
	
	public static final double CONFIDENCE_INTERVAL = 0.99;
	public static final double ERROR_BOUND_PERCENT = 3;
	public static final int MAX_RUNS = Integer.MAX_VALUE;
	
	/**
	 * Does not care about final markings -> simulates net until no transitions are enabled any more...
	 *  
	 * @param model the model capturing the stochastic behavior of the net
	 * @param observedEvents the monitored partial trace (complete, i.e., no visible transition missing) 
	 * @param currentTime the time of prediction (usually later than the last event's time stamp) 
	 * @param initialMarking initial marking of the net
	 * @param useTime indicator, whether to use the current time as constraint
	 * @return
	 */
	public Pair<Double,Double> predict(StochasticNet model, XTrace observedEvents, Date currentTime, Marking initialMarking, double unitFactor, boolean useTime) {
		Semantics<Marking,Transition> semantics = getCurrentState(model, initialMarking, observedEvents);
		if (semantics.getCurrentState() == null){
			System.out.println("Debug me!");
		}
		Marking currentMarking = semantics.getCurrentState();
		Long lastEventTime = XTimeExtension.instance().extractTimestamp(observedEvents.get(observedEvents.size()-1)).getTime();
//		System.out.println("Time between last event and current time: "+(currentTime.getTime()-lastEventTime)+"ms");
		PNSimulatorTraceLess simulator = new PNSimulatorTraceLess();
		PNSimulatorConfig config = new PNSimulatorConfig(1,unitFactor);
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		//long now = System.currentTimeMillis();
		
		StochasticNetUtils.useCache(true);
		double errorPercent = 100; // percentage in error of 99% confidence band
		int i = 0;
		while (errorPercent > ERROR_BOUND_PERCENT && i <MAX_RUNS){
			i++;
			stats.addValue((Long)simulator.simulateOneTrace(model, semantics, config, currentMarking, lastEventTime, currentTime.getTime(), i, useTime, null));
			semantics.setCurrentState(currentMarking);
			if (i % 100 == 0){
				// update error:
				errorPercent = getErrorPercent(stats);
			}
		}
		System.out.println("stopped simulation after "+i+" samples... with error: "+errorPercent+"%.");
		
		StochasticNetUtils.useCache(false);
		//System.out.println("Simulated 1000 traces in "+(System.currentTimeMillis()-now)+"ms ("+(useTime?"constrained":"unconstrained")+")");
		return new Pair<Double,Double>(stats.getMean(),getConfidenceIntervalWidth(stats, 0.99));
	}
	
	private double getErrorPercent(DescriptiveStatistics stats){
		double mean = stats.getMean();
		double confidenceIntervalWidth = getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL);
		return (mean/(mean-confidenceIntervalWidth/2.) - 1) * 100;
	}
	
	private double getConfidenceIntervalWidth(DescriptiveStatistics summaryStatistics, double confidence) {
		TDistribution tDist = new TDistribution(summaryStatistics.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1-((1-confidence) / 2.));
		return 2 * a * Math.sqrt(summaryStatistics.getVariance() / summaryStatistics.getN());
	}

	/**
	 * TODO: Maybe switch to alignment approach
	 * 
	 * @param model
	 * @param initialMarking
	 * @param observedEvents
	 * @return
	 */
	private Semantics<Marking,Transition> getCurrentState(StochasticNet model, Marking initialMarking, XTrace observedEvents) {
		Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(model);
		semantics.initialize(model.getTransitions(), initialMarking);
		Set<Marking> visitedMarkings = new HashSet<Marking>();
		for (XEvent event : observedEvents){
			String transitionName = XConceptExtension.instance().extractName(event);
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
						executeTransition(semantics, enabledTransition, time);
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
	
	private void executeTransition(Semantics<Marking, Transition> semantics, Transition transition, Long time) throws IllegalTransitionException{
		Marking before = semantics.getCurrentState();
		semantics.executeExecutableTransition(transition);
		Marking after = semantics.getCurrentState();
		Marking oldInvalidPlaces = new Marking(before);
		oldInvalidPlaces.removeAll(after);
		Marking newPlaces = new Marking(after);
		newPlaces.removeAll(before);
//		for (Place p : oldInvalidPlaces){
//			if (placeTimes.containsKey(p) && placeTimes.get(p).size() > 0){
//				placeTimes.get(p).remove(0);
//			}
//		}
//		for (Place p : newPlaces){
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(time);
//		}
	}
		
}
