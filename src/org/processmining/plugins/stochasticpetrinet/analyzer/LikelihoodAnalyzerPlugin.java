package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;

public class LikelihoodAnalyzerPlugin {

    @Plugin(name = "Compute Likelihoods",
            parameterLabels = {"Event Log", StochasticNet.PARAMETER_LABEL},
            returnLabels = {CaseStatisticsList.PARAMETER_LABEL},
            returnTypes = {CaseStatisticsList.class},
            userAccessible = true,
            help = "Returns a list of log-likelihood values for all the traces in the log.")

    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public CaseStatisticsList transform(UIPluginContext context, XLog log, StochasticNet net) {
        CaseStatisticsList loglikelihoods = LikelihoodAnalyzer.getLogLikelihoods(context, log, net);

        return loglikelihoods;
    }
}
