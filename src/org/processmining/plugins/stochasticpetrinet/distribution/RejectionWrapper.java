package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.OutOfRangeException;

public class RejectionWrapper implements RealDistribution {

	protected RealDistribution wrappedDist;
	protected double constraint;
	protected double scale;
	protected double numericalMean = Double.NaN;

	public RejectionWrapper(RealDistribution dist){
		this(dist,0);
	}
	public RejectionWrapper(RealDistribution dist, double constraint){
		this.wrappedDist = dist;
		this.constraint = constraint;
		// rescale the density, such that it integrates to 1:
		this.scale = 1.0/(1.0-wrappedDist.cumulativeProbability(constraint));
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
	@Deprecated
	public double cumulativeProbability(double x0, double x1) {
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
		return constraint > wrappedDist.getSupportLowerBound()?constraint:wrappedDist.getSupportLowerBound();
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

	public double sample() {
		boolean foundSample = false;
		double sample = Double.NaN;
		while (!foundSample){
			sample = wrappedDist.sample();
			if (sample >= constraint){
				foundSample = true;
			}
		}
		return sample;
	}

	public double[] sample(int sampleSize) {
		double[] samples = new double[sampleSize];
		for (int i=0; i < sampleSize; i++){
			samples[i] = sample();
		}
		return samples;
	}

}
