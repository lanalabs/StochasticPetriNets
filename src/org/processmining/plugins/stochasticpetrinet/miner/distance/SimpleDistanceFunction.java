package org.processmining.plugins.stochasticpetrinet.miner.distance;

public class SimpleDistanceFunction implements DistanceFunction {

    public double getFinalDistance(Distance dist) {
        return (dist.getLogDistance() + dist.getModelDistance() + (1 - (dist.getFitness()+dist.getPrecision()) / 2.0 )) / 3.;
    }

}
