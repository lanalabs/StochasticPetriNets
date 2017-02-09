package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import org.processmining.plugins.stochasticpetrinet.measures.AbstractMeasure;
import org.processmining.plugins.stochasticpetrinet.measures.AbstractionLevel;

/**
 * A class that captures the entropy of a net.
 */
public class EntropyMeasure extends AbstractMeasure<Double> {

    private AbstractionLevel level;

    private String info;

    public EntropyMeasure(AbstractionLevel level, String info) {
        this.level = level;
        this.info = info;
    }

    public String getName() {
        return "Entropy " + level.getName() + "(" + info + ")";
    }

}
