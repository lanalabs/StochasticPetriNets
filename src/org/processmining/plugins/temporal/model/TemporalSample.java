package org.processmining.plugins.temporal.model;


public class TemporalSample {
    public TemporalSample(double temporalDistance, int traceDistance) {
        this.duration = temporalDistance;
        this.distanceInTrace = traceDistance;
    }

    private int distanceInTrace;
    private double duration;

    public int getDistanceInTrace() {
        return distanceInTrace;
    }

    public double getDuration() {
        return duration;
    }

}
