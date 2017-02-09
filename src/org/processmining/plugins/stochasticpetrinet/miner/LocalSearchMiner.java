package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;

public class LocalSearchMiner extends OptimalMiner {

    private static final int MAX_ITER = 10000;
    private static final int MAX_MOVES_WITHOUT_IMPROVEMENT = 100;

    public LocalSearchMiner(DistanceFunction function, PluginContext context, XLog log, PetrinetGraph model) {
        super(function, context, log, model);
        updateDistances(currentLog, currentGraph);
    }

    PNRepResult currentAlignment;

    protected boolean updateDistances(XLog log, PetrinetGraph model) {
        boolean improved = false;
        currentAlignment = ((Pair<PNRepResult, PNManifestFlattener>) StochasticNetUtils.replayLog(context, currentGraph, currentLog, false, true)).getFirst();

        currentDistance = computeDistance(log, model);
        double dist = function.getFinalDistance(currentDistance);
        if (dist < bestDistance) {
            improved = true;
            bestDistance = dist;
            bestModel = currentGraph;
            bestLog = currentLog;
        }
        return improved;
    }

    protected void performSearch() {

        int iter = 0;
        int movesWithoutImprovement = 0;

        while (iter < MAX_ITER && movesWithoutImprovement < MAX_MOVES_WITHOUT_IMPROVEMENT) {
            moveInLog();
            movesWithoutImprovement++;

            if (updateDistances(currentLog, currentGraph)) {
                movesWithoutImprovement = 0;
            }

            moveInGraph();
            movesWithoutImprovement++;

            if (updateDistances(currentLog, currentGraph)) {
                movesWithoutImprovement = 0;
            }
        }
    }

    private void moveInGraph() {
        // we assume that the alignment guides us in changing the graph.
//		currentGraph.getN
//		currentAlignment.

    }

    private XLog moveInLog() {
        // TODO Auto-generated method stub
        return null;
    }
}
