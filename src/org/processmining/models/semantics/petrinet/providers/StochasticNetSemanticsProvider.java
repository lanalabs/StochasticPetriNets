package org.processmining.models.semantics.petrinet.providers;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;

@Plugin(name = "Stochastic Net Semantics Provider", parameterLabels = {}, returnLabels = {"GStochastic Net Semantics"}, returnTypes = {StochasticNetSemantics.class}, userAccessible = true)
public class StochasticNetSemanticsProvider {

    @PluginVariant(variantLabel = "Regular Semantics", requiredParameterLabels = {})
    public StochasticNetSemantics provideNormal(PluginContext context) {
        context.getFutureResult(0).setLabel("Regular semantics");
        return new StochasticNetSemanticsImpl();
    }

}
