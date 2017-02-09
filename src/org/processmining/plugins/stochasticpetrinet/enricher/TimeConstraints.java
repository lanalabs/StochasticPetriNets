package org.processmining.plugins.stochasticpetrinet.enricher;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.Map;

public class TimeConstraints {

    public enum TimeAssumption {
        NO_WAIT_TIME, NO_SERVICE_TIME, FIRST_WAIT_TIME_IS_ZERO, UNOBSERVED_ARE_IMMEDIATE;
    }

    private TimeAssumption assumption;

    private Map<Transition, DistributionType> transitionContraints;

    public TimeAssumption getAssumption() {
        return assumption;
    }

    public void setAssumption(TimeAssumption assumption) {
        this.assumption = assumption;
    }

    public Map<Transition, DistributionType> getTransitionContraints() {
        return transitionContraints;
    }

    public void setTransitionContraints(Map<Transition, DistributionType> transitionContraints) {
        this.transitionContraints = transitionContraints;
    }

}
