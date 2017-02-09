package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.utils.datastructures.LimitedQueue;

/**
 * The simplest form of model to fit to a data set is the average.
 *
 * @author Andreas Rogge-Solti
 */
public class AverageMethodTimeSeries extends TimeSeries<Double> {

    protected DescriptiveStatistics stats;

    protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
        stats = new DescriptiveStatistics();
        for (Observation<Double> obs : currentObservations) {
            if (!Double.isNaN(obs.observation)) {
                stats.addValue(obs.observation);
            }
        }
    }

    protected Prediction<Double> getPrediction(int h, Object... payload) {
        Prediction<Double> prediction = new Prediction<>();
        prediction.prediction = stats.getMean();
        prediction.lower5Percentile = stats.getPercentile(5);
        prediction.upper95Percentile = stats.getPercentile(95);
        return prediction;
    }

    protected boolean isAvailable(Double observation) {
        return !Double.isNaN(observation);
    }
}
