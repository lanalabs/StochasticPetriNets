package org.processmining.plugins.stochasticpetrinet.prediction;

import java.util.Date;

import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * This is a simple time predictor based on a stochastic model.
 * Restrictive assumption: Trace is replayable directly by model to get to the current marking state.
 * (TODO: Leverage this by using a (stochastic) alignment of the observations to the model)
 * 
 * Working procedure: 
 * - Go to current marking by simple replay in the model.
 * - Simulate a number of traces to see end result
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class TimePredictorPlugin {
	
	@Plugin(name = "Predict duration by Simulation", 
			parameterLabels = { "StochasticNet", "Trace", "Time" }, 
			returnLabels = { "Expected Duration" }, 
			returnTypes = { Double.class }, 
			userAccessible = true,
			help = "Predicts the remainding duration for a given trace and a model and time.")
	@UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
	public Double predict(final UIPluginContext context, StochasticNet model, XTrace observedEvents, Date currentTime){
		Marking initialMarking = StochasticNetUtils.getInitialMarking(context, model);
		TimePredictor predictor = new TimePredictor();
		return predictor.predict(model, observedEvents, currentTime, initialMarking, false).getFirst();	
	}
}
