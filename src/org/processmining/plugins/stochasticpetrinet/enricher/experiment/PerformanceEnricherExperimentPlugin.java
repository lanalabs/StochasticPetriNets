package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricher;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

/**
 * Performs a round-trip: model -> log -> model
 * 1. generates a log for each execution semantics ({@link ExecutionPolicy}).
 * 2. strips the performance information of the stochastic net to get a plain model.
 * 3. uses logs and model to enrich Petri nets
 * 4. compare model parameters between each learned model and the original model. 
 * 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PerformanceEnricherExperimentPlugin {

	public static final int[] TRACE_SIZES = new int[]{10,30,100,300,1000,3000,10000};
	
	public static final int[] NOISE_LEVELS = new int[]{0,5,10,15,20,25,30,35,40,45,50};
	
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
		Marking initialMarking = StochasticNetUtils.getInitialMarking(context, net);
		
		return performExperiment(context, net, initialMarking);
	}

	/**
	 * We first simulate the net a number of times with a combination of given trace-sizes and policies, and then
	 * enrich the base Petri net to be stochastic again. Then we compare the resulting model parameters to each other. 
	 * @param context
	 * @param net
	 * @param initialMarking
	 * @return
	 */
	public PerformanceEnricherExperimentResult performExperiment(UIPluginContext context, StochasticNet net,
			Marking initialMarking) {
		PerformanceEnricherExperimentResult result = new PerformanceEnricherExperimentResult();

		PNSimulator simulator = new PNSimulator();
		// construct a plain copy of the net:
		Petrinet plainNet = StochasticNetUtils.getPlainNet(context, net);
		
		for (int traceSize : TRACE_SIZES){
			for (ExecutionPolicy policy : ExecutionPolicy.values()){
				PNSimulatorConfig config = new PNSimulatorConfig(traceSize,UNIT_FACTOR,0,1,1000,policy);
				XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, initialMarking);
				
				PerformanceEnricher enricher = new PerformanceEnricher();
				Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, plainNet, log, true, true);
				PerformanceEnricherConfig mineConfig = new PerformanceEnricherConfig(DistributionType.GAUSSIAN_KERNEL, (double) UNIT_FACTOR, policy);
				Object[] enrichedNet = enricher.transform(context, manifest, mineConfig);
				StochasticNet learnedNet = (StochasticNet) enrichedNet[0];
				result.add(traceSize, policy, net, learnedNet);
			}
		}
		System.out.println("-----------------------------");
		System.out.println(result.getResultsCSV());
		System.out.println("-----------------------------");
		return result;
	}
}
