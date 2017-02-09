package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;

/**
 * A naive seasonal forecast (forecasts the same value as the one observed in the last season)
 *
 * @author Andreas Rogge-Solti
 */
public class SeasonalNaiveMethodTimeSeries extends TimeSeries<Double> {
    protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
        // nothing to do
    }

    protected Prediction<Double> getPrediction(int h, Object... payload) {
        Prediction<Double> prediction = new Prediction<>();
        long index = getLastObservation().timestamp + h;
        prediction.prediction = getObservationOfLastSeason(index).observation;
        prediction.lower5Percentile = prediction.prediction;
        prediction.upper95Percentile = prediction.prediction;
        return prediction;
    }

    protected boolean isAvailable(Double observation) {
        return !Double.isNaN(observation);
    }
}
