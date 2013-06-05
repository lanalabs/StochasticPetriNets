package org.processmining.plugins.stochasticpetrinet.enricher;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.plugins.manifestanalysis.visualization.performance.PerfCounter;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.stochasticpetrinet.distribution.RCensoredLogSplineDistribution;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.GradientDescent;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.MarkingBasedSelectionWeightCostFunction;

/**
 * Collects statistics of the network given an alignment.
 * Does so by taking into account different semantics.
 * 
 * Aim is to reconstruct the original model parameters, such that a simulation of the model would 
 * give similar results, as observed in the log.
 * 
 * Problem: some semantics make it hard to reason about the original transition distributions.
 * For example, in the race selection policy, only the winning transitions' time is recorded, 
 * and the other transitions that lose a race, might lose their progress depending on the memory policy:
 * 
 * 1.) in the resampling case, these times are lost, and the reconstruction of distributions is hard.
 * 2.) in the enabling memory case, all parallel transitions are fine, as they keep their progress, and 
 *     only conflicting transitions that lose a race will not leave a trace of their sample in the event log.
 * 3.) the age memory, allows losing transitions to keep their progress, even in conflict. 
 *     However, if they will not have a chance to finish and fire eventually, before the process ends,
 *     their sample durations will be lost, too. 
 * 
 * Learning distributions from data, that is partly censored is possible, and there are some algorithms (e.g., EM-algorithm)
 * that can be used to fit a model to the data. See {@link RCensoredLogSplineDistribution}, which provides such functionality through an R-binding.
 * 
 * In the global preselection policy, only one transition is being processed, such that all other transitions have to wait, even if they are in parallel.
 * Since the distribution times are never hidden, (there is no racing), collecting them is easy. We need to collect information about the weights in each marking
 * and later average the weights out, such that their relation will be in accordance with the observed behavior. 
 * See {@link GradientDescent} and {@link MarkingBasedSelectionWeightCostFunction} in the optimizer package.
 * 
 * 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class CollectorCounter extends ReliableInvisibleTransitionPerfCounter{

	/** stores firing times for transitions */
	protected Map<Integer, List<Double>> firingTimes;
	/** stores all censored sample times, where the transition could not use it's sampled duration, because it lost against another. */
	protected Map<Integer, List<Double>> censoredTimes;
	
	/** Stores the age of a transition in ms since the last sampling period */ 
	protected Map<Integer, Long> ageVariables;
	
	/** The time of the last event, that was replayed in the model */
	protected long lastFiringTime;
	
	private ManifestEvClassPattern manifest;
	
	/** for each visited marking, collect the number of times a transition was picked.
	* the transitions are indexed by their encoded id used in the parent class's {@link #getTrans2Idx()}
	* the values in the array are the counts for the different observed next transitions.
	*/  
	protected Map<short[], int[]> markingBasedSelections;
	
	
	private short[] currentMarking;
	
	/**
	 * Stores the transitions that are enabled in the current marking.
	 */
	private Set<Integer> currentlyEnabled;
	
	/**
	 * Stores the transitions that were disabled by the current transition firing.
	 * Used for updating these in enabling-memory mode.
	 */
	private Set<Integer> disabledTransitions;
	
	/** {@link ExecutionPolicy} of the net for which the performance is to be collected */
	private ExecutionPolicy executionPolicy;
	
	public CollectorCounter(ManifestEvClassPattern manifest, ExecutionPolicy executionPolicy){
		super(null);
		this.manifest = manifest;
		this.executionPolicy  = executionPolicy;
		currentlyEnabled = new HashSet<Integer>();
		ageVariables = new HashMap<Integer, Long>();
		firingTimes = new HashMap<Integer, List<Double>>();
		censoredTimes = new HashMap<Integer, List<Double>>();
		markingBasedSelections = new HashMap<short[], int[]>();
	}
	
	

