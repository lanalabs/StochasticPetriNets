package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class RAbstractDistribution extends AbstractRealDistribution {

    protected String method;

    public RAbstractDistribution(RandomGenerator randomGenerator) {
        super(randomGenerator);
        method = "";
    }

    public double density(double x) {
        return 0;
    }

    public double cumulativeProbability(double x) {
        return 0;
    }

    public double getNumericalMean() {
        return 0;
    }

    public double getNumericalVariance() {
        return 0;
    }

    public double getSupportLowerBound() {
        return 0;
    }

    public double getSupportUpperBound() {
        return 0;
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public boolean isSupportConnected() {
        return false;
    }

}
