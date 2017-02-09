package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

import java.util.Map;


public class WeightsOptimizer {

    private double[] weights;
    Map<String, int[]> markingBasedSelections;

    /**
     * @param weights                initial weights
     * @param markingBasedSelections a map of selection integers for markings (markings are string-encoded)
     */
    public WeightsOptimizer(double[] weights, Map<String, int[]> markingBasedSelections) {
        this.weights = weights;
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
