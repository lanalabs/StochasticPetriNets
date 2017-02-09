package org.processmining.plugins.stochasticpetrinet.distribution.numeric;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.plugins.stochasticpetrinet.analyzer.ReplayStep;
import org.processmining.plugins.stochasticpetrinet.distribution.ApproximateDensityDistribution;

public class FastDensityFunction implements UnivariateFunction {
    private RealDistribution distribution;

    /**
     * One child. Perform Fourier transform for convolution of the two random variables.
     *
     * @param step
     * @param child
     */
    public FastDensityFunction(ReplayStep step, ReplayStep child) {
        this.distribution = ConvolutionHelper.getConvolvedDistribution(step.transition.getDistribution(), child.transition.getDistribution());
    }

    /**
     * No children. Just return the distribution.
     *
     * @param step
     */
    public FastDensityFunction(ReplayStep step) {
        this.distribution = new ApproximateDensityDistribution(step.transition.getDistribution(), true);
    }

    public double value(double x) {
        return this.distribution.density(x);
    }
}