package org.processmining.plugins.temporal;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.temporal.model.TemporalModel;

public class TemporalMinerPlugin {


    @Plugin(name = "Mine Temporal Model",
            parameterLabels = {"Log"},
            returnLabels = {TemporalModel.PARAMETER_LABEL},
            returnTypes = {TemporalModel.class},
            userAccessible = true,
            help = "Extracts temporal relations from a log between events.")

    @UITopiaVariant(affiliation = "WU Vienna", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
    public TemporalModel transform(UIPluginContext context, XLog log) {
        TemporalMiner miner = new TemporalMiner();
        return miner.mine(context, log);
    }

}
