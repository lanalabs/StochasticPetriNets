package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.distance.Distance;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;

/**
 * Created by andreas on 3/22/17.
 */
public class PetrinetLSMiner extends LocalSearchMiner<PetrinetGraph> {

    public PetrinetLSMiner(DistanceFunction function, PluginContext context, XLog log, PetrinetGraph model) {
        super(function, context, log, model);
    }

    protected PNRepResult currentAlignment;

    @Override
    protected Distance computeDistance(XLog log, PetrinetGraph model) {
        currentAlignment = ((Pair<PNRepResult, PNManifestFlattener>) StochasticNetUtils.replayLog(context, model, log, false, true)).getFirst();


        return null;
    }

    @Override
    protected PetrinetGraph moveInModel(PetrinetGraph model, XLog log) {
        return null;
    }

    @Override
    protected XLog moveInLog(XLog log, PetrinetGraph model) {
        return null;
    }
}
