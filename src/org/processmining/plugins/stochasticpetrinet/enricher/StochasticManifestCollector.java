package org.processmining.plugins.stochasticpetrinet.enricher;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.manifestanalysis.visualization.performance.PerfCounter;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.stochasticpetrinet.distribution.RCensoredLogSplineDistribution;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.GradientDescent;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.MarkingBasedSelectionWeightCostFunction;

import cern.colt.Arrays;

/**
 * Simple analyzer for a manifest based replay.
 * 
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
 * @author Andreas Rogge-Solti
 */

public class StochasticManifestCollector {
	/** stores firing times for transitions */
	protected Map<Integer, List<Double>> firingTimes;
	/** stores all censored sample times, where the transition could not use it's sampled duration, because it lost against another. */
	protected Map<Integer, List<Double>> censoredTimes;
	
	/** Stores the age of a transition in model time units since the last sampling period */ 
	protected Map<Integer, Double> ageVariables;
	
	protected ManifestEvClassPattern manifest;
	
	/**
	 * Mapping the Petri net to integer-encoded numbers
	 */
	protected Transition[] idx2Trans;
	
	private TObjectIntMap<Transition> trans2Idx;

	private Place[] idx2Place;
	private TObjectIntMap<Place> place2Idx;
	// utility: encode the net
	protected TIntObjectHashMap<short[]> encodedTrans2Pred;
	protected TIntObjectHashMap<short[]> encodedTrans2Succ;
	
	/** for each visited marking, collect the number of times a transition was picked.
	* the transitions are indexed by their encoded id used in the parent class's {@link #getTrans2Idx()}
	* the values in the array are the counts for the different observed next transitions.
	*/  
	protected Map<String, int[]> markingBasedSelections;
	
	/**
	 * Stores the longest trace's duration in time units of the model
	 */
	protected double longestTrace = 0;
	
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
	/** Configuration for the enricher */
	private PerformanceEnricherConfig config;
	
	/**
	 * Captures the average fitness of the log and the model.
	 */
	private DescriptiveStatistics fitnessStatistic;
	
