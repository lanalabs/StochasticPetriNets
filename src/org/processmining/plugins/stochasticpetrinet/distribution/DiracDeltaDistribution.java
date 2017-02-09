package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;

import java.util.Arrays;

/**
 * This is the distribution for a random variable emitting a deterministic value.
 * It has all its probability mass on one single point.
 *
 * @author Andreas Rogge-Solti
 */
public class DiracDeltaDistribution extends AbstractRealDistribution {
    private static final long serialVersionUID = -955719767235470057L;

    private double value;


    public DiracDeltaDistribution(double value) {
        super(null); // nothing random about this distribution
        this.value = value;
    }

    public double density(double x) {
        if (x == value) {
            return Double.POSITIVE_INFINITY;
        }
        return 0;
    }

    public double cumulativeProbability(double x) {
        if (x < value) {
            return 0;
        } else {
            return 1;
        }
    }

    public double getNumericalMean() {
        return value;
    }

    public double getNumericalVariance() {
        return 0;
    }

    public double getSupportLowerBound() {
        return value;
    }

    public double getSupportUpperBound() {
        return value;
    }

    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    public boolean isSupportUpperBoundInclusive() {
        return true;
    }

    public boolean isSupportConnected() {
        return true;
    }

    public double sample() {
        return value;
    }

    public double[] sample(int sampleSize) {
        double[] values = new double[sampleSize];
        Arrays.fill(values, value);
        return values;
    }

    public double inverseCumulativeProbability(double p) throws OutOfRangeException {
        if (p < 0 || p > 1) {
            throw new OutOfRangeException(p, 0, 1);
        }
        return value;
    }

}
