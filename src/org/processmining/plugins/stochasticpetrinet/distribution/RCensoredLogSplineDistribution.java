package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

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
	
	/** name of the spline in R */
	protected String rName;
	/** static counter to avoid name clashes in R */ 
	protected static int counter = 1;
	/** reference to the R engine */
	private Rengine engine;
	
	public RCensoredLogSplineDistribution(){
		super(new JDKRandomGenerator());
		rName = "my_spline"+counter++;
		engine = RProvider.getEngine();
	}
	
	/**
	 * Initializes the log spline density estimation with the observed and right censored values.
	 * TODO: delete observations and censored values after fitting process, to increase performance.
	 * @param observedValues
	 * @param censoredValues
	 */
	public void initWithValues(double[] observedValues, double[] censoredValues){
		REXP rObsValues = new REXP(observedValues);
		REXP rCensoredValues = new REXP(censoredValues);
		engine.assign(getObservedValuesString(), rObsValues);
		engine.assign(getRightCensoredValuesString(), rCensoredValues);
		engine.eval(rName+" <- oldlogspline(uncensored="+getObservedValuesString()+",right="+getRightCensoredValuesString()+",lbound=0)");
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
		return engine.eval("doldlogspline("+x+","+rName+")").asDouble();
	}

	public double cumulativeProbability(double x) {
		return engine.eval("poldlogspline("+x+","+rName+")").asDouble();
	}

	public double getNumericalMean() {
		return Double.NaN;
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
