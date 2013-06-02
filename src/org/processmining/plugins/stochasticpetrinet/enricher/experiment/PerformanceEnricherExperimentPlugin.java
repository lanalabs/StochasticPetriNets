package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.impl.ToResetNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricher;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

/**
 * Performs a round-trip: model -> log -> model
 * 1. generates a log for each execution semantics ({@link ExecutionPolicy}).
 * 2. strips the performance information of the stochastic net to get a plain model.
 * 3. uses logs and model to enrich petri nets
 * 4. compare model parameters between each learned model and the original model. 
 * 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PerformanceEnricherExperimentPlugin {

	public static final int[] TRACE_SIZES = new int[]{10,100,1000,10000};
	
	/**
	 * Use minutes as unit in the model
	 */
	public static final int UNIT_FACTOR = 60000;
	
	
	
	@Plugin(name = "Enrich Petri Net (Simulated Experiment)", 
			parameterLabels = { "StochasticNet" }, 
			returnLabels = { "Enrichment quality" }, 
			returnTypes = { PerformanceEnricherExperimentResult.class }, 
			userAccessible = true,
			help = "Simulates a log given a stochastic model and then uses the underlying model to enrich it with information from the log.")
	@UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
	public PerformanceEnricherExperimentResult plugin(UIPluginContext context, StochasticNet net){
		PerformanceEnricherExperimentResult result = new PerformanceEnricherExperimentResult();
		PNSimulator simulator = new PNSimulator();
		Marking initialMarking = StochasticNetUtils.getInitialMarking(context, net);
		
		Petrinet plainNet;
		try {
			plainNet = (Petrinet) ToResetNet.fromPetrinet(context, net)[0];
		} catch (ConnectionCannotBeObtained e) {
			e.printStackTrace();
			context.getFutureResult(0).cancel(true);
			return null;
		}
		
		for (int traceSize : TRACE_SIZES){
			for (ExecutionPolicy policy : ExecutionPolicy.values()){
				PNSimulatorConfig config = new PNSimulatorConfig(traceSize,UNIT_FACTOR,0,1,1000,policy);
				XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, initialMarking);
				
				PerformanceEnricher enricher = new PerformanceEnricher();
				Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, plainNet, log, true);
				Object[] enrichedNet = enricher.transform(context, manifest);
				StochasticNet learnedNet = (StochasticNet) enrichedNet[0];
				result.add(traceSize, policy, net, learnedNet);
			}
		}		
		return result;
	}
	
}
