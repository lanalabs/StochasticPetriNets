package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.rosuda.JRI.REXP;

/**
 * Wraps the logspline density estimation for data that is randomly right-censored.
 * This happens often, if only the fastest transition in the stochastic Petri net is observed.
 * All competing transitions' samples that lost the race and whose sampled durations are not
 * reconstructable from replay, need to be taken into account as censored data.
 * <p>
 * uses the oldlogspline function described in:
 * <a href="http://amstat.tandfonline.com/doi/full/10.1080/10618600.1992.10474588">Kooperberg, Charles, and Charles J. Stone. "Logspline density estimation for censored data."
 * Journal of Computational and Graphical Statistics 1.4 (1992): 301-328.</a>
 *
 * @author Andreas Rogge-Solti
 */
public class RCensoredLogSplineDistribution extends RLogSplineDistribution {
    private static final long serialVersionUID = 2700414963204442814L;

    /**
     * The tries to fit a log-spline to censored data
     */
    private static final int MAX_RETRIES = 10;

    /**
     * static counter to avoid name clashes in R
     */
    protected static int counter = 1;

    /**
     * @deprecated Might cause problems, when integrating the function, as no effective upper bound is given.
     */
    public RCensoredLogSplineDistribution() {
        this(Integer.MAX_VALUE - 1);
    }

    public RCensoredLogSplineDistribution(double maxTraceLength) {
        super("myCensoredSpline" + counter++);
        method = "oldlogspline";
        upperBound = maxTraceLength;
    }

    /**
     * Initializes the log spline density estimation with the observed and right censored values.
     * TODO: delete observations and censored values after fitting process, to increase performance.
     *
     * @param observedValues
     * @param censoredValues
     */
    public void initWithValues(double[] observedValues, double[] censoredValues) throws NonConvergenceException {
        this.values = observedValues;
        REXP rObsValues = new REXP(observedValues);
        REXP rCensoredValues = new REXP(censoredValues);
        engine.assign(getObservedValuesString(), rObsValues);
        engine.assign(getRightCensoredValuesString(), rCensoredValues);
        try {
            int knots = -1;
            if (observedValues.length > 500) {
                knots = Math.max(5, Math.min(12, 5 + (int) Math.log(observedValues.length)));
            }
            REXP expr = engine.eval(rName + " <- " + method + "(uncensored=" + getObservedValuesString() + ",right=" + getRightCensoredValuesString() + ", " + (knots > 1 ? "nknots=" + knots + "," : "") + " lbound=0, ubound=" + ((int) upperBound + 1) + ")");
            int tries = 0;
            double[] lessCensoredValue = censoredValues.clone();
            while (expr == null && tries++ < MAX_RETRIES) {
                // no convergence ->
                // try with less censored data:
                lessCensoredValue = new double[9 * lessCensoredValue.length / 10];
                for (int i = 0; i < lessCensoredValue.length; i++) {
                    lessCensoredValue[i] = censoredValues[i];
                }
                REXP rLessCensoredValues = new REXP(lessCensoredValue);
                System.err.println("...retrying with " + lessCensoredValue.length + " censored values.");

                engine.assign(getRightCensoredValuesString(), rLessCensoredValues);
                expr = engine.eval(rName + " <- " + method + "(uncensored=" + getObservedValuesString() + ",right=" + getRightCensoredValuesString() + ", " + (knots > 1 ? "nknots=" + (--knots) + "," : "") + " lbound=0, ubound=" + ((int) upperBound + 1) + ")");
            }
            if (expr == null) {
                String reason = "non-convergence";
                if (observedValues.length < 50) {
                    reason = "sample size of " + observedValues.length + " might be too small!";
                }
                // fall back to simple logspline.
                throw new NonConvergenceException("Could not fit a logspline to the " + observedValues + " observed values and " + censoredValues.length + " censored values!\n" +
                        "Probably due to " + reason);
            } else {
                // check whether convergence achieved.
                try {
                    double mean = getNumericalMean();
                    if (Double.isInfinite(mean) || Double.isNaN(mean)) {
                        // fall back to simple logspline.
                        throw new NonConvergenceException("Could not fit a logspline to the " + observedValues + " observed values and " + censoredValues.length + " censored values!\n" +
                                "Probably due to infinite mean.");
                    }
                } catch (TooManyEvaluationsException e) {
                    throw new NonConvergenceException("Mean calculation impossible with this logspline fit!");
                }
            }
        } finally {
            engine.eval("rm(" + getObservedValuesString() + ")");
            engine.eval("rm(" + getRightCensoredValuesString() + ")");
        }
    }

    public String getObservedValuesString() {
        return rName + "ObsVals";
    }

    public String getRightCensoredValuesString() {
        return rName + "CensoredVals";
    }

//	public double getNumericalMean() {
//		
//		long now = System.currentTimeMillis();
//		// disabled for now, as integration does not work, currently.
//		if (Double.isNaN(numericalMean)){
////			UnivariateIntegrator integrator = new IterativeLegendreGaussIntegrator(2,0.01,0.01);
//			UnivariateIntegrator integrator = new TrapezoidIntegrator(0.01,0.0001,3,64);
//			numericalMean = integrator.integrate(100000, DistributionUtils.getWeightedFunction(this), 0, upperBound);
//				
//			System.out.println("Mean calculation ("+numericalMean+") based on integration took "+(System.currentTimeMillis()-now)+"ms");
//			if (numericalMean < 0){
//				System.out.println("Debug me!");
//			}
//		}
//		
//		if(Double.isNaN(numericalMean) || Double.isInfinite(numericalMean)){
//			// work around to find the mean by drawing a 10000 size sample and getting it's mean:
//			now = System.currentTimeMillis();
//			int N = 10000;
//			double[] samples =  engine.eval("r"+method+"("+N+","+rName+")").asDoubleArray();
//			int countNaNs = 0;
//			DescriptiveStatistics stats = new DescriptiveStatistics();
//			for (double d : samples){
//				if (Double.isNaN(d) || Double.isInfinite(d)){
//					countNaNs++;
//				} else if (d >= 0){
//					stats.addValue(d);
//				} else {
//					System.out.println("Debug me!");
//				}
//			}
//			if (countNaNs < N-2){
//				numericalMean = stats.getMean();
//			} else {
//				numericalMean = Double.NaN;
//			}
//			if (numericalMean < 0){
//				numericalMean = 0;
//			}
//			
//			System.out.println("Mean calculation based on "+N+" samples took "+(System.currentTimeMillis()-now)+"ms.\n" +
//					"sample mean = "+numericalMean+", omitted "+countNaNs+" values."); // +", numerical mean = "+numericalMean+"\n" +
//					//"Difference to numerical mean based on integration: "+(numericalMean-sampleMean)+" ~= " +((numericalMean/sampleMean - 1.)*100)+" percent.");
//		}
//		
//		
//		return numericalMean;
//	}

    public double getNumericalVariance() {
        return Double.NaN;
    }
}
