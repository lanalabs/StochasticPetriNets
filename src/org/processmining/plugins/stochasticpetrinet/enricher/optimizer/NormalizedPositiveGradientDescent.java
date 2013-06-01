package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

public class NormalizedPositiveGradientDescent extends GradientDescent {

	/**
	 * Renormalizes weights, such that their average is 1 and ensures they are all positive.
	 */
	protected void postProcessWeights(double[] theta) {
		super.postProcessWeights(theta);
		double sum = 0;
		for (int i = 0; i < theta.length; i++){
			if (theta[i] < 0){
				theta[i] = 0;
			}
			sum += theta[i];
		}
		double scaleNormal = theta.length/sum;
		
		for (int i = 0; i < theta.length; i++){
			theta[i] *= scaleNormal;
		}
	}

}
