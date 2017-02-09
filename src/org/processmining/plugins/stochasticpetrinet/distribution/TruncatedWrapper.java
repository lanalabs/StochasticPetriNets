package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;

/**
 * A wrapper around a distribution that cuts off a part below a certain threshold {@link #constraint}
 * and rescales the rest of the distribution to become a valid one again.
 * <p>
 * This class should be used if samples from the original distribution are unlikely to fall into the region above the {@link #constraint}.
 * The sampling of random values from this distribution is done by the <a href="http://en.wikipedia.org/wiki/Slice_sampling">slice sampling technique</a>.
 *
 * @author Andreas Rogge-Solti
 * @see RejectionWrapper for the simpler version building on rejection sampling.
 */
public class TruncatedWrapper extends RejectionWrapper {
    private static final long serialVersionUID = -6541835452767655288L;

    /**
     * sampler to sample from constrained distribution directly
     */
    protected SliceSampler sampler;

    TruncatedWrapper(RealDistribution dist) {
        this(dist, 0);
    }

    TruncatedWrapper(RealDistribution dist, double constraint) {
        super(dist, constraint);

        // init slice sampler:
        double xStart = findPositiveX(this);
        if (wrappedDist.density(xStart) == 0) {
            throw new IllegalArgumentException("did not find positive values for the wrapped distribution (" + wrappedDist.toString() + ") constrained above " + constraint);
        }
        sampler = new SliceSampler((UnivariateFunction) this, xStart, wrappedDist.density(xStart) * 0.5);
    }

    public void reseedRandomGenerator(long seed) {
        sampler.setSeed(seed);
    }

    /**
     * Slice sampling
     * Note that due to floating point arithmetic, too large constraints, i.e. those where the
     * density of the truncated distribution is 0, will not work!
     *
     * @throws IllegalArgumentException when constraint is too high, i.e., density is (floating point rounded) zero.
     */
    public double sample() {
        return sampler.sample();
    }

    private double findPositiveX(UnivariateFunction function) {
        double current = 0.05;
        while (function.value(current + constraint) == 0 && !Double.isInfinite(current)) {
            // first search between 0-1
            if (Math.abs(current) < 1) {
                current += 0.1;
            } else {
                current *= 1.1;
            }
        }
        if (Double.isInfinite(current)) {
            throw new IllegalArgumentException("Could not locate a positive value of the function " + function);
        }
        return current + constraint;
    }

    public double[] sample(int sampleSize) {
        double xStart = findPositiveX(this);
        if (wrappedDist.density(xStart) == 0) {
            throw new IllegalArgumentException("did not find positive values for the wrapped distribution (" + wrappedDist.toString() + ") constrained above " + constraint);
        }
        double[] values = sampler.sample(sampleSize);

        return DistributionUtils.shuffle(values);
    }
}
