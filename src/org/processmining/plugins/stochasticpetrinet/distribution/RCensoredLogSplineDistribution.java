package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

import cern.colt.Arrays;

/**
 * Wraps the logspline density estimation for data that is randomly right-censored. 
 * This happens often, if only the fastest transition in the stochastic Petri net is observed.
 * All competing transitions' samples that lost the race and whose sampled durations are not 
 * reconstructable from replay, need to be taken into account as censored data.
 * 
 * uses the oldlogspline function described in: 	
 * <a href="http://amstat.tandfonline.com/doi/full/10.1080/10618600.1992.10474588">Kooperberg, Charles, and Charles J. Stone. "Logspline density estimation for censored data." 
 * Journal of Computational and Graphical Statistics 1.4 (1992): 301-328.</a>
 * 
 * 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class RCensoredLogSplineDistribution extends AbstractRealDistribution {
	private static final long serialVersionUID = 2700414963204442814L;

	/**
	 * The tries to fit a log-spline to censored data
	 */
	private static final int MAX_RETRIES = 15;
	
	/** name of the spline in R */
	protected String rName;
	/** static counter to avoid name clashes in R */ 
	protected static int counter = 1;
	/** reference to the R engine */
	private Rengine engine;

	private double numericalMean = Double.NaN;
	
	private String method = "oldlogspline";
	
	public RCensoredLogSplineDistribution(){
		super(new JDKRandomGenerator());
		rName = "myCensoredSpline"+counter++;
		engine = RProvider.getEngine();
	}
	
	/**
	 * Initializes the log spline density estimation with the observed and right censored values.
	 * TODO: delete observations and censored values after fitting process, to increase performance.
	 * @param observedValues
	 * @param censoredValues
	 */
	public void initWithValues(double[] observedValues, double[] censoredValues) throws NonConvergenceException{
		REXP rObsValues = new REXP(observedValues);
		REXP rCensoredValues = new REXP(censoredValues);
		engine.assign(getObservedValuesString(), rObsValues);
		engine.assign(getRightCensoredValuesString(), rCensoredValues);
		int knots = -1;
		if (observedValues.length > 500){
			knots = Math.max(5, Math.min(15, 5+(int)Math.log(observedValues.length)));
		}
		REXP expr = engine.eval(rName+" <- "+method+"(uncensored="+getObservedValuesString()+",right="+getRightCensoredValuesString()+", "+(knots>1?"nknots="+knots+",":"")+" lbound=0)");
		int tries = 0;
		double[] lessCensoredValue = censoredValues.clone();
		while (expr == null && tries++ < MAX_RETRIES){
			// no convergence ->
			// try with less censored data:
			lessCensoredValue = new double[9*lessCensoredValue.length/10];
			for (int i = 0; i < lessCensoredValue.length; i++){
				lessCensoredValue[i] = censoredValues[i];
			}
			REXP rLessCensoredValues = new REXP(lessCensoredValue);
			System.err.println("...retrying with "+lessCensoredValue.length+" censored values.");
			
			engine.assign(getRightCensoredValuesString(), rLessCensoredValues);
			expr = engine.eval(rName+" <- "+method+"(uncensored="+getObservedValuesString()+",right="+getRightCensoredValuesString()+", "+(knots>1?"nknots="+(--knots)+",":"")+" lbound=0)");
		}
		if (expr == null) {
			// fall back to simple logspline. 
			System.err.println("The fit of the logspline to observed: "+Arrays.toString(observedValues)+" with "+censoredValues.length+" unobserved values did not converge. Falling back to not use censored data...");
			method="logspline";
			expr = engine.eval(rName+" <- "+method+"("+getObservedValuesString()+", lbound=0)");
			if (expr == null){
				// still no convergence!
				throw new NonConvergenceException("The logspline fit of the values "+Arrays.toString(observedValues)+" did not converge!");
			}
		}
//		System.out.println(expr.getContent());
		
		engine.eval("rm("+getObservedValuesString()+")");
		engine.eval("rm("+getRightCensoredValuesString()+")");
	}

	public String getObservedValuesString() {
		return rName+"ObsVals";
	}
	public String getRightCensoredValuesString() {
		return rName+"CensoredVals";
	}
	
	public double probability(double x) {
		throw new UnsupportedOperationException("probability not supported!");
	}

	public double density(double x) {
		return engine.eval("d"+method+"("+x+","+rName+")").asDouble();
	}

	public double cumulativeProbability(double x) {
		return engine.eval("p"+method+"("+x+","+rName+")").asDouble();
	}


	public double getNumericalMean() {
		
		long now = System.currentTimeMillis();
		// disabled for now, as integration does not work, currently.
//		if (Double.isNaN(numericalMean)){
//			UnivariateIntegrator integrator = new IterativeLegendreGaussIntegrator(16,0.01,0.000001);
//			double upperBound = inverseCumulativeProbability(.99)+10;
//			numericalMean = integrator.integrate(integrator.getMaximalIterationCount(), DistributionUtils.getWeightedFunction(this), 0, upperBound);
//			System.out.println("Mean calculation based on integration took "+(System.currentTimeMillis()-now)+"ms");
//		}
		
		if(Double.isNaN(numericalMean)){
			// work around to find the mean by drawing a 10000 size sample and getting it's mean:
			now = System.currentTimeMillis();
			int N = 10000;
			double sampleMean =  engine.eval("mean(r"+method+"("+N+","+rName+"))").asDouble();
			System.out.println("Mean calculation based on "+N+" samples took "+(System.currentTimeMillis()-now)+"ms.\n" +
					"sample mean = "+sampleMean); // +", numerical mean = "+numericalMean+"\n" +
					//"Difference to numerical mean based on integration: "+(numericalMean-sampleMean)+" ~= " +((numericalMean/sampleMean - 1.)*100)+" percent.");
			numericalMean = sampleMean;
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
}
