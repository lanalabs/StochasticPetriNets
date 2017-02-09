package org.processmining.models.semantics.petrinet.impl;

import org.processmining.framework.providedobjects.SubstitutionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SubstitutionType(substitutedType = StochasticNetSemantics.class)
public class StochasticNetSemanticsImpl extends AbstractResetInhibitorNetSemantics implements StochasticNetSemantics {
    private static final long serialVersionUID = 7834067963183391132L;

    public void initialize(Collection<Transition> transitions, Marking state) {
        super.initialize(transitions, state);
    }

    /**
     * Overrides default semantics, as only one of the transitions with highest priority can fire
     */
    public Collection<Transition> getExecutableTransitions() {
        long now = System.nanoTime();
        Collection<Transition> executableTransitions = getEnabledTransitions();
        Collection<Transition> transitions = getTransitionsOfHighestPriority(executableTransitions);
        long later = System.nanoTime();
//		System.out.println("getExecutableTransitions: "+(later-now)/1000000.+" ms.");
        return transitions;
    }

    /**
     * Gets all transitions, that are still enabled, even though some immediate transitions can fire first.
     *
     * @return all enabled transitions (these do not lose progress in the "enabling memory" policy,
     * even though they can not fire in a vanishing marking...)
     */
    public Collection<Transition> getEnabledTransitions() {
        return super.getExecutableTransitions();
    }

    private Collection<Transition> getTransitionsOfHighestPriority(Collection<Transition> executableTransitions) {
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
                // ignore priorities
                highestPriorityTransitions.add(t);
            }
        }
        return highestPriorityTransitions;
    }


}