//	public void init(ManifestEvClassPattern manifest, String timeAtt, Class<?> c, boolean[] caseFilter) {
//		super.init(manifest, timeAtt, c, caseFilter);
//	}



	protected void updateMarkingMoveModel(TIntObjectMap<List<Long>> timedPlaces, short[] marking,
			int encTrans) {
		addMarkingTransitionCounter(marking, encTrans);
		
		super.updateMarkingMoveModel(timedPlaces, marking, encTrans);

		// add an entry for model move only:
		if (firingTimes.get(encTrans) == null){
			firingTimes.put(encTrans, new ArrayList<Double>());
		}
		firingTimes.get(encTrans).add(Double.NaN);
	}


	/**
	 * Adds 1 to the firing counts of a transition in a marking.
	 * 
	 * @param marking the marking in which the transition fired
	 * @param encTrans the transition that fired
	 */
	private void addMarkingTransitionCounter(short[] marking, int encTrans) {
		boolean markingFound = false;
		short[] markingInMap = null;
		for (short[] m : markingBasedSelections.keySet()){
			boolean equals = true;
			for (int i= 0; i < m.length; i++){
				equals = equals && marking[i] == m[i];
			}
			if(equals){
				markingInMap = m;
			}
		}
		if (markingInMap == null){
			markingInMap = marking;
		}
		if (!markingBasedSelections.containsKey(markingInMap)){
			markingBasedSelections.put(marking, new int[idx2Trans.length]);
		}
		markingBasedSelections.get(markingInMap)[encTrans]++;
		currentMarking = null;
	}
	
	/**
	 * Finds all transitions that are enabled in the given marking and returns 
	 * a set of their indices (as used in {@link PerfCounter}). 
	 * @param marking a marking containing the number of tokens on each place?
	 * @return
	 */
	private Set<Integer> getConcurrentlyEnabledTransitions(short[] marking){
		// go through all transitions and check, whether all their 
		// input places are set within the marking
		Set<Integer> enabledTransitions = new HashSet<Integer>();
		for (int tId = 0; tId < idx2Trans.length; tId++){ // tId is encoded transition id
			// go through all input places of the transition:
			short[] pred = this.encodedTrans2Pred.get(tId);
			boolean transitionIsEnabled = true;
			for (short p : pred){
				boolean predecessorIsInMarking = false;
				if (marking[p] > 0) {
					predecessorIsInMarking = true;
				}
//				for (short place : marking){
//					if (place == p){
//						predecessorIsInMarking = true;
//					}
//				}
				transitionIsEnabled = transitionIsEnabled && predecessorIsInMarking;
			}
			if (transitionIsEnabled){
				enabledTransitions.add(tId);
			}
		}
		return enabledTransitions;
	}
	
	protected void updateManifestSojournTime(Long lastTokenTakenTime, long firingTime, int patternIDOfManifest) {
		super.updateManifestSojournTime(lastTokenTakenTime, firingTime, patternIDOfManifest);
		
		// stupid reverse engineering hack to get back the manifest id and its transition id, which we need later.
		int encodedTransitionId = -1;
		int i = 0;
		while (encodedTransitionId == -1){
			int manifestPatternId = manifest.getPatternIDOfManifest(i);
			if (manifestPatternId == patternIDOfManifest){
				encodedTransitionId = manifest.getEncTransOfManifest(i);
			}
			i++;
		} 
		
		// add an entry for synchronous move:
		if (firingTimes.get(encodedTransitionId) == null){
			firingTimes.put(encodedTransitionId, new ArrayList<Double>());
		}
		double timeForTransition = 0;
		if (lastTokenTakenTime == null){
			// this happens, when transitions that are not invisible, did not move synchronously before.
			firingTimes.get(encodedTransitionId).add(timeForTransition);
		} else {
			timeForTransition = (double)(firingTime - lastTokenTakenTime);
			
			if (executionPolicy.equals(ExecutionPolicy.RACE_AGE_MEMORY)){
				timeForTransition += getAge(encodedTransitionId);
			}
			
			if (executionPolicy.equals(ExecutionPolicy.RACE_ENABLING_MEMORY)){
				// add a censored time for all transitions that lost the race and got disabled:
				for (int disabledTransitionId : disabledTransitions){
					if (disabledTransitionId != encodedTransitionId){
						// lost the race and got disabled (had a value greater than the winning transition):
						censoredTimes.get(disabledTransitionId).add(timeForTransition);
					}
				}
			}
			
			if (executionPolicy.equals(ExecutionPolicy.RACE_RESAMPLING)){
				// add a censored time for all transitions that lost the race:
				for (int enabledTransitionId : currentlyEnabled){
					if (enabledTransitionId != encodedTransitionId){
						// lost the race (had a value greater than the winning transition):
						censoredTimes.get(enabledTransitionId).add(timeForTransition);
					}
				}
			}
			
			firingTimes.get(encodedTransitionId).add(timeForTransition);
		}
		if (currentMarking != null){
			addMarkingTransitionCounter(currentMarking, encodedTransitionId);
		} 
	}

	public List<Double> getPerformanceData(int encodedTransID){
		List<Double> cleanPerformanceData = new LinkedList<Double>();
		if (firingTimes.containsKey(encodedTransID)){
			for (Double d : firingTimes.get(encodedTransID)){
				if (!Double.isNaN(d)){
					cleanPerformanceData.add(d);
				}
			}
		}
		return cleanPerformanceData;
	}
	
	public int getDataCount(int encodedTransID){
		if (firingTimes.containsKey(encodedTransID)){
			return firingTimes.get(encodedTransID).size();
		}
		return 0;
	}
	
	public Map<short[],int[]> getMarkingBasedSelections(){
		return markingBasedSelections;
	}



	protected Long takeTokens(TIntObjectMap<List<Long>> timedPlaces, short[] marking, int encTrans, long currentEventFiringTime) {
		long timeSpendInMarking = 0;
		if (lastFiringTime > -1){
			timeSpendInMarking = currentEventFiringTime-lastFiringTime;
		}
		lastFiringTime = currentEventFiringTime;
		
		currentMarking = marking.clone();
		currentlyEnabled = getConcurrentlyEnabledTransitions(currentMarking);
		Long maxInputTokenTime = super.takeTokens(timedPlaces, marking, encTrans, currentEventFiringTime);
		Set<Integer> afterwardsEnabled = getConcurrentlyEnabledTransitions(marking);
		disabledTransitions = new HashSet<Integer>();
		for (Integer enabledBefore : currentlyEnabled){
			if (enabledBefore != encTrans && !afterwardsEnabled.contains(enabledBefore)){
				disabledTransitions.add(enabledBefore);
			}
		}
		
		/* 
		 * age memory and enabling memory are default, that is time is calculated based on tokens, however:
		 * in age memory the enabling time has to be accumulated:
		 */
		if (executionPolicy.equals(ExecutionPolicy.RACE_ENABLING_MEMORY)){
			// this is default -> execution times are read off tokens.
			// subtracting the current time from the maximum of the input tokens is correct 
			// age variables are ignored
		} else if (executionPolicy.equals(ExecutionPolicy.RACE_AGE_MEMORY) && maxInputTokenTime != null){
			for (Integer disabled:disabledTransitions){
				// increment age variables of transitions by the firing duration of the current transition:
				long previousAge = getAge(disabled);
				previousAge += currentEventFiringTime-maxInputTokenTime;
				ageVariables.put(disabled, previousAge);
			}
			// reset age variable of firing transition
			ageVariables.put(encTrans,0l);
		} 
		/* In race - resampling, EVERY transition loses its progress after firing, that is has to start new */
		/* in global preselection, ONLY ONE transition makes progress at a time */
		else if (executionPolicy.equals(ExecutionPolicy.RACE_RESAMPLING) || executionPolicy.equals(ExecutionPolicy.GLOBAL_PRESELECTION)){
			// update token times to ALL be the last event firing time = resetting all timers
			for (int key : timedPlaces.keys()){
				Long[] newValues = new Long[timedPlaces.get(key).size()];
				Arrays.fill(newValues, currentEventFiringTime);
				timedPlaces.put(key, new LinkedList<Long>(Arrays.asList(newValues)));
			}
		}
		
		return maxInputTokenTime;
	}



	private long getAge(Integer tId) {
		if (!ageVariables.containsKey(tId)){
			ageVariables.put(tId, 0l);
		}
		return ageVariables.get(tId);
	}
	
	
	
}
