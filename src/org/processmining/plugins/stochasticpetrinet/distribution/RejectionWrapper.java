package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.RealDistribution;

/**
 * A wrapper around a distribution that cuts off a part below a certain threshold {@link #constraint}
 * and rescales the rest of the distribution to become a valid one again.
 * <p>
 * For sampling, a basic rejection sampling approach is implemented in {@link #sample()}.
 *
 * @author Andreas Rogge-Solti
 * @see TruncatedWrapper for a more advanced version using a slice sampling approach.
 */
public class RejectionWrapper extends AnotherAbstractRealDistribution {
    private static final long serialVersionUID = -5611826477784739098L;

    /**
     * The original distribution
     */
    protected RealDistribution wrappedDist;
    /**
     * the constraint, such that the distribution is truncated below this constraint
     */
    protected double constraint;
    /**
     * scaling function, such that the truncated distribution will integrate to 1
     */
    protected double scale = Double.NaN;

    public RejectionWrapper(RealDistribution dist) {
        this(dist, 0);
    }

    public RejectionWrapper(RealDistribution dist, double constraint) {
        this.wrappedDist = dist;
        this.constraint = constraint;
    }

    public double density(double x) {
        if (x < constraint) {
            return 0;
        } else {
            return getScale() * wrappedDist.density(x);
        }
    }

    private double getScale() {
        if (Double.isNaN(scale)) {
            // rescale the density, such that it integrates to 1:
            this.scale = 1.0 / (1.0 - wrappedDist.cumulativeProbability(constraint));
        }
        return scale;
    }

    public double getSupportLowerBound() {
        return constraint > wrappedDist.getSupportLowerBound() ? constraint : wrappedDist.getSupportLowerBound();
    }

    public double getSupportUpperBound() {
        return wrappedDist.getSupportUpperBound();
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return wrappedDist.isSupportUpperBoundInclusive();
    }

    public boolean isSupportConnected() {
        return wrappedDist.isSupportConnected();
    }

    public void reseedRandomGenerator(long seed) {
        wrappedDist.reseedRandomGenerator(seed);
    }

    /**
     * Perform simple rejection sampling until we find a sample that is above the constraint threshold.
     */
    public double sample() {
        boolean foundSample = false;
        double sample = Double.NaN;
        while (!foundSample) {
            sample = wrappedDist.sample();
            if (sample >= constraint) {
                foundSample = true;
            }
        }
        return sample;
    }

    public double[] sample(int sampleSize) {
        double[] samples = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            samples[i] = sample();
        }
        return samples;
    }

}
