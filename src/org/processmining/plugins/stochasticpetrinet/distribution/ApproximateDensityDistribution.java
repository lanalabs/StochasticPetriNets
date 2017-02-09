package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.Well1024a;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class ApproximateDensityDistribution extends AbstractRealDistribution implements UnivariateFunction {
    private static final long serialVersionUID = -4726958900733026212L;

    public static final int DEFAULT_SEGMENTS = 1024;
    public static final double DEFAULT_CUTOFF = 0.0001;

    private double min;
    private double max;
    private double[] densityValues;
    private double binwidth;

    private Double cachedMean = null;
    private double mode;
    private Double cachedVariance;

    private double[] cumulativeValues;

    private SliceSampler sampler;

    private double shift;

    private boolean isZero = false;

    public ApproximateDensityDistribution(RealDistribution dist, boolean sample) {
        this(dist, DEFAULT_SEGMENTS, DEFAULT_CUTOFF, sample);
    }

    /**
     * Approximates a distribution with a density piecewise line through some sample density points
     *
     * @param dist
     */
    public ApproximateDensityDistribution(RealDistribution dist, int points, double cutoffBelow, boolean sample) {
        super(new Well1024a());
        min = dist.getSupportLowerBound();
        if (min == Double.NEGATIVE_INFINITY) {
            min = dist.inverseCumulativeProbability(cutoffBelow / 2);
        }
        max = dist.getSupportUpperBound();
        if (max == Double.POSITIVE_INFINITY) {
            max = dist.inverseCumulativeProbability(1 - cutoffBelow / 2);
        }
        densityValues = new double[points];
        for (int i = 0; i < densityValues.length; i++) {
            densityValues[i] = dist.density(min + (i / (double) densityValues.length) * (max - min));
        }
        this.binwidth = (max - min) / (densityValues.length - 1);
        this.shift = 0;
        computeCumulativeValues(sample);
    }

    public ApproximateDensityDistribution(double[] values, double min, double max) {
        this(values, min, max, 0);
    }

    public ApproximateDensityDistribution(double[] values, double min, double max, double shift) {
        super(new Well1024a());
        this.densityValues = values;
        this.min = min;
        this.max = max;
        this.binwidth = (max - min) / (values.length - 1);
        this.shift = shift;
        computeCumulativeValues(true);
    }

    private void computeCumulativeValues(boolean sample) {
        cumulativeValues = new double[densityValues.length];
        double maxDens = 0;
        int modeIndex = 0;
        for (int i = 0; i < densityValues.length; i++) {
            if (densityValues[i] > maxDens) {
                maxDens = densityValues[i];
                modeIndex = i;
            }
            if (i == 0) {
                cumulativeValues[i] = 0;
            } else {
                cumulativeValues[i] = cumulativeValues[i - 1] + densityValues[i] * binwidth;
            }
        }
        mode = min + (modeIndex / (double) densityValues.length) * (max - min) + shift;
        // scale to 1:
        double factor = cumulativeValues[cumulativeValues.length - 1];
        if (factor > 0) {
            //		assert(Math.abs(1-factor) < 0.1);
            for (int i = 0; i < cumulativeValues.length; i++) {
                cumulativeValues[i] /= factor;
                densityValues[i] /= factor;
            }
            if (sample) {
                this.sampler = new SliceSampler((UnivariateFunction) this, mode, value(mode) / 2);
            }
        } else {
            // approximation is entirely 0!
            isZero = true;
        }
    }

    public double cumulativeProbability(double x) {
        if (isZero) return 0;

        x += shift;
        if (x <= min) {
            return 0;
        } else if (x >= max) {
            return 0;
        } else {
            double position = (x - min) / (max - min);
            int lowerIndex = (int) (position);
            double ratio = position - lowerIndex;
            int upperIndex = lowerIndex + 1;
            if (lowerIndex < 0 || upperIndex >= cumulativeValues.length) {
                return 0;
            } else {
                return (1 - ratio) * cumulativeValues[lowerIndex] + ratio * cumulativeValues[upperIndex];
            }
        }
    }

    public double density(double x) {
        if (isZero) return 0;

        x -= shift;
        if (x <= min) {
            return 0;
        } else if (x >= max) {
            return 0;
        } else {
            double position = ((x - min) * densityValues.length) / (max - min);
            int lowerIndex = (int) (position);
            double ratio = position - lowerIndex;
            int upperIndex = lowerIndex + 1;
            if (lowerIndex < 0 || upperIndex >= densityValues.length) {
                return 0;
            } else {
                return (1 - ratio) * densityValues[lowerIndex] + ratio * densityValues[upperIndex];
            }
        }
    }

    public double value(double x) {
        return density(x);
    }

    public double getNumericalMean() {
        if (isZero) return 0;

        if (cachedMean == null) {
            computeNumericalStats();
        }
        return cachedMean;
    }

    private void computeNumericalStats() {
        double xStart = mode;
        SliceSampler sampler = new SliceSampler((UnivariateFunction) this, xStart, density(xStart) / 2);
        double[] samples = sampler.sample(50000);
        DescriptiveStatistics stats = new DescriptiveStatistics(samples);
        cachedMean = stats.getMean();
        cachedVariance = stats.getVariance();
    }

    public double getNumericalVariance() {
        if (cachedVariance == null) {
            computeNumericalStats();
        }
        return cachedVariance;
    }

    public double getSupportLowerBound() {
        return min + shift;
    }

    public double getSupportUpperBound() {
        return max + shift;
    }

    public boolean isSupportConnected() {
        return true;
    }

    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    public boolean isSupportUpperBoundInclusive() {
        return true;
    }

    public double sample() {
        return sampler.sample();
    }

    public double[] sample(int sampleSize) {
        return DistributionUtils.shuffle(sampler.sample(sampleSize));
    }

}
