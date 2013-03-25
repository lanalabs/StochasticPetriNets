package org.processmining.plugins.stochasticpetrinet.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.OutOfRangeException;

public class ConstrainedWrapper implements RealDistribution{

	private RealDistribution wrappedDist;
	private double constraint;
		
	private SliceSampler sampler;
	private double scale;
	
	public ConstrainedWrapper(RealDistribution dist){
		this(dist,0);
	}
	
	public ConstrainedWrapper(RealDistribution dist, double constraint){
		this.wrappedDist = dist;
		this.constraint = constraint;
		this.scale = 1.0/(1.0-wrappedDist.cumulativeProbability(constraint));
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

	public double inverseCumulativeProbability(double p) throws OutOfRangeException {
		throw new UnsupportedOperationException("inverseCumulativeProbability not implemented!");
	}

	public double getNumericalMean() {
		throw new UnsupportedOperationException("numericalMean not implemented!");
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

	public double sample() {
		double xStart = constraint;
		if (wrappedDist.density(constraint) == 0){
			try{
				xStart = wrappedDist.getNumericalMean();
				if(Double.isNaN(xStart)){
					xStart = findPositiveX(getFunction());
				}
			} catch (UnsupportedOperationException ex){
				xStart = findPositiveX(getFunction());
			}
		}
		return sampler.sample(getFunction(), xStart, wrappedDist.density(xStart)*0.5);
	}

	private double findPositiveX(UnivariateFunction function) {
		double current = 1;
		while (function.value(current)==0){
			current *= -1.5;
		}
		return current;
	}

	public double[] sample(int sampleSize) {
		double[] values = sampler.sample(getFunction(), constraint, wrappedDist.density(constraint)*0.5,sampleSize);
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
