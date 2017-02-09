package org.processmining.plugins.overfit;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

public class OverfitMinerPlugin {
    @Plugin(name = "Mine overfitting Petri Net Model",
            parameterLabels = {"Log"},
            returnLabels = {"Petri net", "Marking"},
            returnTypes = {Petrinet.class, Marking.class},
            userAccessible = true,
            help = "Preserves all paths in the log and creates a perfectly overfitting Petri net model without parallelism or loops.")

    @UITopiaVariant(affiliation = "WU Vienna", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
    public Object[] transform(UIPluginContext context, XLog log) {
        OverfitMiner miner = new OverfitMiner();
        return miner.mine(context, log);
    }
}
