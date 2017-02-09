package org.processmining.plugins.stochasticpetrinet.miner.distance;

/**
 * The score of a candidate Log-Model pair vs. a starting Log-Model Pair.
 *
 * @author Andreas Rogge-Solti
 */
public class Distance {

    private double fitness;
    private double precision;
    private double generalization;

    private double logDistance;

    private double modelDistance;

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getGeneralization() {
        return generalization;
    }

    public void setGeneralization(double generalization) {
        this.generalization = generalization;
    }

    public double getLogDistance() {
        return logDistance;
    }

    public void setLogDistance(double logDistance) {
        this.logDistance = logDistance;
    }

    public double getModelDistance() {
        return modelDistance;
    }

    public void setModelDistance(double modelDistance) {
        this.modelDistance = modelDistance;
    }

    public Distance clone() {
        Distance clone = new Distance();
        clone.fitness = fitness;
        clone.generalization = generalization;
        clone.logDistance = logDistance;
        clone.modelDistance = modelDistance;
        clone.precision = precision;
        return clone;
    }
}
