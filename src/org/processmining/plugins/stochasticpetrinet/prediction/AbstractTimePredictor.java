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

public class AbstractTimePredictor {

	/**
	 * the confidence interval to be used for estimating bounds on the predicted remaining duration 
	 */
	public static final double CONFIDENCE_INTERVAL = 0.99;
	
	/**
	 * Simulation is allowed to stop, when relative error is below this value
	 */
	public static final double ERROR_BOUND_PERCENT = 3;
	
	/**
	 * If we wanted to restrict the number of simulated runs, we could do it here
	 */
	public static final int MAX_RUNS = Integer.MAX_VALUE;

	
	/**
	 * Does not care about final markings -> simulates net until no transitions are enabled any more...
	 * Time
	 * @param {@link StochasticNet} model the model capturing the stochastic behavior of the net
	 * @param observedEvents the monitored partial trace (complete, i.e., no visible transition missing) 
	 * @param currentTime the time of prediction (can be later than the last event's time stamp) 
	 * @param initialMarking initial marking of the net
	 * @param useTime indicator, whether to use the current time as constraint
	 * @return {@link Pair} of doubles (the point predictor, and the associated 99 percent confidence interval)
	 */
	public Pair<Double,Double> predict(StochasticNet model, XTrace observedEvents, Date currentTime, Marking initialMarking) {
		DescriptiveStatistics stats = getPredictionStats(model, observedEvents, currentTime, initialMarking);
//		System.out.println("stopped simulation after "+i+" samples... with error: "+errorPercent+"%.");
		
		StochasticNetUtils.useCache(false);
		//System.out.println("Simulated 1000 traces in "+(System.currentTimeMillis()-now)+"ms ("+(useTime?"constrained":"unconstrained")+")");
		return new Pair<Double,Double>(stats.getMean(),getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL));
	}

	/**
	 * Maximum likelihood estimate for the risk of missing a deadline until the end of the process. 
	 * (Currently, we did not implement the time until we reach a certain state)
	 *  
	 * @param model StochasticNet capturing the stochastic behavior of the net
	 * @param observedEvents the monitored partial trace (complete, i.e., no visible transition missing) 
	 * @param currentTime the time of prediction (can be later than the last event's time stamp) 
	 * @param targetTime the deadline with respect to which the risk is calculated
	 * @param initialMarking initial marking of the net
	 * @param useTime indicator, whether to use the current time as constraint
	 * @return
	 */
	public Double computeRiskToMissTargetTime(StochasticNet model, XTrace observedEvents, Date currentTime, Date targetTime, Marking initialMarking){
		DescriptiveStatistics stats = getPredictionStats(model, observedEvents, currentTime, initialMarking);
		double[] sortedEstimates = stats.getSortedValues();
		long[] longArray = new long[sortedEstimates.length];
		for (int i = 0 ; i < sortedEstimates.length; i++)
		{
		    longArray[i] = (long) sortedEstimates[i];
		}
		
		return 1 - (StochasticNetUtils.getIndexBinarySearch(longArray, targetTime.getTime()) / (double)sortedEstimates.length);
	}
	
	protected DescriptiveStatistics getPredictionStats(StochasticNet model, XTrace observedEvents, Date currentTime,
			Marking initialMarking) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected double getConfidenceIntervalWidth(DescriptiveStatistics summaryStatistics, double confidence) {
		TDistribution tDist = new TDistribution(summaryStatistics.getN() - 1);
		double a = tDist.inverseCumulativeProbability(1-((1-confidence) / 2.));
		return 2 * a * Math.sqrt(summaryStatistics.getVariance() / summaryStatistics.getN());
	}
	
	protected double getErrorPercent(DescriptiveStatistics stats){
		double mean = stats.getMean();
		double confidenceIntervalWidth = getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL);
		return (mean/(mean-confidenceIntervalWidth/2.) - 1) * 100;
	}
	
	
	/**
	 * TODO: Maybe switch to alignment approach
	 * 
	 * @param model
	 * @param initialMarking
	 * @param observedEvents
	 * @return
	 */
	public static Semantics<Marking,Transition> getCurrentState(StochasticNet model, Marking initialMarking, XTrace observedEvents) {
		Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(model);
		semantics.initialize(model.getTransitions(), initialMarking);
		for (XEvent event : observedEvents){
			Set<Marking> visitedMarkings = new HashSet<Marking>();
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


	protected static void addAllEnabledTransitions(
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
	
	protected static void executeTransition(Semantics<Marking, Transition> semantics, Transition transition, Long time) throws IllegalTransitionException{
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
