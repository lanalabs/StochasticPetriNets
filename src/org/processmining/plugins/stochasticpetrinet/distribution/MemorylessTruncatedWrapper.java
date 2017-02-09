package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * When truncating an exponential function left (setting a lower constraint),
 * the result is the same exponential function offset by the constraint
 * <p>
 * exp(x,lambda=2) truncated with x>=3 is:
 * exp(x | x >= 3, lamda=2) = exp(x-3, lamda=2)
 */
public class MemorylessTruncatedWrapper extends TruncatedWrapper {

    public MemorylessTruncatedWrapper(RealDistribution dist) {
        super(dist);
    }

    public MemorylessTruncatedWrapper(RealDistribution dist, double constraint) {
        super(dist, 0);
        this.constraint = constraint;
        this.scale = 1;
    }

    public double density(double x) {
        return wrappedDist.density(x - constraint);
    }

    public double cumulativeProbability(double x) {
        return wrappedDist.cumulativeProbability(x - constraint);
    }

    public double cumulativeProbability(double x0, double x1) throws NumberIsTooLargeException {
        return wrappedDist.cumulativeProbability(x0 - constraint, x1 - constraint);
    }

    public double inverseCumulativeProbability(double p) throws OutOfRangeException {
        return wrappedDist.inverseCumulativeProbability(p) + constraint;
    }

    public double getNumericalMean() {
        return wrappedDist.getNumericalMean() + constraint;
    }

    public double getNumericalVariance() {
        return wrappedDist.getNumericalVariance();
    }

    public double sample() {
        return wrappedDist.sample() + constraint;
    }

    public double[] sample(int sampleSize) {
        double[] samples = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            samples[i] = sample();
        }
        return samples;
    }

}
