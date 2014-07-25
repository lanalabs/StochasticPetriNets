package org.processmining.models.semantics.petrinet.impl;

import java.util.Iterator;

import org.processmining.models.semantics.IllegalTransitionException;

import cern.colt.Arrays;

import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;

public class EfficientTimedMarking {

	private TreeMultiset<Integer>[] timedMarking;
	
	public EfficientTimedMarking(TreeMultiset<Integer>[] state){
		this.timedMarking = state;
	}
	
	@SuppressWarnings("unchecked")
	public EfficientTimedMarking(short[] currentMarking, int time) {
		this.timedMarking = new TreeMultiset[currentMarking.length];
		for (int i = 0; i < currentMarking.length; i++){
			timedMarking[i] = TreeMultiset.create();
			timedMarking[i].add(time, currentMarking[i]);
		}
	}

	/**
	 * Checks if the number of tokens specified in the marking parameter equals the internal timed marking
	 * (ignoring the times on the tokens).
	 * @param marking short[] stores the number of tokens on each place
	 * @return boolean
	 */
	public boolean equalsMarking(short[] marking){
		for (int i = 0; i < marking.length; i++){
			if (marking[i] != timedMarking[i].size()){
				return false;
			}
		}
		return true;
	}
	
	public int length() {
		return timedMarking.length;
	}
	
	@SuppressWarnings("unchecked")
	public static TreeMultiset<Integer>[] copyState(TreeMultiset<Integer>[] state) {
		TreeMultiset<Integer>[] copy = new TreeMultiset[state.length];
		for (int i = 0; i < state.length; i++){
			copy[i] = TreeMultiset.create(state[i]);
		}
		return copy;
	}
	
	public EfficientTimedMarking clone(){
		return new EfficientTimedMarking(copyState(timedMarking));
	}

	/**
	 * Executes a transition with a given duration.
	 * 
	 * @param transitionVector vector capturing the inputs and outputs of a transition 
	 * @param transitionDuration
	 * @return int the time of firing
	 * @throws IllegalTransitionException
	 */
	public int executeTransitionWithDuration(short[] transitionVector, int transitionDuration) throws IllegalTransitionException {
		int maxPlaceTime = removeTokensAndGetMaximumTime(transitionVector);
		int timeOfFiring = maxPlaceTime + transitionDuration;
		addTokensAtTime(transitionVector, timeOfFiring);
		return timeOfFiring;
	}
	/**
	 * Executes a transition at a given time.
	 * 
	 * @param transitionVector vector capturing the inputs and outputs of a transition
	 * @param timeOfFiring the time of firing of the transition 
	 * @return int the duration of the transition
	 *  
	 * @throws IllegalTransitionException
	 */
	public int executeTransitionAtTime(short[] transitionVector, Integer timeOfFiring) throws IllegalTransitionException {
		int maxPlaceTime = removeTokensAndGetMaximumTime(transitionVector);
		addTokensAtTime(transitionVector, timeOfFiring);
		return timeOfFiring - maxPlaceTime;
	}

	private int removeTokensAndGetMaximumTime(short[] transitionVector) throws IllegalTransitionException {
		int maxPlaceTime = Integer.MIN_VALUE;
		for (int i = 0; i < transitionVector.length; i++){
			if (transitionVector[i]  < 0){
				// remove the oldest tokens:
				for (int j = transitionVector[i]; j < 0; j++){
					Entry<Integer> firstEntry = timedMarking[i].pollFirstEntry();
					if (firstEntry == null){
						throw new IllegalTransitionException("transition "+Arrays.toString(transitionVector), "current state");
					}
					maxPlaceTime = Math.max(maxPlaceTime, firstEntry.getElement());	
				}
			}
		}
		return maxPlaceTime;
	}
	private void addTokensAtTime(short[] transitionVector, int timeOfFiring) {
		for (int i = 0; i < transitionVector.length; i++){
			if (transitionVector[i]  > 0){
				timedMarking[i].add(timeOfFiring, transitionVector[i]);		
			}
		}
	}
	
	
	
	public int getDurationOfFiring(short[] transitionVector, Short[] inputPlaces, int firingTime){
		int maxPlaceTime = Integer.MIN_VALUE;
		for (int i = 0; i < inputPlaces.length; i++){
			// remove the oldest tokens:
			Iterator<Integer> iter = timedMarking[inputPlaces[i]].iterator();
			int pos = 0;
			while (pos > transitionVector[inputPlaces[i]]){
				maxPlaceTime = Math.max(maxPlaceTime, iter.next());
				pos--;
			}
		}
		return firingTime - maxPlaceTime;
	}

	public int getNumberOfTokens(int i) {
		return timedMarking[i].size();
	}

	public short[] reduceToStructure() {
		short[] structuralMarking = new short[timedMarking.length];
		for (int i = 0; i < timedMarking.length; i++){
			structuralMarking[i] = (short) timedMarking[i].size();
		}
		return structuralMarking;
	}
}
