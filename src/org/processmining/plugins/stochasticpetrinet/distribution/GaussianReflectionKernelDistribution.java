package org.processmining.plugins.stochasticpetrinet.distribution;

import java.math.BigDecimal;

/**
 * Very plain boundary reflection kernel estimator.
 *
 * @author Andreas Rogge-Solti
 */
public class GaussianReflectionKernelDistribution extends GaussianKernelDistribution {
    private static final long serialVersionUID = -7153626225944172226L;

    /**
     * The lower threshold where reflection occurs
     */
    protected double threshold;

    /**
     * Boundary at x=0 for positive domain.
     */
    public GaussianReflectionKernelDistribution() {
        this(0);
    }

    public GaussianReflectionKernelDistribution(double threshold) {
        super();
        this.threshold = threshold;
    }

    public GaussianReflectionKernelDistribution(double threshold, double precision) {
        super(precision);
        this.threshold = threshold;
    }

    public double density(double x) {
        if (x < threshold) {
            return 0;
        }
        BigDecimal density = new BigDecimal(0);
        BigDecimal factor = new BigDecimal(1.0);
        factor = factor.divide(new BigDecimal(sampleValues.size()), veryPrecise);
        for (Long pos : kernelPointsAndWeights.keySet()) {
            double xKernelPos = pos * precision;
            density = density.add(factor.multiply(new BigDecimal(ndist.density(x - xKernelPos)).multiply(new BigDecimal(kernelPointsAndWeights.get(pos)), veryPrecise), veryPrecise), veryPrecise);
            density = density.add(factor.multiply(new BigDecimal(ndist.density((threshold - x) - xKernelPos)).multiply(new BigDecimal(kernelPointsAndWeights.get(pos)), veryPrecise), veryPrecise), veryPrecise);
//			density += (1/weightSum) * ndist.density(x-xKernelPos)*kernelPointsAndWeights.get(pos);
        }
        return density.doubleValue();
    }


    public double sample() {
        double sample = super.sample();
        return sample >= threshold ? sample : threshold - sample;
    }


}
