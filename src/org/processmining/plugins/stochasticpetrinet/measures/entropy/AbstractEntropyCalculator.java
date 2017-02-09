package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.measures.AbstractionLevel;
import org.processmining.plugins.stochasticpetrinet.measures.MeasureProvider;

import java.util.Map;

public abstract class AbstractEntropyCalculator implements MeasureProvider<EntropyMeasure> {

    private AbstractionLevel level;

    protected boolean logResults = true;

    public final EntropyMeasure getMeasure(UIPluginContext context, Petrinet net) {
        Map<Outcome, Double> outcomesAndCounts = getOutcomesAndCounts(context, net, StochasticNetUtils.getInitialMarking(context, net), level);

        EntropyMeasure measure = getEntropyForOutcomes(outcomesAndCounts);
        return measure;
    }

    public void setLogResults(boolean logResults) {
        this.logResults = logResults;
    }

    protected EntropyMeasure getEntropyForOutcomes(Map<Outcome, Double> outcomesAndCounts) {
        EntropyMeasure measure = new EntropyMeasure(level, getNameInfo());
        double sumOfCounts = 0;
        for (Double count : outcomesAndCounts.values()) {
            sumOfCounts += count;
        }
        double value = 0;
        for (Double count : outcomesAndCounts.values()) {
            double p = count / sumOfCounts;
            value += -p * Math.log(p) / Math.log(2);
        }
        measure.setValue(value);
        return measure;
    }

    protected abstract String getNameInfo();

    public void setAbstractionLevel(AbstractionLevel level) {
        this.level = level;
    }

    protected abstract Map<Outcome, Double> getOutcomesAndCounts(UIPluginContext context, Petrinet net, Marking initialMarking, final AbstractionLevel level);

}
