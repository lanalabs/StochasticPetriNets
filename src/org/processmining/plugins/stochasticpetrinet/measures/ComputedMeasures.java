package org.processmining.plugins.stochasticpetrinet.measures;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class ComputedMeasures implements Serializable {
    private static final long serialVersionUID = -7320324015497590779L;

    private List<AbstractMeasure<? extends Number>> computedMeasures;

    public ComputedMeasures() {
        computedMeasures = new LinkedList<>();
    }

    public ComputedMeasures(List<AbstractMeasure<? extends Number>> computedMeasures) {
        this.computedMeasures = computedMeasures;
    }

    public List<AbstractMeasure<? extends Number>> getComputedMeasures() {
        return computedMeasures;
    }

    public void setComputedMeasures(List<AbstractMeasure<? extends Number>> computedMeasures) {
        this.computedMeasures = computedMeasures;
    }

    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (AbstractMeasure<? extends Number> measure : computedMeasures) {
            builder.append(measure.toString()).append(":").append(measure.getValue()).append("\n");
        }
        return builder.toString();
    }

}