	public StochasticManifestCollector(ManifestEvClassPattern manifest, ExecutionPolicy executionPolicy, PerformanceEnricherConfig config){
		this.manifest = manifest;
		this.executionPolicy  = executionPolicy;
		this.config = config;
		currentlyEnabled = new HashSet<Integer>();
		ageVariables = new HashMap<Integer, Double>();
		firingTimes = new HashMap<Integer, List<Double>>();
		censoredTimes = new HashMap<Integer, List<Double>>();
		markingBasedSelections = new HashMap<String, int[]>();
		fitnessStatistic = new DescriptiveStatistics();
		
		// init transitions
		PetrinetGraph net = manifest.getNet();
		List<Transition> transitions = new ArrayList<Transition>(net.getTransitions());
		int transSize = transitions.size();
		idx2Trans = transitions.toArray(new Transition[transSize]);
		trans2Idx = new TObjectIntHashMap<Transition>(transSize);
		for (int i = 0; i < idx2Trans.length; i++) {
			trans2Idx.put(idx2Trans[i], i);
			
			firingTimes.put(i, new ArrayList<Double>());
			censoredTimes.put(i, new ArrayList<Double>());
			// TODO: add interval censored times (this is for model moves, i.e., we know an upper border for the transitions in between)
		}
		
		// init places
		List<Place> places = new ArrayList<Place>(net.getPlaces());
		int placeSize = places.size();
		idx2Place = places.toArray(new Place[placeSize]);
		place2Idx = new TObjectIntHashMap<Place>(placeSize);
		for (int i = 0; i < idx2Place.length; i++) {
			place2Idx.put(idx2Place[i], i);
		}

		// init input and output place
		encodedTrans2Pred = new TIntObjectHashMap<short[]>(idx2Trans.length);
		encodedTrans2Succ = new TIntObjectHashMap<short[]>(idx2Trans.length);
		for (int i = 0; i < transSize; i++) {
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(idx2Trans[i]);
			if (inEdges != null) {
				short[] newIn = new short[placeSize];
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : inEdges) {
					int sourceEnc = place2Idx.get(edge.getSource());
					if (edge instanceof ResetArc) {
						newIn[sourceEnc] = (short) (-newIn[sourceEnc] - 1);
					} else if (edge instanceof Arc) {
						if (newIn[sourceEnc] < 0) {
							newIn[sourceEnc] = (short) -net.getArc(edge.getSource(), edge.getTarget()).getWeight();
						} else {
							newIn[sourceEnc] = (short) net.getArc(edge.getSource(), edge.getTarget()).getWeight();
						}
					}
				}
				encodedTrans2Pred.put(i, newIn);
			}
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = net.getOutEdges(idx2Trans[i]);
			if (outEdges != null) {
				short[] newOut = new short[placeSize];
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outEdges) {
					newOut[place2Idx.get(edge.getTarget())] = (short) net.getArc(edge.getSource(), edge.getTarget())
							.getWeight();
				}
				encodedTrans2Succ.put(i, newOut);
			}
		}
	}
	
	public void collectDataFromManifest(){
		// performance calculation
		int[] cases = manifest.getCasePointers();
		for (int i = 0; i < cases.length; i++) {
			Pair<Long,Long> caseBounds = new Pair<Long, Long>(-1l, -1l);
			if (cases[i] >= 0) {
				// create initial marking
				short[] marking = constructEncInitMarking(manifest.getInitMarking());
				// reset last firing time
				long lastFiringTime = Long.MIN_VALUE;
				ageVariables.clear();
				
				// create trace iterator
				Iterator<XEvent> it = manifest.getLog().get(i).iterator();

				// now, iterate through all manifests for the case
				int[] man = manifest.getManifestForCase(i);
				int stepsInAlignment = 0;
				int synchronousAndInvisibleMoves = 0; 
				int currIdx = 0;
				while (currIdx < man.length) {
					if (man[currIdx] == Manifest.MOVELOG) {
						XEvent currEvent = it.next();
						long timeStamp = XTimeExtension.instance().extractTimestamp(currEvent).getTime();
						caseBounds = updateCaseBounds(timeStamp, caseBounds);
						// ignore for now... TODO: maybe use logmoves too
						currIdx++;
						stepsInAlignment++;
					} else if (man[currIdx] == Manifest.MOVEMODEL) {
						double time= Double.NaN;
						if (idx2Trans[man[currIdx+1]].isInvisible()){
							synchronousAndInvisibleMoves++;
							time = 0; // assume that invisible transitions are immediate
						}
						updateMarking(marking, man[currIdx+1], time);
						currIdx += 2;
						stepsInAlignment++;
					} else if (man[currIdx] == Manifest.MOVESYNC) {
						// shared variable
						XEvent currEvent = it.next();
						// extract time information 
						long currEventTime = XTimeExtension.instance().extractTimestamp(currEvent).getTime();
						caseBounds = updateCaseBounds(currEventTime, caseBounds);
						long timeSpentInMarking = 0;
						if (lastFiringTime!=Long.MIN_VALUE){
							timeSpentInMarking = currEventTime - lastFiringTime;
						}
						lastFiringTime = currEventTime;
						updateMarking(marking, manifest.getEncTransOfManifest(man[currIdx+1]), timeSpentInMarking / config.getUnitFactor());
						currIdx += 2;
						stepsInAlignment++;
						synchronousAndInvisibleMoves++;
					}
				}
				double traceDurationInUnits = (caseBounds.getSecond()-caseBounds.getFirst())/ config.getUnitFactor();
				longestTrace = Math.max(longestTrace,traceDurationInUnits);
				fitnessStatistic.addValue(synchronousAndInvisibleMoves/(double)stepsInAlignment);
				// after every replay, we collect the age variables:
				for (Integer transitionId : ageVariables.keySet()){
					censoredTimes.get(transitionId).add(ageVariables.get(transitionId));
				}
				ageVariables.clear();
			}
		}
	}

	private Pair<Long, Long> updateCaseBounds(long timeStamp, Pair<Long,Long> caseBounds) {
		long start = caseBounds.getFirst();
		long end = caseBounds.getSecond();
		if(start < 0){
			start = timeStamp;
		}
		end = timeStamp;
		return new Pair<Long, Long>(start, end);
	}

	private void updateMarking(short[] marking, int encTrans, double timeSpentInMarking) {
		if (timeSpentInMarking < 0){
			System.out.println("Debug me! time should not be < 0!");
		}
		
		currentlyEnabled = getConcurrentlyEnabledTransitions(marking);
		fireTransitionInMarking(marking, encTrans);
		Set<Integer> afterwardsEnabled = getConcurrentlyEnabledTransitions(marking);
		
		// collect conflicting transitions that get disabled by this transition firing
		disabledTransitions = new HashSet<Integer>();
		for (Integer enabledBefore : currentlyEnabled){
			if (enabledBefore != encTrans && !afterwardsEnabled.contains(enabledBefore)){
				disabledTransitions.add(enabledBefore);
			}
		}
		
		if (!Double.isNaN(timeSpentInMarking)){
			// we know that (0, or more) time passed! update transition ages of enabled transitions, if applicable
			if (executionPolicy.equals(ExecutionPolicy.GLOBAL_PRESELECTION)){
				// add the time spent in the marking to the sole active transition:
				firingTimes.get(encTrans).add(timeSpentInMarking);
				// don't use transition ages
			} else if (executionPolicy.equals(ExecutionPolicy.RACE_RESAMPLING)){
				// add the time spent in the marking to the sole active transition:
				firingTimes.get(encTrans).add(timeSpentInMarking);
				// add right-censored values for all the other enabled transitions in this marking:
				if (timeSpentInMarking>0){ 	// ignore vanishing markings of immediate transitions 
					for (Integer enabledBefore : currentlyEnabled){
						if (enabledBefore != encTrans){
							censoredTimes.get(enabledBefore).add(timeSpentInMarking);
						}
					}
				}
				// don't use transition ages
			} else {
				// either race - enabling, or race - age (minor difference: reset age for disabled transitions, only)
				
				// get enabled time of transition:
				double transitionEnabledTime = 0;
				if (ageVariables.containsKey(encTrans)){
					transitionEnabledTime = ageVariables.remove(encTrans);
				}
				firingTimes.get(encTrans).add(timeSpentInMarking+transitionEnabledTime);
				
				if (timeSpentInMarking > 0){
					for (Integer enabled : currentlyEnabled){
						// update other enabled transition ages:
						if (enabled != encTrans){
							if (!ageVariables.containsKey(enabled)){
								ageVariables.put(enabled, 0.);
							}
							ageVariables.put(enabled, ageVariables.get(enabled)+timeSpentInMarking);
						}
					}
				}
				if (executionPolicy.equals(ExecutionPolicy.RACE_ENABLING_MEMORY)){
					for (Integer disabled : disabledTransitions){
						Double censoredTime = ageVariables.remove(disabled);
						if (censoredTime != null && censoredTime > 0){ // ignore losing against immediate transitions
							// only add to censored times, if transition had some progress before losing against the current transition
							censoredTimes.get(disabled).add(censoredTime); 
						}
					}
				}
			}
		}
	}
		
	private void fireTransitionInMarking(short[] marking, int encTrans) {
		addMarkingTransitionCounter(marking, encTrans);
		// update marking
		short[] pred = encodedTrans2Pred.get(encTrans);
		if (pred != null) {
			// decrease the value
			for (int place = 0; place < pred.length; place++) {
				if (pred[place] != 0) {
					marking[place] -= pred[place];
				}
			}
		}
		short[] succ = encodedTrans2Succ.get(encTrans);
		if (succ != null) {
			// increase the value
			for (int place=0; place < succ.length; place++){
				marking[place] += succ[place];
			}
		}
	}
	
	/**
	 * Adds 1 to the firing counts of a transition in a marking.
	 * 
	 * @param marking the marking in which the transition fired
	 * @param encTrans the transition that fired
	 */
	private void addMarkingTransitionCounter(short[] marking, int encTrans) {
		// sanity check:
		for (short s : marking){
			if (s < 0){
				System.out.println("Debug me! Marking < 0!");
			}
		}
			
		String markingString = Arrays.toString(marking);
		if (!markingBasedSelections.containsKey(markingString)){
			markingBasedSelections.put(markingString, new int[idx2Trans.length]);
		}
		markingBasedSelections.get(markingString)[encTrans]++;
//		short[] markingInMap = null;
//		for (short[] m : markingBasedSelections.keySet()){
//			boolean equals = true;
//			for (int i= 0; i < m.length; i++){
//				equals = equals && marking[i] == m[i];
//			}
//			if(equals){
//				markingInMap = m;
//			}
//		}
//		if (markingInMap == null){
//			markingInMap = marking;
//		}
//		if (!markingBasedSelections.containsKey(markingInMap)){
//			markingBasedSelections.put(marking, new int[idx2Trans.length]);
//		}
//		markingBasedSelections.get(markingInMap)[encTrans]++;
	}

	private short[] constructEncInitMarking(Marking initMarking) {
		short[] res = new short[idx2Place.length];
		for (Place p : initMarking.baseSet()) {
			res[place2Idx.get(p)] = initMarking.occurrences(p).shortValue();
		}
		return res;
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
			for (int i = 0; i < pred.length; i++){
				transitionIsEnabled = transitionIsEnabled && marking[i] >= pred[i];
			}
			if (transitionIsEnabled){
				enabledTransitions.add(tId);
			}
		}
		return enabledTransitions;
	}

	public double getMeanTraceFitness(){
		return fitnessStatistic.getMean();
	}
	
	public Map<String, int[]> getMarkingBasedSelections() {
		return markingBasedSelections;
	}

	public int getEncOfTrans(Transition t) {
		return trans2Idx.get(t);
	}

	public int getDataCount(int encodedTransID){
		if (firingTimes.containsKey(encodedTransID)){
			return firingTimes.get(encodedTransID).size();
		}
		return 0;
	}

	public List<Double> getFiringTimes(int indexOfTransition) {
		return firingTimes.get(indexOfTransition);
	}

	public List<Double> getCensoredFiringTimes(int indexOfTransition) {
		return censoredTimes.get(indexOfTransition);
	}

	public double getMaxTraceDuration() {
		return longestTrace;
	}
}
