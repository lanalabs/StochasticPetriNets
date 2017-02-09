package org.processmining.plugins.stochasticpetrinet.distribution;


import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;

/**
 * Simple gaussian kernel estimator. Adds a gaussian kernel for each data point with specified
 * smoothing parameter {@link #kernelBandwidth}
 * <p>
 * A precision parameter controls the precision, such that for a precision
 * of 0.1, all sample values between -0.05 and 0.05 will be treated as 0.0.
 * <p>
 * <p>
 * The bandwidth of the kernel is adjusted to the data distribution and works best for normally distributed data
 * It used the formula proposed in:
 * <p>
 * Scott, D. W. (1992) Multivariate Density Estimation: Theory, Practice, and Visualization. Wiley
 *
 * @author Andreas Rogge-Solti
 */
public class GaussianKernelDistribution extends AnotherAbstractRealDistribution {
    private static final long serialVersionUID = 8240483694253488758L;

    /**
     * The precision parameter determines the interval size for kernels for improved efficiency.
     * We do not store n kernels for n observations, but group kernels falling into a particular interval
     * into one with the weight factor capturing the number of occurrences.
     * <p>
     * Change: Make this dynamic depending on the range of values (make it
     */
    protected double precision;

    /**
     * Grid over the data
     */
    public static final int NUMBER_OF_BINS = 1000;

    /**
     * This map stores the number of occurrences of values in defined intervals.
     * The interval size is regulated by the {@link #precision} argument
     */
    protected Map<Long, Double> kernelPointsAndWeights;

    /**
     * All observed values in an array (easier for sampling)
     */
    protected List<Double> sampleValues;

    protected static MathContext veryPrecise = new MathContext(50);

    /**
     * smoothing parameter
     */
    protected double h;
    protected NormalDistribution ndist;

    private Double cachedMean;
    private Double cachedVariance;


    public GaussianKernelDistribution() {
        this(0.1);
    }

    /**
     * Creates a kernel distribution grouping kernels with values falling into the range of the precision parameter into one "bin" with added weight
     * Precision 0.1 for example creates ten bins for one unit, precision 0.5 creates two bins.
     * Values in the interval [0.05,0.05[ fall into bin "0".
     *
     * @param precision the interval size to be captured by one bin. Instead of creating n kernels for n values,
     *                  we reduce the kernel count by grouping similar values and adjusting the weight of the shared kernel.
     */
    public GaussianKernelDistribution(double precision) {
        this.precision = precision;
        this.kernelPointsAndWeights = new TreeMap<Long, Double>();
        this.sampleValues = new ArrayList<Double>();
    }

    public void addValues(double[] values) {
        sampleValues = new ArrayList<Double>(values.length);
        for (double d : values) {
            sampleValues.add(d);
        }
        updateKernels();
    }

    public void addValue(double val) {
        sampleValues.add(val);
        updateKernels();
    }

    protected void updateKernels() {
        Collections.sort(sampleValues);

        precision = (getReasonableUpperBound() - getReasonableLowerBound()) / NUMBER_OF_BINS;

        kernelPointsAndWeights = new TreeMap<Long, Double>();
        for (double val : sampleValues) {
            Long position = Math.round(val / precision);
            if (kernelPointsAndWeights.containsKey(position)) {
                kernelPointsAndWeights.put(position, kernelPointsAndWeights.get(position) + 1);
            } else {
                kernelPointsAndWeights.put(position, 1.0);
            }
        }
        updateSmoothingParameter();
    }

    /**
     * Uses the 'rule of thumb' for the kernel bandwith combined with the more
     * robust quantile based approximation.
     */
    public void updateSmoothingParameter() {
        double quantile25to75 = Double.MAX_VALUE;
        if (sampleValues.size() > 4) {
            quantile25to75 = sampleValues.get(3 * sampleValues.size() / 4) - sampleValues.get(sampleValues.size() / 4);
        }
        if (quantile25to75 == 0) {
            quantile25to75 = 1;
        }
        double[] vals = getDoubleArray(this.sampleValues);
        DescriptiveStatistics stats = new DescriptiveStatistics(vals);
        double sd = stats.getStandardDeviation();
        if (sd == 0) {
            sd = 1;
        }
        h = 1.06 * Math.min(sd, quantile25to75 / 1.34) * Math.pow(sampleValues.size(), -1 / 5.);

        ndist = new NormalDistribution(0, h);
    }

