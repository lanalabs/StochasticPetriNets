package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.Well1024a;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.FastMath;

public abstract class AnotherAbstractRealDistribution extends AbstractRealDistribution implements UnivariateFunction {
    private static final long serialVersionUID = -600495498673503611L;

    private static final int SAMPLE_SIZE = 10000;

    protected Double cachedMean, cachedVariance;

    public AnotherAbstractRealDistribution() {
        super(new Well1024a());
    }

    public double cumulativeProbability(double x) {
//		if (Double.isInfinite(x)){
//			// search for some finite number, where the numerical value is already very close to 0.
//			double xScan = getSupportLowerBound()+10;
//			double val = density(xScan);
//			while (val > 1e-15){
//				xScan *= 2;
//				val = density(xScan);
//			}
//			x = xScan;
//		}
        return DistributionUtils.integrateReliably(this, getSupportLowerBound(), x);
    }

    /**
     * The expected value:
     */
    public double getNumericalMean() {
        if (cachedMean == null) {
            try {
                cachedMean = DistributionUtils.integrateReliably(DistributionUtils.getWeightedFunction(this), getSupportLowerBound(), getSupportUpperBound());
            } catch (IllegalArgumentException e) {
                // sample the mean:
                double[] samples = sample(SAMPLE_SIZE);
                DescriptiveStatistics stats = new DescriptiveStatistics(samples);
                cachedMean = stats.getMean();
            }
        }
        return cachedMean;
    }

    public double getNumericalVariance() {
        if (cachedVariance == null) {
            cachedVariance = DistributionUtils.integrateReliably(DistributionUtils.getWeightedSecondMomentFunction(this), getSupportLowerBound(), getSupportUpperBound()) - FastMath.pow(getNumericalMean(), 2);
        }
        return cachedVariance;
    }

    /**
     * The function value is the density.
     */
    public final double value(double x) {
        return density(x);
    }

}
