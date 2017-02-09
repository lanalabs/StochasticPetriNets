package org.processmining.plugins.stochasticpetrinet.measures;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;

public interface MeasureProvider<T extends AbstractMeasure<?>> {

    /**
     * Calculates a model measure for a given {@link Petrinet}.
     *
     * @param context {@link UIPluginContext} provided by the ProM framework.
     * @param net     {@link Petrinet} representing the model structure.
     * @return a subclass of {@link AbstractMeasure}
     */
    public T getMeasure(UIPluginContext context, Petrinet net);

    /**
     * Name of the measure that will be computed
     *
     * @return String the name
     */
    public String getMeasureName();

    /**
     * Tells the measure provider which abstraction level to use
     *
     * @param level {@link AbstractionLevel} to use in computing the measure
     */
    public void setAbstractionLevel(AbstractionLevel level);
}