    protected double[] getDoubleArray(List<Double> values) {
        double[] returnArray = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            returnArray[i] = values.get(i);
        }
        return returnArray;
    }

    public double cumulativeProbability(double x) {
        //double cProb = 0;
        BigDecimal cProb = new BigDecimal(0);
        if (h == 0) {
            System.out.println("Debug me!");
        }
        BigDecimal factor = new BigDecimal(1.0);
        factor = factor.divide(new BigDecimal(sampleValues.size()), veryPrecise);
        for (Long pos : kernelPointsAndWeights.keySet()) {
            double xKernelPos = pos * precision;
            cProb = cProb.add(factor.multiply(new BigDecimal(ndist.cumulativeProbability(x - xKernelPos)).multiply(new BigDecimal(kernelPointsAndWeights.get(pos))), veryPrecise), veryPrecise);
        }
        double retVal = cProb.doubleValue();
        if (retVal > 1) {
            System.out.println("This should never be the case!");
        }
        return retVal;
//		double cProb = 0;
//		NormalDistribution ndist = new NormalDistribution(0,h);
//		for (Long pos : kernelPointsAndWeights.keySet()){
//			double xKernelPos = pos*precision;
//			cProb += (1.0/sampleValues.length) * ndist.cumulativeProbability(x-xKernelPos)*kernelPointsAndWeights.get(pos);
//		}
//		if (cProb >= 1){
//			return 0.9999999999999;
//		}
//		return cProb;
    }

    public double cumulativeProbability(double arg0, double arg1) throws NumberIsTooLargeException {
        return cumulativeProbability(arg1) - cumulativeProbability(arg0);
    }

    public double density(double x) {

//		BigDecimal density = new BigDecimal(0);
//		BigDecimal factor = new BigDecimal(1.0);
//		factor = factor.divide(new BigDecimal(sampleValues.length), veryPrecise);
//		NormalDistribution ndist = new NormalDistribution(0,h);
//		for (Long pos : kernelPointsAndWeights.keySet()){
//			double xKernelPos = pos*precision;
//			density = density.add(factor.multiply(new BigDecimal(ndist.density(x-xKernelPos)).multiply(new BigDecimal(kernelPointsAndWeights.get(pos)),veryPrecise),veryPrecise),veryPrecise);
////			density += (1/weightSum) * ndist.density(x-xKernelPos)*kernelPointsAndWeights.get(pos);
//		}
//		return density.doubleValue();
        double density = 0;

        for (Long pos : kernelPointsAndWeights.keySet()) {
            double xKernelPos = pos * precision;
            density += ndist.density(x - xKernelPos) * kernelPointsAndWeights.get(pos);
        }
        // normalize again:
        density *= (1.0 / sampleValues.size());
        return density;
    }

    public double getNumericalMean() {
        if (cachedMean == null) {
            DescriptiveStatistics stats = new DescriptiveStatistics(getDoubleArray(sampleValues));
            cachedMean = stats.getMean();
        }
        return cachedMean;
    }

    public double getSupportLowerBound() {
//		return Double.NEGATIVE_INFINITY;
        return getReasonableLowerBound();
    }

    public double getSupportUpperBound() {
        return getReasonableUpperBound();
//		return Double.POSITIVE_INFINITY;
    }

//	/**
//	 * Binary search under the assumption that the probability distribution is monotonic
//	 */
//	public double inverseCumulativeProbability(double cProb) throws OutOfRangeException {
//		if (cProb < 0 || cProb > 1){
//			throw new OutOfRangeException(cProb,0,1);
//		} else if (cProb == 0) {
//			return Double.NEGATIVE_INFINITY;
//		} else if (cProb == 1){
//			return Double.POSITIVE_INFINITY;
//		} else {
//			if (sampleValues.length<2){
//				throw new IllegalArgumentException("Insufficient samples for nonparametric kernel estimation!");
//			}
//			double lowerBound = sampleValues[0]-1;
//			double upperBound = sampleValues[sampleValues.length-1]+1;
//			double lowerCumulative = cumulativeProbability(lowerBound);
//			double upperCumulative = cumulativeProbability(upperBound);
//			// expand lower bound if necessary:
//			double intervalLength = (upperBound-lowerBound);
//			while (cProb < lowerCumulative){
//				upperBound = lowerBound;
//				upperCumulative = lowerCumulative;
//				
//				lowerBound = lowerBound-intervalLength;
//				lowerCumulative = cumulativeProbability(lowerBound);
//			}
//			// expand upper bound to include searched value
//			while (cProb > upperCumulative){
//				lowerBound = upperBound;
//				lowerCumulative = upperCumulative;
//				
//				upperBound = upperBound + intervalLength;
//				upperCumulative = cumulativeProbability(upperBound);
//			}
//			// interval contains cProb: perform binary search:
//			double centerCumulative = cumulativeProbability((lowerBound+upperBound)/2.0);
//			while(Math.abs(centerCumulative-cProb) > SOLVER_DEFAULT_ABSOLUTE_ACCURACY){
//				if (cProb < centerCumulative){
//					upperBound = (lowerBound+upperBound)/2.0;
//					upperCumulative = centerCumulative;
//				} else {
//					lowerBound = (lowerBound+upperBound)/2.0;
//					lowerCumulative = centerCumulative;
//				}
//				centerCumulative = cumulativeProbability((lowerBound+upperBound)/2.0);
//			}
//			return (lowerBound+upperBound)/2;
//		}
//	}

    public boolean isSupportConnected() {
        return true;
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    /**
     * Should use density, as P(X=x) is zero for real-valued distributions
     */
    public double probability(double x) {
        return 0;
    }

    /**
     * Simply select one value from the observations at random and
     * sample from it's Gaussian Kernel heap.
     */
    @Override
    public double sample() {
        int nextPos = randomData.nextInt(0, sampleValues.size() - 1);
        Long pos = Math.round(sampleValues.get(nextPos) / precision);
        if (h == 0) {
            return pos * precision;
        } else {
            return ndist.sample() + pos * precision;
        }
    }

    public List<Double> getValues() {
        return this.sampleValues;
    }

    /**
     * The smoothing parameter for the density estimation that depends on the number of nodes and the inter-quartile range
     *
     * @return
     */
    public double getH() {
        return h;
    }

    public double getReasonableUpperBound() {
        return sampleValues.get(sampleValues.size() - 1).doubleValue() + 10 * h;
    }

    public double getReasonableLowerBound() {
        return sampleValues.get(0).doubleValue() - 10 * h;
    }
}
