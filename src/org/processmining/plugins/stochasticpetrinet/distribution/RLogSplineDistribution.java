package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;

public class RLogSplineDistribution extends AbstractRealDistribution{
	private static final long serialVersionUID = -8263917191038257266L;

	private static int counter = 0;
	
	private String rName;
	
	private Rengine engine;
	
	public RLogSplineDistribution(){
		super(new JDKRandomGenerator());
		rName = "my_spline"+counter++;
		engine = RProvider.getEngine();
	}
	
	public void addValues(double... values){
		REXP rValues = new REXP(values);
		engine.assign(getValsString(), rValues);
		engine.eval(rName+" <- logspline("+getValsString()+")");
	}

	public String getValsString() {
		return rName+"vals";
	}
	
	public double probability(double x) {
		throw new UnsupportedOperationException("probability not supported!");
	}

	public double density(double x) {
		return engine.eval("dlogspline("+x+","+rName+")").asDouble();
	}

	public double cumulativeProbability(double x) {
		return engine.eval("plogspline("+x+","+rName+")").asDouble();
	}

	public double getNumericalMean() {
		throw new UnsupportedOperationException("numerical mean not supported!");
	}

	public double getNumericalVariance() {
		throw new UnsupportedOperationException("numerical variance not supported!");
	}

	public double getSupportLowerBound() {
		return Double.NEGATIVE_INFINITY;
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