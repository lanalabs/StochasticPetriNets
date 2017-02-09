package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the expolynomial distribution class presented in:
 * <p>
 * Horváth, András, Lorenzo Ridi, and Enrico Vicario.
 * "Approximating distributions and transient probabilities of Markov chains by
 * Bernstein expolynomial functions." Proc. of International Conference on the Numerical
 * Solution of Markov Chains. Williamsburg, VA, USA, September 15-18, Williamsburg, VA, USA: College of William & Mary. 2010.
 *
 * @author Andreas Rogge-Solti
 */
public class BernsteinExponentialApproximation extends AnotherAbstractRealDistribution {
    private static final long serialVersionUID = -8850638237161586884L;

    public static final int DEFAULT_N = 20;

    public static final double DEFAULT_C = 0.1;

    /**
     * The points to approximate the original density distribution.
     * nPoints.length = n+1
     */
    private double[] nPoints;

    private Double lowerBound, upperBound;

    private double c;


    /**
     * Constructs a Bernstein Exponential with a given number of sampling points
     *
     * @param nPoints sampling points of the distribution
     */
    public BernsteinExponentialApproximation(double[] nPoints, Double lowerBound, Double upperBound, double c) {
        this.nPoints = nPoints.clone();
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.c = c;
    }

    /**
     * @param f          the original function to approximate (usually a {@link RealDistribution}
     * @param lowerBound the lower bound "a" parameter in the cited work (must be greater or equal 0)
     * @param upperBound the upper bound "b" parameter in the cited work (can be infinite, must be greater than the lower bound)
     * @param n          the number of sampling points (granularity of the approximation)
     * @param c          the scaling factor for the sampling points
     */
    public BernsteinExponentialApproximation(UnivariateFunction f, Double lowerBound, Double upperBound, int n, double c) {
//		this.nPoints = new double[n+1];
//		this.lowerBound = lowerBound;
//		this.upperBound = upperBound;
//		this.c = c;
//		double eToMinusA = FastMath.exp(- this.lowerBound);
//		double eToMinusB = FastMath.exp(- this.upperBound);
//		double[] samplingPoints = new double[nPoints.length];
//		for(int i = 0; i < n; i++){
//			samplingPoints[i] = ( -FastMath.log( eToMinusA - (i/(double)n) * (eToMinusA - eToMinusB)))/c;
//			this.nPoints[i] = f.value(( -FastMath.log( eToMinusA - (i/(double)n) * (eToMinusA - eToMinusB)))/c); 
//		}
//		samplingPoints[n] = this.upperBound;
//		System.out.println("sampling points (n="+n+", c="+c+"):" +Arrays.toString(samplingPoints));
//		this.nPoints[n] = f.value(this.upperBound);
//		ensureIsDistribution(); 

        this.nPoints = new double[n + 1];
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.c = c;
        final double base = getBase(c);
        double eToMinusA = Math.pow(base, -this.lowerBound);
        double eToMinusB = Math.pow(base, -this.upperBound);
        double[] samplingPoints = new double[nPoints.length];
        for (int i = 0; i < n; i++) {
            samplingPoints[i] = -FastMath.log(base, eToMinusA - (i / (double) n) * (eToMinusA - eToMinusB));
            this.nPoints[i] = f.value(-FastMath.log(base, eToMinusA - (i / (double) n) * (eToMinusA - eToMinusB)));
        }
        samplingPoints[n] = this.upperBound;
        this.nPoints[n] = f.value(this.upperBound);

        System.out.println("sampling points (n=" + n + ", c=" + c + "):" + Arrays.toString(samplingPoints));
        this.nPoints[n] = f.value(this.upperBound);
        ensureIsDistribution();
    }

    private double getBase(double c) {
        return 1. + (FastMath.E - 1) * c;
    }

    private void ensureIsDistribution() {
        double scalingFactor = cumulativeProbability(getSupportUpperBound());
        if (Math.abs(scalingFactor - 1.0) > 0.00001) {
            // rescale and try again:
            for (int i = 0; i < nPoints.length; i++) {
                this.nPoints[i] /= scalingFactor;
            }
            ensureIsDistribution();
        }
    }

    public BernsteinExponentialApproximation(RealDistribution d, Double lowerBound, Double upperBound) {
        this(d, lowerBound, upperBound, DEFAULT_N);
    }

    public BernsteinExponentialApproximation(RealDistribution d, Double lowerBound, Double upperBound, int n) {
        this(d, lowerBound, upperBound, n, DEFAULT_C);
    }

    public BernsteinExponentialApproximation(RealDistribution d, Double lowerBound, Double upperBound, int n, double c) {
        this(DistributionUtils.getDensityFunction(d), lowerBound, upperBound, n, c);
    }


    public double density(double x) {
        double scaledX = x;

        if (scaledX < lowerBound) {
            return 0;
        }
        if (scaledX > upperBound) {
            return 0;
        }
        final double base = getBase(c);
        double eToMinusA = FastMath.pow(base, -lowerBound);
        double eToMinusB = FastMath.pow(base, -upperBound);
        double eToMinusX = FastMath.pow(base, -scaledX);
        int n = nPoints.length - 1;

        double density = 0.0;
        for (int i = 0; i < nPoints.length; i++) {
            density += nPoints[i] * ArithmeticUtils.binomialCoefficient(n, i) *
                    ((FastMath.pow(eToMinusA - eToMinusX, i) * FastMath.pow(eToMinusX - eToMinusB, n - i)) / FastMath.pow(eToMinusA - eToMinusB, n));
        }
        return density;
    }

    public double getSupportLowerBound() {
        return lowerBound;
    }

    public double getSupportUpperBound() {
        return upperBound;
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public boolean isSupportConnected() {
        return true;
    }

    public Double getSamplingConstantC() {
        return c;
    }

    public double[] getNPoints() {
        return nPoints;
    }

    public List<Double> getParameters() {
        List<Double> params = new ArrayList<>();
        params.add(getSupportLowerBound());
        params.add(getSupportUpperBound());
        params.add(getSamplingConstantC());
        for (int i = 0; i < nPoints.length; i++) {
            params.add(nPoints[i]);
        }
        return params;
    }

}
