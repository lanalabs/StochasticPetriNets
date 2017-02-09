package org.processmining.plugins.stochasticpetrinet.measures;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

import java.util.LinkedList;
import java.util.List;

public class MeasurePlugin {

    @Plugin(name = "Compute a model measure",
            parameterLabels = {"Petri net"},
            returnLabels = {"Computed Measures"},
            returnTypes = {ComputedMeasures.class},
            userAccessible = true,
            help = "Asks the user to select a bunch of measures to .")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
    public ComputedMeasures getMeasure(UIPluginContext context, Petrinet net) {
        return getMeasure(context, net, new MeasureConfig());
    }

    public ComputedMeasures getMeasure(UIPluginContext context, Petrinet net, MeasureConfig config) {
        // compute all combinations by default:
        List<AbstractMeasure<? extends Number>> measures = new LinkedList<>();
        for (MeasureProvider provider : config.getMeasureProviders()) {
            for (AbstractionLevel level : config.getAbstractionLevels()) {
                provider.setAbstractionLevel(level);
                AbstractMeasure measure = provider.getMeasure(context, net);
                measures.add(measure);
            }
        }
        return new ComputedMeasures(measures);
    }

}
