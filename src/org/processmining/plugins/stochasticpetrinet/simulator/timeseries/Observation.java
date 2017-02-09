package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

/**
 * An individual historical observation on which to base future predictions.
 */
public class Observation<H> {

    /**
     * time stamp of the occurrence of this observation
     */
    public long timestamp;

    /**
     * the duration of the observed value, e.g. the duration in ms (can be 0)
     */
    public H observation;

    /**
     * additional information
     */
    public Object[] payload;
}
