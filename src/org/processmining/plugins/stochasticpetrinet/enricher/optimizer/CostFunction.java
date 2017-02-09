package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

public interface CostFunction {

    /**
     * returns the cost for a current assignment to the parameter vector theta.
     *
     * @param theta double[] the parameter vector
     * @return the cost for the current assignment of the vector.
     */
    public double getCost(double[] theta);

    /**
     * returns the partial derivation of the i-th index of the function
     *
     * @param theta the weight vector
     * @param index the index of the weight, of which the derivative is to be returned.
     * @return slope of the curve in the dimension i (value of the partial derivation in the i-th dimension)
     */
    public double getPartialDerivation(double[] theta, int i);
}
