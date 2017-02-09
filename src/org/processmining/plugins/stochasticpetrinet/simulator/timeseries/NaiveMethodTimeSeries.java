package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;

/**
 * The naive predictor for a time series simply returns the latest value as the predictor for the next one.
 *
 * @author andreas
 */
public class NaiveMethodTimeSeries extends TimeSeries<Double> {

    protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
        // nothing to do
    }

    protected Prediction<Double> getPrediction(int h, Object... payload) {
        Prediction<Double> prediction = new Prediction<>();
        prediction.prediction = findLastAvailableObservation().observation;
        if (Double.isNaN(prediction.prediction)) {
            System.err.println("Debug me!");
        }
        prediction.lower5Percentile = prediction.prediction;
        prediction.upper95Percentile = prediction.prediction;
        return prediction;
    }

    protected boolean isAvailable(Double observation) {
        return !Double.isNaN(observation);
    }
}
