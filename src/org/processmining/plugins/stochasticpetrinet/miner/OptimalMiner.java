package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.stochasticpetrinet.miner.distance.Distance;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;

public abstract class OptimalMiner {

    protected final XLog origLog;
    protected final PetrinetGraph origModel;

    protected final DistanceFunction function;

    protected Distance currentDistance;
    protected double bestDistance;

    protected XLog bestLog;
    protected PetrinetGraph bestModel;

    protected XLog currentLog;
    protected PetrinetGraph currentGraph;
    protected PluginContext context;

    public OptimalMiner(DistanceFunction function, PluginContext context, XLog log, PetrinetGraph model) {
        this.function = function;
        this.context = context;
        this.origLog = log;
        this.origModel = model;
        this.currentLog = log;
        this.currentGraph = model;
        this.currentDistance = computeDistance(log, model);
        this.bestLog = log;
        this.bestModel = model;
        this.bestDistance = function.getFinalDistance(currentDistance);
    }

    protected Distance computeDistance(XLog origLog2, PetrinetGraph origModel2) {
        return null;
    }

    public XLog getBestLog() {
        return bestLog;
    }

    public PetrinetGraph getBestModel() {
        return bestModel;
    }

    public void searchForBetterLogAndModel() {
        performSearch();
    }

    /**
     * Tries to find a log-model pair that is best in terms of a score
     */
    protected abstract void performSearch();


}
