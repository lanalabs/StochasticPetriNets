package org.processmining.models.semantics.petrinet.impl;

import cern.colt.Arrays;
import org.processmining.models.semantics.IllegalTransitionException;

import java.util.*;

public class EfficientTimedMarking {

//	private final String timedMarkingKey;

    private InternalMarking timedMarking;

    private static HashMap<InternalMarking, InternalMarking> markingCache = new HashMap<>();

    private InternalMarking findCanonicalValue(InternalMarking value) {
        InternalMarking canon = markingCache.get(value);
        if (canon == null) {
            // not in the set, so put it there for the future
            markingCache.put(value, value);
            return value;
        } else {
            // in the set
            return canon;
        }
    }

    public static void clearCachedMarkings() {
        System.out.println("Clearing " + markingCache.size() + " cached markings.");
        markingCache.clear();
    }

    private int offset = 0;


    public EfficientTimedMarking(List<Integer>[] state) {
        this.timedMarking = new InternalMarking(state);
        normalize();
//		this.timedMarkingKey = NormalizedMarkingCache.getInstance().getKey(timedMarking);
    }

    @SuppressWarnings("unchecked")
    public EfficientTimedMarking(short[] currentMarking, int time) {
        this.offset = time;
        List<Integer>[] newMarking = new List[currentMarking.length];
        for (int i = 0; i < currentMarking.length; i++) {
            List<Integer> places = new LinkedList<Integer>();

            for (int j = 0; j < currentMarking[i]; j++) {
                places.add(0);
            }
            if (!places.isEmpty()) {
                newMarking[i] = places;
            }
        }
        this.timedMarking = new InternalMarking(newMarking);
//		this.timedMarkingKey = NormalizedMarkingCache.getInstance().getKey(timedMarking);
    }

    /**
     * Checks if the number of tokens specified in the marking parameter equals the internal timed marking
     * (ignoring the times on the tokens).
     *
     * @param marking short[] stores the number of tokens on each place
     * @return boolean
     */
    public boolean equalsMarking(short[] marking) {
        return timedMarking.equalsMarking(marking);
    }

    public int length() {
        return timedMarking.getMarking().length;
    }

    @SuppressWarnings("unchecked")
    private List<Integer>[] copyState(List<Integer>[] state) {
        List<Integer>[] copy = new LinkedList[state.length];
        for (int i = 0; i < state.length; i++) {
            if (state[i] != null) {
                copy[i] = new LinkedList<>(state[i]);
            }
        }
        return copy;
    }

    public EfficientTimedMarking clone() {
        InternalMarking clonedMarking = timedMarking.clone();
        EfficientTimedMarking clone = new EfficientTimedMarking(clonedMarking.getMarking());
        clone.offset = offset;
        return clone;
    }

    /**
     * Executes a transition with a given duration.
     *
     * @param transitionVector   vector capturing the inputs and outputs of a transition
     * @param transitionDuration
     * @return int the time of firing
     * @throws IllegalTransitionException
     */
    public int executeTransitionWithDuration(short[] transitionVector, int transitionDuration) throws IllegalTransitionException {
        int maxPlaceTime = removeTokensAndGetMaximumTime(transitionVector);
        int timeOfFiring = maxPlaceTime + transitionDuration;
        addTokensAtTime(transitionVector, timeOfFiring);
        normalize();
        return timeOfFiring;
    }

    /**
     * Executes a transition at a given time.
     *
     * @param transitionVector vector capturing the inputs and outputs of a transition
     * @param timeOfFiring     the time of firing of the transition
     * @return int the duration of the transition
     * @throws IllegalTransitionException
     */
    public int executeTransitionAtTime(short[] transitionVector, Integer timeOfFiring) throws IllegalTransitionException {
        int maxPlaceTime = removeTokensAndGetMaximumTime(transitionVector);
        addTokensAtTime(transitionVector, timeOfFiring);
        normalize();
        return timeOfFiring - maxPlaceTime;
    }

    private int removeTokensAndGetMaximumTime(short[] transitionVector) throws IllegalTransitionException {
        int maxPlaceTime = Integer.MIN_VALUE;
        for (int i = 0; i < transitionVector.length; i++) {
            if (transitionVector[i] < 0) {
                // remove the oldest tokens:
                for (int j = transitionVector[i]; j < 0; j++) {
                    if (timedMarking.getMarking()[i].isEmpty()) {
                        throw new IllegalTransitionException("transition " + Arrays.toString(transitionVector), "current state");
                    }
                    Integer firstEntry = timedMarking.getMarking()[i].remove(0) + offset;
                    if (timedMarking.getMarking()[i].isEmpty()) {
                        timedMarking.getMarking()[i] = null;
                    }
                    maxPlaceTime = Math.max(maxPlaceTime, firstEntry);
                }
            }
        }
        return maxPlaceTime;
    }

    private void addTokensAtTime(short[] transitionVector, int timeOfFiring) {
        for (int i = 0; i < transitionVector.length; i++) {
            if (transitionVector[i] > 0) {
                for (int j = 0; j < transitionVector[i]; j++) {
                    if (timedMarking.getMarking()[i] == null) {
                        timedMarking.getMarking()[i] = new LinkedList<>();
                    }
                    timedMarking.getMarking()[i].add(timeOfFiring - offset);
                }
                Collections.sort(timedMarking.getMarking()[i]);
            }
        }
    }


