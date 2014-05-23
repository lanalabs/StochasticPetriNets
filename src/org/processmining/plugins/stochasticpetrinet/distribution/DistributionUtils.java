package org.processmining.plugins.stochasticpetrinet.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.FastMath;

public class DistributionUtils {

	/**
	 * Used for numerical integration.
	 * given a distribution with a density, it returns a function of x * density(x)
	 * @param dist
	 * @return
	 */
	public static UnivariateFunction getWeightedFunction(final RealDistribution dist){
		UnivariateFunction function = new UnivariateFunction() {
			public double value(double x) {
				return x*(dist.density(x));
			}
		};
		return function;
	}
	
	/**
	 * Useful after operations in Fourier space.
	 * 
	 * @param complexValues
	 * @return extracts the real part of the complex numbers
	 */
	public static double[] getRealPart(Complex[] complexValues){
		double[] realPart = new double[complexValues.length];
		for (int i = 0; i < realPart.length; i++){
			realPart[i] = complexValues[i].getReal();
		}
		return realPart;
	}
	
	public static double[] getVectorLength(Complex[] complexValues){
		double[] lengths = new double[complexValues.length];
		for (int i = 0; i < lengths.length; i++){
			lengths[i] = FastMath.sqrt(FastMath.pow(complexValues[i].getReal(),2) + FastMath.pow(complexValues[i].getImaginary(), 2));
		}
		return lengths;
	}
	
	public static double[] shuffle(double[] values) {
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
