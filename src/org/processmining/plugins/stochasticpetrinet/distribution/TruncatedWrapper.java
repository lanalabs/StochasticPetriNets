package org.processmining.plugins.stochasticpetrinet.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

public class TruncatedWrapper implements RealDistribution{

	/** The original distribution  */
	protected RealDistribution wrappedDist;
	/** the constraint, such that the distribution is truncated below this constraint */
	protected double constraint;
	
	/** sampler to sample from constrained distribution directly*/
	protected SliceSampler sampler;
	/** scaling function, such that the truncated distribution will integrate to 1 */
	protected double scale;
	
	protected double numericalMean = Double.NaN;
	
	TruncatedWrapper(RealDistribution dist){
		this(dist,0);
	}
	
	TruncatedWrapper(RealDistribution dist, double constraint){
		this.wrappedDist = dist;
		this.constraint = constraint;
		// rescale the density, such that it integrates to 1:
		this.scale = 1.0/(1.0-wrappedDist.cumulativeProbability(constraint));
		// 
		sampler = new SliceSampler();
	}
	
	public double probability(double x) {
		throw new UnsupportedOperationException("probability() not implemented");
	}

	public double density(double x) {
		if (x < constraint){
			return 0;
		} else {
			return scale * wrappedDist.density(x);
		}
	}

	public double cumulativeProbability(double x) {
		UnivariateIntegrator integrator = new SimpsonIntegrator();
		return integrator.integrate(10000, getFunction(), Double.NEGATIVE_INFINITY, x);
	}

	public double cumulativeProbability(double x0, double x1) throws NumberIsTooLargeException {
		UnivariateIntegrator integrator = new SimpsonIntegrator();
		return integrator.integrate(10000, getFunction(), x0, x1);
	}

	public UnivariateFunction getFunction() {
		UnivariateFunction function = new UnivariateFunction() {
			public double value(double x) {
				return density(x);
			}
		};
		return function;
	}
	
	public UnivariateFunction getWeightedFunction(){
		UnivariateFunction function = new UnivariateFunction() {
			public double value(double x) {
				if (x < constraint){
					return 0;
				} else {
					return x*(density(x));
				}
			}
		};
		return function;
	}

	public double inverseCumulativeProbability(double p) throws OutOfRangeException {
		throw new UnsupportedOperationException("inverseCumulativeProbability not implemented!");
	}

	public double getNumericalMean() {
		if (Double.isNaN(numericalMean)){
			UnivariateIntegrator integrator = new IterativeLegendreGaussIntegrator(16,0.01,0.000001);
			double upperBound = wrappedDist.inverseCumulativeProbability(.99)+constraint+10;
			numericalMean = integrator.integrate(integrator.getMaximalIterationCount(), getWeightedFunction(), constraint, upperBound);
		}
		return numericalMean;
	}

	public double getNumericalVariance() {
		throw new UnsupportedOperationException("numericalVariance not implemented!");
	}

	public double getSupportLowerBound() {
		return constraint;
	}

	public double getSupportUpperBound() {
		return wrappedDist.getSupportUpperBound();
	}

	public boolean isSupportLowerBoundInclusive() {
		return true;
	}

	public boolean isSupportUpperBoundInclusive() {
		return wrappedDist.isSupportUpperBoundInclusive();
	}

	public boolean isSupportConnected() {
		return true;
	}

	public void reseedRandomGenerator(long seed) {
		sampler.setSeed(seed);
	}

	/**
	 * Slice sampling
	 * Note that due to floating point arithmetic, too large constraints, i.e. those where the 
	 * density of the truncated distribution is 0, will not work! 
	 * @throws IllegalArgumentException when constraint is too high, i.e., density is (floating point rounded) zero.
	 */
	public double sample() {
		double xStart = findPositiveX(getFunction());
		if (wrappedDist.density(xStart) == 0){
			throw new IllegalArgumentException("did not find positive values for the wrapped distribution ("+wrappedDist.toString()+") constrained above "+constraint);
		}
		return sampler.sample(getFunction(), xStart, wrappedDist.density(xStart)*0.5);
	}

	private double findPositiveX(UnivariateFunction function) {
		double current = 1;
		while (function.value(current+constraint)==0){
			current *= -1.5;
		}
		return current+constraint;
	}

	public double[] sample(int sampleSize) {
		double xStart = findPositiveX(getFunction());
		if (wrappedDist.density(xStart) == 0){
			throw new IllegalArgumentException("did not find positive values for the wrapped distribution ("+wrappedDist.toString()+") constrained above "+constraint);
		}
		double[] values = sampler.sample(getFunction(), xStart, wrappedDist.density(xStart)*0.5,sampleSize);
		// shuffle and return:
		List<Double> valuesList = new ArrayList<Double>();
		for (double val : values){
			valuesList.add(val);
		}
		Collections.shuffle(valuesList);
		for (int i = 0; i < values.length; i++){
			values[i] = valuesList.get(i);
		}
		return values;
	}
}
