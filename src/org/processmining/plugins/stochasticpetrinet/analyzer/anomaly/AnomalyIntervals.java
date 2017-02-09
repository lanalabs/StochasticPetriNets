package org.processmining.plugins.stochasticpetrinet.analyzer.anomaly;

import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.List;
import java.util.Map;

/**
 * Simple container class to store temporal anomaly regions (intervals),
 * which correspond to duration outliers of transitions in a {@link StochasticNet}
 */
public class AnomalyIntervals {

    private Map<Transition, List<Pair<Double, Double>>> intervals;

    private double anomalyRate;

    private String netLabel;

    public AnomalyIntervals(Map<Transition, List<Pair<Double, Double>>> anomalyLists, String netLabel, double anomalyRate) {
        this.intervals = anomalyLists;
        this.anomalyRate = anomalyRate;
        this.netLabel = netLabel;
    }

    public Map<Transition, List<Pair<Double, Double>>> getIntervals() {
        return intervals;
    }

    public void setIntervals(Map<Transition, List<Pair<Double, Double>>> intervals) {
        this.intervals = intervals;
    }

    public double getAnomalyRate() {
        return anomalyRate;
    }

    public void setAnomalyRate(double anomalyRate) {
        this.anomalyRate = anomalyRate;
    }

    public String getNetLabel() {
        return this.netLabel;
    }

}
