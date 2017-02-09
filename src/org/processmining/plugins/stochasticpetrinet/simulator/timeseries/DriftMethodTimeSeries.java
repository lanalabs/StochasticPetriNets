package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;

/**
 * The drift method extrapolates a line through the first observation and the last observation
 * for computing the next forecast.
 *
 * @author Andreas Rogge-Solti
 * @see {@link https://www.otexts.org/fpp/2/3}
 */
public class DriftMethodTimeSeries extends TimeSeries<Double> {

    int T;

    double firstObservation;
    double lastObservation;

    protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
        T = currentObservations.size();
        firstObservation = findFirstNonNanObservations(currentObservations);
        lastObservation = findLastAvailableObservation().observation;
    }

    private double findFirstNonNanObservations(LimitedQueue<Observation<Double>> currentObservations) {
        for (int i = 0; i < currentObservations.size(); i++) {
            Observation<Double> obs = currentObservations.get(i);
            if (!Double.isNaN(obs.observation)) {
                T -= i;
                return obs.observation;
            }
        }
        // only NaNs in the observations!!
        return 0.0;
    }

    protected Prediction<Double> getPrediction(int h, Object... payload) {
        double pred;
        if (T > 1) {
            pred = Math.max(0, lastObservation + h * ((lastObservation - firstObservation) / (T - 1))); // ensure that we do not predict values below zero!
        } else {
            pred = lastObservation;
        }
        Prediction<Double> prediction = new Prediction<>();
        prediction.prediction = pred;
        prediction.lower5Percentile = pred;
        prediction.upper95Percentile = pred;
        return prediction;
    }

    protected boolean isAvailable(Double observation) {
        return !Double.isNaN(observation);
    }
}
