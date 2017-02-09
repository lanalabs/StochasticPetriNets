package org.processmining.plugins.temporal.model;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andreas Rogge-Solti
 */
public class TemporalConnection {

    private List<TemporalSample> samples;

    private DescriptiveStatistics stats;

    private TemporalNode before;
    private TemporalNode after;

    public TemporalConnection(TemporalNode fromNode, TemporalNode toNode) {
        this.before = fromNode;
        this.after = toNode;
        this.stats = new DescriptiveStatistics();
        this.samples = new ArrayList<>();
    }

    public void addSample(double temporalDistance, int traceDistance) {
        this.samples.add(new TemporalSample(temporalDistance, traceDistance));
        if (traceDistance == 1) {
            stats.addValue(temporalDistance);
        }
    }

    public List<TemporalSample> getSamples() {
        return samples;
    }

    public void setSamples(List<TemporalSample> samples) {
        this.samples = samples;
    }

    public DescriptiveStatistics getStats() {
        return stats;
    }

    public void setStats(DescriptiveStatistics stats) {
        this.stats = stats;
    }

    public TemporalNode getBefore() {
        return before;
    }

    public TemporalNode getAfter() {
        return after;
    }

}
