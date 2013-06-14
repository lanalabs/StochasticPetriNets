package org.processmining.models.semantics.petrinet.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.providedobjects.SubstitutionType;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;

/**
 * Hopefully more efficient implementation of the semantics...
 * 
 * @author Andreas Rogge-Solti
 *
 */
@SubstitutionType(substitutedType = StochasticNetSemantics.class)
public class EfficientStochasticNetSemanticsImpl implements StochasticNetSemantics{
	private static final long serialVersionUID = 7834067963183391132L;

	private int[][] transitionMatrix;
	private Transition[] transitions;
	private Place[] places;
	private Map<Place,Integer> placePositionInArray;
	private Map<Transition, Integer> transitionPositionInArray;
	private Map<Integer, List<Pair<Integer,Integer>>> transitionInputs;
	private Map<Integer, List<Pair<Integer,Integer>>> transitionOutputs;
	private int[] currentMarking;
	
	/**
	 * Stores for each place (encoded as position in {@link #places} array)
	 * all transitions that depend on the place, are stored in this map
	 */
	private Map<Integer, Set<Integer>> dependentTransitions;
	
	
	public void initialize(Collection<Transition> transitions, Marking state) {
		// fill transition matrix:
		this.transitions = transitions.toArray(new Transition[transitions.size()]);
		List<Place> places = new ArrayList<Place>();
		for (PetrinetNode node : this.transitions[0].getGraph().getNodes()){
			if(node instanceof Place){
				places.add((Place) node);
			}
		}
		this.dependentTransitions = new HashMap<Integer, Set<Integer>>();
		this.places = places.toArray(new Place[places.size()]);
		placePositionInArray = new HashMap<Place, Integer>();
		for (int i = 0; i < this.places.length; i++){
			placePositionInArray.put(this.places[i], i);
		}
		transitionPositionInArray = new HashMap<Transition, Integer>();
		
		transitionInputs = new HashMap<Integer, List<Pair<Integer,Integer>>>();
		transitionOutputs = new HashMap<Integer, List<Pair<Integer,Integer>>>();
		
		transitionMatrix = new int[this.transitions.length][];
		for (int i = 0; i < this.transitions.length; i++){
			transitionMatrix[i] = new int[this.places.length];
			Arrays.fill(transitionMatrix[i],0);
			
			List<Pair<Integer,Integer>> transitionInput = new ArrayList<Pair<Integer,Integer>>();
			List<Pair<Integer,Integer>> transitionOutput = new ArrayList<Pair<Integer,Integer>>();
			
			Transition t = this.transitions[i];
			transitionPositionInArray.put(t,i);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getInEdges(t)){
				PetrinetNode node = edge.getSource();
				if (node instanceof Place){
					int placePos = placePositionInArray.get(node);
					transitionMatrix[i][placePos]--;
				}
			}
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getOutEdges(t)){
				PetrinetNode node = edge.getTarget();
				if (node instanceof Place){
					int placePos = placePositionInArray.get(node);
					transitionMatrix[i][placePos]++;
				}
			}
			for (int place = 0; place < this.places.length; place++){
				if (!dependentTransitions.containsKey(place)){
					dependentTransitions.put(place, new HashSet<Integer>());
				}
				if (transitionMatrix[i][place]>0){
					transitionOutput.add(new Pair<Integer, Integer>(place,transitionMatrix[i][place]));
				}
				if (transitionMatrix[i][place]<0){
					dependentTransitions.get(place).add(i);
					transitionInput.add(new Pair<Integer, Integer>(place,-transitionMatrix[i][place]));
				}
			}
			transitionInputs.put(i, transitionInput);
			transitionOutputs.put(i, transitionOutput);
		}
		currentMarking = new int[this.places.length];
		setCurrentState(state);
	}

	/**
	 * Overrides default semantics, as only one of the transitions with highest priority can fire
	 */
	public Collection<Transition> getExecutableTransitions() {
		long now = System.nanoTime();
		Collection<Transition> executableTransitions = getEnabledTransitions();
		Collection<Transition> transitions = getTransitionsOfHighestPriority(executableTransitions);
		long later = System.nanoTime();
//		System.out.println("getExecutableTransitions (efficient): "+(later-now)/1000000.+" ms.");
		return transitions;
	}
	/**
	 * Gets all transitions, that are still enabled, even though some immediate transitions can fire first. 
	 * @return all enabled transitions (these do not lose progress in the "enabling memory" policy, 
	 * even though they can not fire in a vanishing marking...)
	 */
	public Collection<Transition> getEnabledTransitions() {
		List<Transition> enabledTransitions = getEnabledTransitionsByOnlyLookingAtPossibleCandidates();
		return enabledTransitions;
	}

	private List<Transition> getEnabledTransitionsByOnlyLookingAtPossibleCandidates(){
		List<Transition> enabledTransitions = new ArrayList<Transition>();
		
		Set<Integer> transitionCandidates = getTransitionCandidates();
		for (int trans : transitionCandidates){
			List<Pair<Integer,Integer>> inputs = transitionInputs.get(trans);
			boolean enabled = true;
			for (Pair<Integer,Integer> input : inputs){
				enabled &= currentMarking[input.getFirst()] >= input.getSecond();
				if (!enabled) break;
			}
			if (enabled){
				enabledTransitions.add(transitions[trans]);
			}
		}
		return enabledTransitions;
	}
	
	private List<Transition> getEnabledTransitionsBySearchingThroughHashMaps() {
		List<Transition> enabledTransitions = new ArrayList<Transition>();
		
		for (int trans = 0; trans < transitions.length; trans++){
			List<Pair<Integer,Integer>> inputs = transitionInputs.get(trans);
			boolean enabled = true;
			for (Pair<Integer,Integer> input : inputs){
				enabled &= currentMarking[input.getFirst()] >= input.getSecond();
				if (!enabled) break;
			}
			if (enabled){
				enabledTransitions.add(transitions[trans]);
			}
		}
		return enabledTransitions;
	}
	
	private Set<Integer> getTransitionCandidates() {
		Set<Integer> transitionCandidates = new HashSet<Integer>();
		for (int p = 0; p < currentMarking.length; p++){
			if (currentMarking[p] > 0){
				transitionCandidates.addAll(dependentTransitions.get(p));
			}
		}
		return transitionCandidates;
	}

	private List<Transition> getEnabledTransitionsBySearchingThroughMatrix() {
		List<Transition> enabledTransitions = new ArrayList<Transition>();
		for (int trans = 0; trans < transitions.length; trans++){
			boolean enabled = true;
			for (int place = 0; place < places.length; place++){
				enabled &= currentMarking[place]>= - transitionMatrix[trans][place];
				if (!enabled) break;
			}
			if (enabled){
				enabledTransitions.add(transitions[trans]);
			}
		}
		return enabledTransitions;
	}

	private Collection<Transition> getTransitionsOfHighestPriority(Collection<Transition> executableTransitions) {
		int priority = 0;

		List<Transition> highestPriorityTransitions = new ArrayList<Transition>();
		for (Transition t : executableTransitions){
			if (t instanceof TimedTransition){
				TimedTransition timedTransition = (TimedTransition)t;
				if (timedTransition.getPriority() == priority){
					highestPriorityTransitions.add(timedTransition);
				} else if (timedTransition.getPriority() > priority){
					priority = timedTransition.getPriority();
					highestPriorityTransitions.clear();
					highestPriorityTransitions.add(timedTransition);
				} else {
					// other transitions with higher priority disable the current transition!
				}
			}
		}
		return highestPriorityTransitions;
	}

	protected Collection<Transition> getTransitions() {
		return Arrays.asList(transitions);
	}

	public Marking getCurrentState() {
		Marking m = new Marking();
		for (int place = 0; place < currentMarking.length; place++){
			int tokens = currentMarking[place];
			for (int token = 0; token < tokens; token++){
				m.add(places[place]);	
			}
		}
		return m;
	}

	public void setCurrentState(Marking currentState) {
		Arrays.fill(currentMarking, 0);
		if (currentState == null){
			System.out.println("Debug me!");
		}
		for (Place p : currentState){
			currentMarking[placePositionInArray.get(p)]++;
		}
	}

	public PetrinetExecutionInformation executeExecutableTransition(Transition toExecute)
				throws IllegalTransitionException {
		int tId = transitionPositionInArray.get(toExecute);
		int[] markingBeforeFiring = currentMarking.clone();
		
		for (int place = 0; place < places.length; place++){
			currentMarking[place] += transitionMatrix[tId][place];
			if (currentMarking[place] < 0){
					currentMarking = markingBeforeFiring;
					throw new IllegalTransitionException(toExecute, getCurrentState());
			}
		}
		return null;
	}	
}
