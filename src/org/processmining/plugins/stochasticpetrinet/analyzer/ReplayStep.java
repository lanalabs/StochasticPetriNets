package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;

import java.util.HashSet;
import java.util.Set;

/**
 * A Replay Step with the duration and
 * probabilistic information like density p(A=a | Model)
 * according to the probabilistic Model used in replay.
 *
 * @author Andreas Rogge-Solti
 */
public class ReplayStep {
    public TimedTransition transition;
    public Double duration;
    public Double density;

    public Set<ReplayStep> parents;
    public Set<ReplayStep> children;

    public ReplayStep(TimedTransition transition, double duration, double density, Set<ReplayStep> predecessorSteps) {
        this.transition = transition;
        this.duration = duration;
        this.density = density;
        this.parents = predecessorSteps;
        this.children = new HashSet<ReplayStep>();
    }

    public String toString() {
        return "{" + transition.getLabel() + ", dur: " + duration + ", dens: " + density + "}";
    }
}