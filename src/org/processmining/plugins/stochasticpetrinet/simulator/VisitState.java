package org.processmining.plugins.stochasticpetrinet.simulator;

import org.deckfour.xes.model.XTrace;
import org.processmining.models.semantics.petrinet.Marking;

import java.math.BigDecimal;

/**
 * A State to visit during exploration
 * TODO: make more compact (e.g. use integer encoding)
 * Created by andreas on 6/7/17.
 */
public class VisitState {
    protected XTrace trace;
    protected Marking marking;
    protected Long time;
    protected BigDecimal probability;

    public VisitState(XTrace trace, Marking marking, Long time, BigDecimal probability) {
        this.trace = trace;
        this.marking = marking;
        this.time = time;
        this.probability = probability;
    }

    public XTrace getTrace() {
        return trace;
    }

    public void setTrace(XTrace trace) {
        this.trace = trace;
    }

    public Marking getMarking() {
        return marking;
    }

    public void setMarking(Marking marking) {
        this.marking = marking;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public BigDecimal getProbability() {
        return probability;
    }

    public void setProbability(BigDecimal probability) {
        this.probability = probability;
    }
}
