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

	private double numericalMean = Double.NaN;
	
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
		long now = System.currentTimeMillis();
//		if (Double.isNaN(numericalMean)){
//			UnivariateIntegrator integrator = new IterativeLegendreGaussIntegrator(16,0.01,0.000001);
//			double upperBound = inverseCumulativeProbability(.99)+10;
//			numericalMean = integrator.integrate(integrator.getMaximalIterationCount(), DistributionUtils.getWeightedFunction(this), 0, upperBound);
//			System.out.println("Mean calculation based on integration took "+(System.currentTimeMillis()-now)+"ms");
//		}
		if (Double.isNaN(numericalMean)){
			// work around to find the mean by drawing a 10000 size sample and getting it's mean:
			now = System.currentTimeMillis();
			int N = 10000; // a sample mean of 10000 samples has a 
			double sampleMean =  engine.eval("mean(rlogspline("+N+","+rName+"))").asDouble();
			System.out.println("Mean calculation based on "+N+" samples took "+(System.currentTimeMillis()-now)+"ms.\n" +
					"sample mean = "+sampleMean);
//							", numerical mean = "+numericalMean+"\n" +
//					"Difference to numerical mean based on integration: "+(numericalMean-sampleMean)+" ~= " +((numericalMean/sampleMean - 1.)*100)+" percent.");
			numericalMean = sampleMean;
		}
		return numericalMean;
	}

	public double getNumericalVariance() {
		return Double.NaN;
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