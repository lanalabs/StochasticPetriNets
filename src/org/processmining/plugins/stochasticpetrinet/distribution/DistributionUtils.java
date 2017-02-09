package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.util.FastMath;

import java.util.*;
import java.util.Map.Entry;

public class DistributionUtils {

    /**
     * Used for numerical integration to compute the mean.
     * given a distribution with a density, it returns a function of x * density(x)
     *
     * @param dist
     * @return
     */
    public static UnivariateFunction getWeightedFunction(final RealDistribution dist) {
        UnivariateFunction function = new UnivariateFunction() {
            public double value(double x) {
                return x * (dist.density(x));
            }
        };
        return function;
    }

    /**
     * Used for numerical integration to compute the variance.
     * given a distribution with a density, it returns a function of x * density(x)
     *
     * @param dist
     * @return
     */
    public static UnivariateFunction getWeightedSecondMomentFunction(final RealDistribution dist) {
        UnivariateFunction function = new UnivariateFunction() {
            public double value(double x) {
                return FastMath.pow(x, 2) * (dist.density(x));
            }
        };
        return function;
    }

    /**
     * Simply returns a wrapper function that returns the density.
     *
     * @param dist the distribution to extract the density from.
     * @return {@link UnivariateFunction}
     */
    public static UnivariateFunction getDensityFunction(final RealDistribution dist) {
        UnivariateFunction function = new UnivariateFunction() {
            public double value(double x) {
                return dist.density(x);
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
    public static double[] getRealPart(Complex[] complexValues) {
        double[] realPart = new double[complexValues.length];
        for (int i = 0; i < realPart.length; i++) {
            realPart[i] = complexValues[i].getReal();
        }
        return realPart;
    }

    public static double[] getVectorLength(Complex[] complexValues) {
        double[] lengths = new double[complexValues.length];
        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = FastMath.sqrt(FastMath.pow(complexValues[i].getReal(), 2) + FastMath.pow(complexValues[i].getImaginary(), 2));
        }
        return lengths;
    }

    public static double[] shuffle(double[] values) {
        // shuffle and return:
        List<Double> valuesList = new ArrayList<Double>();
        for (double val : values) {
            valuesList.add(val);
        }
        Collections.shuffle(valuesList);
        for (int i = 0; i < values.length; i++) {
            values[i] = valuesList.get(i);
        }
        return values;
    }

    public static Map<Integer, Double> discretizeDistribution(RealDistribution dist, double binwidth) {
        Map<Integer, Double> probabilitiesPerBin = new TreeMap<>();
        double lowerBound = Math.max(dist.getSupportLowerBound(), 0);
        if (Double.isInfinite(dist.getSupportLowerBound())) {
            lowerBound = dist.inverseCumulativeProbability(0.000001);
        }
        double upperBound = getReliableUpperBound(dist);
        int lowerIndex = getIndex(lowerBound, binwidth);
        int upperIndex = getIndex(upperBound, binwidth);
        double lower = getValue(lowerIndex, binwidth);
        double lowerDensity = dist.density(lower);
        double binwidthThird = binwidth / 3;
        double densityMass = 0;
//		System.out.println("discretizing distribution "+dist+" with binwidth "+binwidth+ " and "+(upperIndex-lowerIndex)+" bins from ("+lowerIndex+" to "+upperIndex+").");
        for (int i = lowerIndex; i <= upperIndex; i++) {
            double upper = lower + binwidth;
            double midL = lower + binwidthThird;
            double midU = midL + binwidthThird;

            double midLDensity = dist.density(midL);
            double midUDensity = dist.density(midU);
            double upperDensity = dist.density(upper);

            double density = (lowerDensity + midLDensity + midUDensity + upperDensity) / 4.;
            if (density > 0.0000001) {
                densityMass += density;
                probabilitiesPerBin.put(i, density);
            }

            // approximate probability by averaging the densities of lower, middle and upper;
            lower = upper;
            lowerDensity = upperDensity;
        }
        // normalize the probabilities to 1:
        for (Entry<Integer, Double> entry : probabilitiesPerBin.entrySet()) {
            entry.setValue(entry.getValue() / densityMass);
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
    public static int getIndex(double value, double binwidth) {
        return (int) Math.floor((value + (binwidth / 2)) / binwidth);
    }

    public static double getValue(int index, double binwidth) {
        return index * binwidth;
    }

    /**
     * Integrates a function numerically with the {@link SimpsonIntegrator}. Does so 10 times with less and less accuracy.
     *
     * @param f         the function to be integrated
     * @param fromValue the lower boundary of the integral
     * @param toValue   the upper boundary of the integral
     * @return double the value of the integral
     * @throws IllegalArgumentException if integration fails 10 times (with less and less accuracy)
     */
    public static double integrateReliably(UnivariateFunction f, double fromValue, double toValue) {
        long accuracyFactor = 1l;
        Double result = null;
        int tries = 0;
        while (result == null && tries++ < 10) {
            UnivariateIntegrator integrator = new SimpsonIntegrator(SimpsonIntegrator.DEFAULT_RELATIVE_ACCURACY * accuracyFactor, SimpsonIntegrator.DEFAULT_ABSOLUTE_ACCURACY * accuracyFactor, SimpsonIntegrator.DEFAULT_MIN_ITERATIONS_COUNT, SimpsonIntegrator.SIMPSON_MAX_ITERATIONS_COUNT);
            try {
                result = integrator.integrate(10000, f, fromValue, toValue);
            } catch (TooManyEvaluationsException e) {
                accuracyFactor *= 2;
            }
        }
        if (result == null) {
            throw new IllegalArgumentException("Could not compute the integral from " + fromValue + " to " + toValue + ". Resorting to sampling.");
        }
        return result;
    }

    public static double getReliableUpperBound(RealDistribution dist) {
        double upperBound = Math.min(dist.getSupportUpperBound(), 1000);
        if (Double.isInfinite(dist.getSupportUpperBound())) {
            upperBound = dist.inverseCumulativeProbability(0.999999);
        }
        return upperBound;
    }
}
