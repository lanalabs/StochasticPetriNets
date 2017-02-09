package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;

/**
 * Constrains a histogram to only use the part that is consistent with the constraint.
 *
 * @author Andreas Rogge-Solti
 */
public class ConstrainedHistogramDistribution extends AbstractRealDistribution {
    private static final long serialVersionUID = 9104031087200222667L;

    private SimpleHistogramDistribution hist;

    public ConstrainedHistogramDistribution(SimpleHistogramDistribution dist, double constraint) {
        hist = new SimpleHistogramDistribution(dist.binwidth);
        for (Integer key : dist.binsAndValues.keySet()) {
            double value = dist.getValue(key);
            if (value >= constraint) {
                for (int i = 0; i < dist.binsAndValues.get(key); i++) {
                    hist.addValue(value);
                }
            }
        }
        // ensure there is at least one sample
        if (hist.sampleSize <= 0) {
            if (dist.getValue(dist.getIndex(constraint)) < constraint) {
                hist.addValue(constraint + dist.binwidth);
            } else {
                hist.addValue(constraint);
            }
        }
    }

    public double probability(double x) {
        throw new UnsupportedOperationException("probability not supported");
    }

    public double density(double x) {
        return hist.density(x);
    }

    public double cumulativeProbability(double x) {
        return hist.cumulativeProbability(x);
    }

    public double getNumericalMean() {
        return hist.getNumericalMean();
    }

    public double getNumericalVariance() {
        return hist.getNumericalVariance();
    }

    public double getSupportLowerBound() {
        return hist.getSupportLowerBound();
    }

    public double getSupportUpperBound() {
        return hist.getSupportUpperBound();
    }

    public boolean isSupportLowerBoundInclusive() {
        return hist.isSupportLowerBoundInclusive();
    }

    public boolean isSupportUpperBoundInclusive() {
        return hist.isSupportUpperBoundInclusive();
    }

    public boolean isSupportConnected() {
        return hist.isSupportConnected();
    }
}
