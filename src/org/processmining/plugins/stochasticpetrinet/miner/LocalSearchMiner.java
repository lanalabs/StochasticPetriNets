package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.InductiveMiner.efficienttree.EfficientTree;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.distance.Distance;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;

public abstract class LocalSearchMiner<M> extends OptimalMiner<M> {

    protected static final int MAX_ITER = 10000;
    protected static final int MAX_MOVES_WITHOUT_IMPROVEMENT = 100;

    public LocalSearchMiner(DistanceFunction function, PluginContext context, XLog log, M model) {
        super(function, context, log, model);
        updateDistances(currentLog, currentModel);
    }

    /**
     * Updates the distances with the current log and model
     * @param log log
     * @param model model
     * @return whether the current log and model
     */
    protected final boolean updateDistances(XLog log, M model){
        boolean improved = false;

        Distance newDistance = computeDistance(log, model);
        double newDist = function.getFinalDistance(newDistance);
        if (newDist < bestDistance){
            improved = true;
            bestDistance = newDist;
            bestModel = model;
            bestLog = log;
        }
        return improved;
    }

    protected void performSearch() {

        int iter = 0;
        int movesWithoutImprovement = 0;

        while (iter < MAX_ITER && movesWithoutImprovement < MAX_MOVES_WITHOUT_IMPROVEMENT) {
            currentLog = moveInLog(currentLog, currentModel);
            movesWithoutImprovement++;

            if (updateDistances(currentLog, currentModel)) {
                movesWithoutImprovement = 0;
            }

            currentModel = moveInModel(currentModel, currentLog);
            movesWithoutImprovement++;

            if (updateDistances(currentLog, currentModel)) {
                movesWithoutImprovement = 0;
            }
        }
    }

    /**
     * we assume that the alignment guides us in changing the graph.
     */
    protected abstract M moveInModel(M model, XLog log);


    /**
     * We assume that the alignment
     * @return XLog adjusted to better fit the current
     */
    protected abstract XLog moveInLog(XLog log, M model);
}
