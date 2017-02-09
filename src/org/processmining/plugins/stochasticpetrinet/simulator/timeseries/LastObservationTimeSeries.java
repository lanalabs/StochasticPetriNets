package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;

public class LastObservationTimeSeries extends TimeSeries<Double> {

    private Double lastObservation;

    protected boolean isAvailable(Double observation) {
        return !Double.isNaN(observation);
    }

    protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
    }

    protected Prediction<Double> getPrediction(int h, Object... payload) {
        return new Prediction<Double>(this.lastObservation, this.lastObservation, this.lastObservation);
    }

    /**
     * No aggregates used for this class, only predict the last single observation!
     *
     * @param lastObs
     */
    public void setLastObservation(Double lastObs) {
        this.lastObservation = lastObs;
    }

}
