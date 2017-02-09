package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

import java.util.Arrays;

public class GradientDescent {

    private static final double EPSILON = 1e-15;

    public double[] optimize(double[] theta, CostFunction costFunction) {
        boolean converged = false;
        double learningRate = 1;

        double lastchange = Double.NaN;
        int iterations = 0;
        while (!converged && iterations++ < 1000000) {
            double[] temp = new double[theta.length];
            Arrays.fill(temp, 0);

            double change = 0;
            for (int i = 0; i < theta.length; i++) {
                temp[i] = costFunction.getPartialDerivation(theta, i);
                change += temp[i] * temp[i];
            }
            change = Math.sqrt(change);

            for (int i = 0; i < theta.length; i++) {
                theta[i] = theta[i] - learningRate * temp[i];
            }

            if (!Double.isNaN(lastchange) && lastchange < change) {
                // we are diverging!
                learningRate = learningRate / 3;
                System.out.println("Slowing learning rate to: " + learningRate + "...");
            }
            if (Double.isNaN(lastchange)) {
                lastchange = change;
            }
            if (Math.abs(change) < EPSILON) {
                converged = true;
            }
//			System.out.println("iteration "+iterations+", change: "+change+", theta: "+Arrays.toString(theta)+ "Cost: "+ costFunction.getCost(theta));
            // can be overwritten by sub-classes for normalization purposes
            postProcessWeights(theta);
        }
        System.out.println("Gradient descent converged: " + converged + " after " + iterations + " iterations.");
        System.out.println("Cost: " + costFunction.getCost(theta) + " for vector " + Arrays.toString(theta));
        return theta;
    }

    /**
     * can be overwritten by sub-classes for normalization purposes
     *
     * @param theta weights to be normalized
     */
    protected void postProcessWeights(double[] theta) {
        // TODO Auto-generated method stub
    }
}
