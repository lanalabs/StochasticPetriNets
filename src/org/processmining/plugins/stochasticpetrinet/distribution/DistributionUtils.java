package org.processmining.plugins.stochasticpetrinet.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
	
	public static Map<Integer, Double> discretizeDistribution(RealDistribution dist, double binwidth){
		Map<Integer, Double> probabilitiesPerBin = new TreeMap<>();
		double lowerBound = Math.max(dist.getSupportLowerBound(), 0);
		double upperBound = Math.min(dist.getSupportUpperBound(), 1000);
		int lowerIndex = getIndex(lowerBound,binwidth);
		int upperIndex = getIndex(upperBound, binwidth);
		double lower = getValue(lowerIndex, binwidth);
		double lowerDensity = dist.density(lower);
		double binwidthThird = binwidth / 3;
		double densityMass = 0;
		for (int i = lowerIndex; i <= upperIndex;i++){
			double upper = lower+binwidth;
			double midL = lower+binwidthThird;
			double midU = midL+binwidthThird;
			
			double midLDensity = dist.density(midL);
			double midUDensity = dist.density(midU);
			double upperDensity = dist.density(upper);
			
			double density = (lowerDensity+midLDensity+midUDensity+upperDensity)/4.;
			if (density > 0){
				densityMass+=density;
				probabilitiesPerBin.put(i, density);
			}
			
			// approximate probability by averaging the densities of lower, middle and upper;
			lower = upper;
			lowerDensity = upperDensity;
		}
		// normalize the probabilities to 1:
		for (Entry<Integer, Double> entry : probabilitiesPerBin.entrySet()){
			entry.setValue(entry.getValue()/densityMass);
		}
		
		return probabilitiesPerBin;
	}
	
	/**
	 * The bin index starting with 0 for values between 0 inclusive and binwidth exclusive
	 * 
	 * @param value
	 * @param binwidth
	 * @return
	 */
	public static int getIndex(double value, double binwidth){
		return (int)Math.floor(value / binwidth);
	}
	
	public static double getValue(int index, double binwidth){
		return index*binwidth;
	}
}
