package org.processmining.models.semantics.petrinet.impl;

import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetExecutionInformation;

import java.util.Collection;

/**
 * Stores a marking encoding temporal states in a discrete domain.
 * An array of sorted multisets stores the current marking/state of the net.
 *
 * @author Andreas Rogge-Solti
 */
public class EfficientDiscreteStochasticNetSemanticsImpl extends EfficientStochasticNetSemanticsImpl {
    private static final long serialVersionUID = 8895431349474689009L;

    protected EfficientTimedMarking currentTimedMarking;

    public void initialize(Collection<Transition> transitions, Marking state) {
        initialize(transitions, state, 0);
    }

    public void initialize(Collection<Transition> transitions, Marking state, int time) {
        super.initialize(transitions, state);
        currentTimedMarking = new EfficientTimedMarking(currentMarking, time);
    }

    public PetrinetExecutionInformation executeExecutableTransition(Transition toExecute) {
        throw new UnsupportedOperationException("Use this method only with a given transitionDuration!");
    }

    /**
     * @return the time of firing
     */
    public int executeExecutableTransition(Transition toExecute, int transitionDuration) throws IllegalTransitionException {
        super.executeExecutableTransition(toExecute);
        int tId = transitionPositionInArray.get(toExecute);
        return currentTimedMarking.executeTransitionWithDuration(transitionMatrix[tId], transitionDuration);
    }

    /**
     * @return the duration of the transition
     */
    public int executeExecutableTransitionAtTime(Transition toExecute, Integer timeOfFiring) throws IllegalTransitionException {
        super.executeExecutableTransition(toExecute);
        return currentTimedMarking.executeTransitionAtTime(transitionMatrix[transitionPositionInArray.get(toExecute)], timeOfFiring);
    }

    public int getDurationOfTransition(Transition toExecute, Short[] modelPlaceIds, int timeOfFiring) {
        return currentTimedMarking.getDurationOfFiring(transitionMatrix[transitionPositionInArray.get(toExecute)], modelPlaceIds, timeOfFiring);
    }

    public EfficientTimedMarking getInternalState() {
        return currentTimedMarking;
    }

    public void setCurrentState(Marking currentState) {
        super.setCurrentState(currentState);
    }

    public void setCurrentState(short[] currentState) {
        throw new UnsupportedOperationException("use different method to set the state, please.");
    }

    public void setInternalState(EfficientTimedMarking state) {
        short[] marking = new short[state.length()];
        for (int i = 0; i < marking.length; i++) {
            marking[i] = (short) state.getNumberOfTokens(i);
        }
        super.setCurrentState(marking);
        this.currentTimedMarking = state.clone();
    }

    public short[][] getTransitionMatrix() {
        return this.transitionMatrix;
    }


}
