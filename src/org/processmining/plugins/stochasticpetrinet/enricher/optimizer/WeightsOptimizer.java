package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

import java.util.Map;

import org.processmining.plugins.stochasticpetrinet.enricher.CollectorCounter;


public class WeightsOptimizer {

	private double[] weights;
	Map<String,int[]> markingBasedSelections;
	
	/**
	 * 
	 * @param weights initial weights
	 * @param performanceCounter {@link CollectorCounter} containing collected transition counts for each marking after replay
	 */
	public WeightsOptimizer(double[] weights, Map<String, int[]> markingBasedSelections) {
		this.weights =  weights;
		this.markingBasedSelections = markingBasedSelections; 
	}

	/**
	 * optimizes weights in a gradient descent way.
	 * 
	 * @return
	 */
	public double[] optimizeWeights() {
		MarkingBasedSelectionWeightCostFunction markingBasedCostFunction = new MarkingBasedSelectionWeightCostFunction(markingBasedSelections);
		
		GradientDescent gradientDescent = new NormalizedPositiveGradientDescent();
		return gradientDescent.optimize(weights, markingBasedCostFunction);
	}
	
	

}
