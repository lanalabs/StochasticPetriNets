package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

public class Prediction<T> {

    public Prediction() {
    }

    public Prediction(T pred, T lower, T upper) {
        this.prediction = pred;
        this.lower5Percentile = lower;
        this.upper95Percentile = upper;
    }

    public T prediction;

    public T lower5Percentile;
    public T upper95Percentile;

}
