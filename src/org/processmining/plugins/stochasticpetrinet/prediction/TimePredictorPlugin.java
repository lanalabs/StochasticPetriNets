package org.processmining.plugins.stochasticpetrinet.prediction;

import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.Date;

/**
 * This is a simple time predictor based on a stochastic model.
 * Restrictive assumption: Trace is replayable directly by model to get to the current marking state.
 * (TODO: Leverage this by using a (stochastic) alignment of the observations to the model)
 * <p>
 * Working procedure:
 * - Go to current marking by simple replay in the model.
 * - Simulate a number of traces to see end result
 *
 * @author Andreas Rogge-Solti
 */
public class TimePredictorPlugin {

    @Plugin(name = "Predict duration by Simulation",
            parameterLabels = {"StochasticNet", "Trace", "Time"},
            returnLabels = {"Expected Duration"},
            returnTypes = {Double.class},
            userAccessible = true,
            help = "Predicts the remainding duration for a given trace and a model and time.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public Double predict(final UIPluginContext context, StochasticNet model, XTrace observedEvents, Date currentTime) {
        Marking initialMarking = StochasticNetUtils.getInitialMarking(context, model);
        TimePredictor predictor = new TimePredictor(false);
        return predictor.predict(model, observedEvents, currentTime, initialMarking).getFirst();
    }

    @Plugin(name = "Compute risk by Simulation",
            parameterLabels = {"StochasticNet", "Trace", "Current Time", "Target Time"},
            returnLabels = {"Expected Duration"},
            returnTypes = {Double.class},
            userAccessible = true,
            help = "Predicts the remainding duration for a given trace and a model and time.")
    @UITopiaVariant(affiliation = "Wirtschaftsuniversität Wien", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
    public Double computeRisk(final UIPluginContext context, StochasticNet model, XTrace observedEvents, Date currentTime, Date targetTime) {
        Marking initialMarking = StochasticNetUtils.getInitialMarking(context, model);
        TimePredictor predictor = new TimePredictor(false);
        return predictor.predict(model, observedEvents, currentTime, initialMarking).getFirst();
    }
}
