package org.processmining.models.semantics.petrinet.impl;

import gnu.trove.map.hash.THashMap;
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

import java.util.*;

/**
 * Hopefully more efficient implementation of the semantics...
 *
 * @author Andreas Rogge-Solti
 */
@SubstitutionType(substitutedType = StochasticNetSemantics.class)
public class EfficientStochasticNetSemanticsImpl implements StochasticNetSemantics {
    private static final long serialVersionUID = 7834067963183391132L;

    protected short[][] transitionMatrix;
    protected Transition[] transitions;
    protected Place[] places;
    protected Map<Place, Short> placePositionInArray;
    protected Map<Transition, Short> transitionPositionInArray;
    protected Map<Short, List<Pair<Short, Short>>> transitionInputs;
    protected Map<Short, List<Pair<Short, Short>>> transitionOutputs;
    protected short[] currentMarking;

    /**
     * Stores for each place (encoded as position in {@link #places} array)
     * all transitions that depend on the place, are stored in this map
     */
    protected Map<Short, Set<Short>> dependentTransitions;


    public void initialize(Collection<Transition> transitions, Marking state) {
        // fill transition matrix:
        this.transitions = transitions.toArray(new Transition[transitions.size()]);
        List<Place> places = new ArrayList<Place>();
        for (PetrinetNode node : this.transitions[0].getGraph().getNodes()) {
            if (node instanceof Place) {
                places.add((Place) node);
            }
        }
        this.dependentTransitions = new THashMap<Short, Set<Short>>();
        this.places = places.toArray(new Place[places.size()]);
        placePositionInArray = new THashMap<Place, Short>();

        for (short i = 0; i < this.places.length; i++) {
            placePositionInArray.put(this.places[i], i);
        }
        transitionPositionInArray = new THashMap<Transition, Short>();

        transitionInputs = new THashMap<Short, List<Pair<Short, Short>>>();
        transitionOutputs = new THashMap<Short, List<Pair<Short, Short>>>();

        transitionMatrix = new short[this.transitions.length][];
        for (short i = 0; i < this.transitions.length; i++) {
            transitionMatrix[i] = new short[this.places.length];
            Arrays.fill(transitionMatrix[i], (short) 0);

            List<Pair<Short, Short>> transitionInput = new ArrayList<Pair<Short, Short>>();
            List<Pair<Short, Short>> transitionOutput = new ArrayList<Pair<Short, Short>>();

            Transition t = this.transitions[i];
            transitionPositionInArray.put(t, i);
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getInEdges(t)) {
                PetrinetNode node = edge.getSource();
                if (node instanceof Place) {
                    int placePos = placePositionInArray.get(node);
                    transitionMatrix[i][placePos]--;
                }
            }
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getOutEdges(t)) {
                PetrinetNode node = edge.getTarget();
                if (node instanceof Place) {
                    int placePos = placePositionInArray.get(node);
                    transitionMatrix[i][placePos]++;
                }
            }
            for (short place = 0; place < this.places.length; place++) {
                if (!dependentTransitions.containsKey(place)) {
                    dependentTransitions.put(place, new HashSet<Short>());
                }
                if (transitionMatrix[i][place] > 0) {
                    transitionOutput.add(new Pair<Short, Short>(place, transitionMatrix[i][place]));
                }
                if (transitionMatrix[i][place] < 0) {
                    dependentTransitions.get(place).add(i);
                    transitionInput.add(new Pair<Short, Short>(place, (short) -transitionMatrix[i][place]));
                }
            }
            transitionInputs.put(i, transitionInput);
            transitionOutputs.put(i, transitionOutput);
        }
        currentMarking = new short[this.places.length];
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
     *
     * @return all enabled transitions (these do not lose progress in the "enabling memory" policy,
     * even though they can not fire in a vanishing marking...)
     */
    public Collection<Transition> getEnabledTransitions() {
        List<Transition> enabledTransitions = getEnabledTransitionsByOnlyLookingAtPossibleCandidates();
        return enabledTransitions;
    }

    protected List<Transition> getEnabledTransitionsByOnlyLookingAtPossibleCandidates() {
        List<Transition> enabledTransitions = new ArrayList<Transition>();

        Set<Short> transitionCandidates = getTransitionCandidates();
        for (short trans : transitionCandidates) {
            List<Pair<Short, Short>> inputs = transitionInputs.get(trans);
            boolean enabled = true;
            for (Pair<Short, Short> input : inputs) {
                enabled &= currentMarking[input.getFirst()] >= input.getSecond();
                if (!enabled) break;
            }
            if (enabled) {
                enabledTransitions.add(transitions[trans]);
            }
        }
        return enabledTransitions;
    }

    protected List<Transition> getEnabledTransitionsBySearchingThroughHashMaps() {
        List<Transition> enabledTransitions = new ArrayList<Transition>();

        for (int trans = 0; trans < transitions.length; trans++) {
            List<Pair<Short, Short>> inputs = transitionInputs.get(trans);
            boolean enabled = true;
            for (Pair<Short, Short> input : inputs) {
                enabled &= currentMarking[input.getFirst()] >= input.getSecond();
                if (!enabled) break;
            }
            if (enabled) {
                enabledTransitions.add(transitions[trans]);
            }
        }
        return enabledTransitions;
    }

    protected Set<Short> getTransitionCandidates() {
        Set<Short> transitionCandidates = new HashSet<Short>();
        for (short p = 0; p < currentMarking.length; p++) {
            if (currentMarking[p] > 0) {
                transitionCandidates.addAll(dependentTransitions.get(p));
            }
        }
        return transitionCandidates;
    }

    protected List<Transition> getEnabledTransitionsBySearchingThroughMatrix() {
        List<Transition> enabledTransitions = new ArrayList<Transition>();
        for (int trans = 0; trans < transitions.length; trans++) {
            boolean enabled = true;
            for (int place = 0; place < places.length; place++) {
                enabled &= currentMarking[place] >= -transitionMatrix[trans][place];
                if (!enabled) break;
            }
            if (enabled) {
                enabledTransitions.add(transitions[trans]);
            }
        }
        return enabledTransitions;
    }

    protected Collection<Transition> getTransitionsOfHighestPriority(Collection<Transition> executableTransitions) {
        int priority = 0;

        List<Transition> highestPriorityTransitions = new ArrayList<Transition>();
        for (Transition t : executableTransitions) {
            if (t instanceof TimedTransition) {
                TimedTransition timedTransition = (TimedTransition) t;
                if (timedTransition.getPriority() == priority) {
                    highestPriorityTransitions.add(timedTransition);
                } else if (timedTransition.getPriority() > priority) {
                    priority = timedTransition.getPriority();
                    highestPriorityTransitions.clear();
                    highestPriorityTransitions.add(timedTransition);
                } else {
                    // other transitions with higher priority disable the current transition!
                }
            } else {
                highestPriorityTransitions.add(t);
            }
        }
        return highestPriorityTransitions;
    }

    protected Collection<Transition> getTransitions() {
        return Arrays.asList(transitions);
    }

    public Marking getCurrentState() {
        Marking m = new Marking();
        for (int place = 0; place < currentMarking.length; place++) {
            int tokens = currentMarking[place];
            for (int token = 0; token < tokens; token++) {
                m.add(places[place]);
            }
        }
        return m;
    }

    public short[] getCurrentInternalState() {
        return currentMarking;
    }

    public void setCurrentState(Marking currentState) {
        Arrays.fill(currentMarking, (short) 0);
        if (currentState == null) {
            System.out.println("Debug me!");
        }
        for (Place p : currentState) {
            currentMarking[placePositionInArray.get(p)]++;
        }
    }

    public void setCurrentState(short[] currentState) {
        currentMarking = currentState.clone();
    }

    public short getPlaceId(Place p) {
        if (placePositionInArray.containsKey(p)) {
            return placePositionInArray.get(p);
        }
        return -1;
    }

    public short getTransitionId(Transition t) {
        if (transitionPositionInArray.containsKey(t)) {
            return transitionPositionInArray.get(t);
        }
        return -1;
    }

    public Transition getTransition(short tId) {
        return transitions[tId];
    }


    public PetrinetExecutionInformation executeExecutableTransition(Transition toExecute)
            throws IllegalTransitionException {
        int tId = transitionPositionInArray.get(toExecute);
        short[] markingBeforeFiring = currentMarking.clone();

        for (int place = 0; place < places.length; place++) {
            currentMarking[place] += transitionMatrix[tId][place];
            if (currentMarking[place] < 0) {
                currentMarking = markingBeforeFiring;
                throw new IllegalTransitionException(toExecute, getCurrentState());
            }
        }
        return null;
    }

    public Object clone() {
        //TODO Make more efficient by just cloning all fields
        EfficientStochasticNetSemanticsImpl clone = new EfficientStochasticNetSemanticsImpl();
        clone.initialize(Arrays.asList(transitions), getCurrentState());

        return clone;
    }
}
