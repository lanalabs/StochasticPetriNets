package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.stochasticpetrinet.miner.distance.Distance;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;

public abstract class OptimalMiner<M> {

    protected final XLog origLog;
    protected final M origModel;

    protected final DistanceFunction function;

    protected Distance currentDistance;
    protected double bestDistance;

    protected XLog bestLog;
    protected M bestModel;

    protected XLog currentLog;
    protected M currentModel;
    protected PluginContext context;

    public OptimalMiner(DistanceFunction function, PluginContext context, XLog log, M model) {
        this.function = function;
        this.context = context;
        this.origLog = log;
        this.origModel = model;
        this.currentLog = log;
        this.currentModel = model;
        this.currentDistance = computeDistance(log, model);
        this.bestLog = log;
        this.bestModel = model;
        this.bestDistance = function.getFinalDistance(currentDistance);
    }

    /**
     * Computes the distance between a log and a model
     * Subclasses need to implement this according to their model type.
     * Ideally, this is a fast step.
     *
     * @param log the log to compare with the model
     * @param model the model to compare with the log
     * @return Distance
     */
    protected abstract Distance computeDistance(XLog log, M model);

    public XLog getBestLog() {
        return bestLog;
    }

    public M getBestModel() {
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
