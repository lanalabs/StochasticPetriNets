package org.processmining.plugins.alignment;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.alignment.model.CaseTimeSeries;

public class TimeSeriesAlignmentPlugin {

    @Plugin(name = "Convert Log to Time Series",
            parameterLabels = {"Event Log"},
            returnLabels = {CaseTimeSeries.PARAMETER_LABEL},
            returnTypes = {CaseTimeSeries.class},
            userAccessible = true,
            help = "Returns a list of log-likelihood values for all the traces in the log.")

    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public CaseTimeSeries transform(UIPluginContext context, XLog log, StochasticNet net) {
        CaseTimeSeries timeSeries = TimeSeriesConverter.convert(context, log);

        return timeSeries;
    }
}
