package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

public class NormalizedPositiveGradientDescent extends GradientDescent {

    /**
     * Renormalizes weights, such that their average is 1 and ensures they are all positive.
     */
    protected void postProcessWeights(double[] theta) {
        super.postProcessWeights(theta);
        normalize(theta);
    }


    public static void normalize(double[] weights) {
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] < 0) {
                weights[i] = 0;
            }
            sum += weights[i];
        }
        double scaleNormal = weights.length / sum;

        for (int i = 0; i < weights.length; i++) {
            weights[i] *= scaleNormal;
        }
    }

}
