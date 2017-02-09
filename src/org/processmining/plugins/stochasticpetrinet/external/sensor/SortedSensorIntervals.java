package org.processmining.plugins.stochasticpetrinet.external.sensor;

import java.util.TreeSet;

public class SortedSensorIntervals extends TreeSet<SensorInterval> {
    private static final long serialVersionUID = -5769912261861574810L;
    public static final String PARAMETER_LABEL = "Sensor Intervals";

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(SensorInterval.getHeader()).append("\n");

        for (SensorInterval interval : this) {
            builder.append(interval.toString()).append("\n");
        }
        return builder.toString();
    }

}
