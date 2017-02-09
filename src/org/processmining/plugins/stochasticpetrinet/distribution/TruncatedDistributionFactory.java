package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

public class TruncatedDistributionFactory {

    public static RealDistribution getConstrainedWrapper(RealDistribution dist) {
        return getConstrainedWrapper(dist, 0);
    }

    public static RealDistribution getConstrainedWrapper(RealDistribution dist, double constraint) {
        if (dist instanceof ExponentialDistribution) {
            if (constraint <= 0.0) {
                return dist;
            } else {
                return new MemorylessTruncatedWrapper(dist, constraint);
            }
        } else if (dist instanceof UniformRealDistribution) {
            UniformRealDistribution uniformDist = (UniformRealDistribution) dist;
            if (uniformDist.getSupportLowerBound() < constraint) {
                if (uniformDist.getSupportUpperBound() < constraint) {
                    return new DiracDeltaDistribution(constraint);
                } else {
                    RealDistribution constrainedDist = new UniformRealDistribution(constraint, uniformDist.getSupportUpperBound());
                    return constrainedDist;
                }
            } else {
                return dist;
            }
        } else if (dist.cumulativeProbability(constraint) < 0.95) {
            return new RejectionWrapper(dist, constraint);
        } else {
            try {
                return new TruncatedWrapper(dist, constraint);
            } catch (IllegalArgumentException e) {
                // fall back:
                return new DiracDeltaDistribution(constraint);
            }
        }
    }
}
