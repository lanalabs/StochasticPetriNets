package org.processmining.models.semantics.petrinet.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.processmining.framework.providedobjects.SubstitutionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;

@SubstitutionType(substitutedType = StochasticNetSemantics.class)
public class StochasticNetSemanticsImpl extends AbstractResetInhibitorNetSemantics implements StochasticNetSemantics{
	private static final long serialVersionUID = 7834067963183391132L;

	public void initialize(Collection<Transition> transitions, Marking state) {
		super.initialize(transitions, state);
	}

	/**
	 * Overrides default semantics, as only one of the transitions with highest priority can fire
	 */
	public Collection<Transition> getExecutableTransitions() {
		Collection<Transition> executableTransitions = super.getExecutableTransitions();
		return getTransitionsOfHighestPriority(executableTransitions);
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
	
	
}