    public int getDurationOfFiring(short[] transitionVector, Short[] inputPlaces, int firingTime) {
        int maxPlaceTime = Integer.MIN_VALUE;
        for (int i = 0; i < inputPlaces.length; i++) {
            // look at the oldest tokens:
            Iterator<Integer> iter = timedMarking.getMarking()[inputPlaces[i]].iterator();
            int pos = 0;
            while (pos > transitionVector[inputPlaces[i]]) {
                maxPlaceTime = Math.max(maxPlaceTime, iter.next() + offset);
                pos--;
            }
        }
        return firingTime - maxPlaceTime;
    }

    private void normalize() {
        // searches for the minimum token time and shifts the offset so that the minimum token time is zero
        int minimumTokenTime = Integer.MAX_VALUE;
        for (int i = 0; i < length(); i++) {
            if (timedMarking.getMarking()[i] != null) {
                for (Integer tokenTime : timedMarking.getMarking()[i]) {
                    minimumTokenTime = Math.min(minimumTokenTime, tokenTime);
                }
            }
        }
        if (minimumTokenTime != 0) {
            this.offset += minimumTokenTime;
            for (int i = 0; i < length(); i++) {
                if (timedMarking.getMarking()[i] != null) {
                    for (int j = 0; j < timedMarking.getMarking()[i].size(); j++) {
                        timedMarking.getMarking()[i].set(j, timedMarking.getMarking()[i].get(j) - minimumTokenTime);
                    }
                }
            }
        }
    }

    public int getNumberOfTokens(int i) {
        return timedMarking.getMarking()[i] == null ? 0 : timedMarking.getMarking()[i].size();
    }

    public short[] reduceToStructure() {
        short[] structuralMarking = new short[length()];
        for (int i = 0; i < length(); i++) {
            structuralMarking[i] = (short) (timedMarking.getMarking()[i] == null ? 0 : timedMarking.getMarking()[i].size());
        }
        return structuralMarking;
    }

    public void pack() {
        timedMarking = findCanonicalValue(timedMarking);
//		assert(timedMarkingKey.equals(NormalizedMarkingCache.getInstance().getKey(timedMarking)));
//		timedMarking = null;
    }

    public void unpack() {
        timedMarking = timedMarking.clone();
//		if( timedMarking == null){
//			timedMarking = NormalizedMarkingCache.getInstance().getMarking(timedMarkingKey).clone();
//		}

    }

    public String toString() {
        return "marking off:" + offset + ", " + Arrays.toString(timedMarking.getMarking());
    }

    private class InternalMarking {
        private List<Integer>[] timedMarking;

        public List<Integer>[] getMarking() {
            return timedMarking;
        }

        public boolean equalsMarking(short[] marking) {
            for (int i = 0; i < marking.length; i++) {
                if (marking[i] != (timedMarking[i] == null ? 0 : timedMarking[i].size())) {
                    return false;
                }
            }
            return true;
        }

        public InternalMarking(List<Integer>[] marking) {
            this.timedMarking = marking;
        }

        public InternalMarking clone() {
            List<Integer>[] clonedMarking = new LinkedList[timedMarking.length];
            for (int i = 0; i < timedMarking.length; i++) {
                if (timedMarking[i] != null) {
                    clonedMarking[i] = new LinkedList<>(timedMarking[i]);
                }
            }
            return new InternalMarking(clonedMarking);
        }

        public int hashCode() {
            int hashCode = 1;
            for (int i = 0; i < timedMarking.length; i++) {
                if (timedMarking[i] != null) {
                    Iterator<Integer> iter = timedMarking[i].iterator();
                    while (iter.hasNext()) {
                        hashCode += iter.next();
                    }
                    hashCode *= 2;
                } else {
                    hashCode *= 2;
                }
            }
            return hashCode;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof InternalMarking)) {
                return false;
            }
            InternalMarking otherMarking = (InternalMarking) obj;
            // check deeply:
            if (timedMarking.length != (otherMarking.timedMarking == null ? 0 : otherMarking.timedMarking.length)) {
                return false;
            }
            for (int i = 0; i < timedMarking.length; i++) {
                if (timedMarking[i] == null && otherMarking.timedMarking[i] == null) {
                    continue;
                }
                if (timedMarking[i] == null || otherMarking.timedMarking[i] == null) {
                    return false;
                }
                if (timedMarking[i].size() != otherMarking.timedMarking[i].size()) {
                    return false;
                }
                Iterator<Integer> it1 = timedMarking[i].iterator();
                Iterator<Integer> it2 = otherMarking.timedMarking[i].iterator();
                while (it1.hasNext()) {
                    int i1 = it1.next();
                    int i2 = it2.next();
                    if (i1 != i2) {
                        return false;
                    }
                }
            }
            return true;
        }

        public String toString() {
            return Arrays.toString(timedMarking);
        }
    }
}
