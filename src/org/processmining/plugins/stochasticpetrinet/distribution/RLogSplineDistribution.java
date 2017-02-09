package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

/**
 * Logspline density fitting to data using the logspline package in R.
 *
 * @author Andreas Rogge-Solti
 */
public class RLogSplineDistribution extends AbstractRealDistribution {
    private static final long serialVersionUID = -8263917191038257266L;

    private static int counter = 0;

    protected double upperBound = Double.MAX_VALUE;

    /**
     * name of the spline in R
     */
    protected String rName;

    /**
     * reference to the R engine
     */
    protected Rengine engine;

    /**
     * The logspline method is used of the logspline package of R
     */
    protected String method = "logspline";

    protected double numericalMean = Double.NaN;

    protected double[] values;

    public RLogSplineDistribution() {
        this("my_spline" + counter++);
    }

    public RLogSplineDistribution(double maxValue) {
        this(maxValue, "my_spline" + counter++);
    }

    public RLogSplineDistribution(String name) {
        this(Double.MAX_VALUE, name);
    }

    public RLogSplineDistribution(double maxValue, String name) {
        super(null);
        rName = name;
        upperBound = maxValue;
        engine = RProvider.getEngine();
    }

    public void addValues(double... values) throws NonConvergenceException {
        try {
            this.values = values;
            REXP rValues = new REXP(values);
            engine.assign(getValsString(), rValues);
            int knots = Math.max(5, Math.min(12, 5 + (int) Math.log(values.length)));
            REXP exp = engine.eval(rName + " <- logspline(" + getValsString() + ",lbound=0,ubound=" + upperBound + ",maxknots=" + knots + ")");
            if (exp == null) {
                // failed to converge!
                throw new NonConvergenceException("Logspline fit to " + values.length + " did not converge (maybe the values are too close to each other...)");
            }
        } finally {
            engine.eval("rm(" + getValsString() + ")");
        }
    }

    public String getValsString() {
        return rName + "vals";
    }

    public double probability(double x) {
        throw new UnsupportedOperationException("probability not supported!");
    }

    public double density(double x) {
        return engine.eval("d" + method + "(" + x + "," + rName + ")").asDouble();
    }

    public double cumulativeProbability(double x) {
        return engine.eval("p" + method + "(" + x + "," + rName + ")").asDouble();
    }

    /**
     * Calculates the numerical mean under the plot function of the log-spline fit.
     * The upper bound for integration is the longest observed trace.
     * Might return Double.NaN in the worst case.
     */
    public double getNumericalMean() {
        long now = System.currentTimeMillis();
//		try{
//			if (Double.isNaN(numericalMean)){
//				double max = Double.MIN_VALUE;
//				for (double value : values){
//					max = Math.max(max, value);
//				}
//				upperBound = 2*max+10;
//	//			if (upperBound == max+2){
//	//				// try to get a nicer value:
//	//				upperBound = engine.eval("q"+method+"(0.99999,"+rName+")").asDouble();
//	//			}
//				if (upperBound > 0){
//					UnivariateIntegrator integrator = new TrapezoidIntegrator(0.01,0.001,2,64);
//					numericalMean = integrator.integrate(100000, DistributionUtils.getWeightedFunction(this), 0, upperBound);
//						
//					System.out.println("Mean calculation ("+numericalMean+") based on integration ("+method+" method) took "+(System.currentTimeMillis()-now)+"ms");
//					if (numericalMean < 0 || numericalMean > upperBound){
//						System.out.println("Debug me!");
//					}
//				}
//			}
//		} catch (TooManyEvaluationsException tme){
//			// integration failed...
//		} catch (MaxCountExceededException mce){
//			
//		}
        if (Double.isNaN(numericalMean) || Double.isInfinite(numericalMean)) {
            // work around to find the mean by drawing a 10000 size sample and getting it's mean:
            now = System.currentTimeMillis();
            int N = 10000; // a sample mean of 10000 samples has a
            REXP exp = engine.eval("mean(r" + method + "(" + N + "," + rName + "))");
            if (exp != null) {
                double sampleMean = exp.asDouble();
                System.out.println("Mean calculation based on " + N + " samples took " + (System.currentTimeMillis() - now) + "ms.\n" +
                        "sample mean = " + sampleMean);
                numericalMean = sampleMean;
            }
        }
        return numericalMean;
    }

    public double getNumericalVariance() {
        return Double.NaN;
    }

    public double getSupportLowerBound() {
        return 0;
    }

    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    public boolean isSupportLowerBoundInclusive() {
        return true;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public boolean isSupportConnected() {
        return false;
    }

    public double sample() {
        return engine.eval("r" + method + "(1," + rName + ")").asDouble();
    }

    public double[] sample(int sampleSize) {
        return engine.eval("r" + method + "(" + sampleSize + "," + rName + ")").asDoubleArray();
    }
}