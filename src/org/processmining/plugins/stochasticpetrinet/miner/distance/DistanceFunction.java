package org.processmining.plugins.stochasticpetrinet.miner.distance;

public interface DistanceFunction {

    /**
     * Computes some (weighted) average of the individual distance components.
     *
     * @param dist Distance object containing all dimensions of distances between log and model
     * @return the final distance in form of a double. Values should be between 0 (best) and 1 (worst)
     */
    public double getFinalDistance(Distance dist);
}
