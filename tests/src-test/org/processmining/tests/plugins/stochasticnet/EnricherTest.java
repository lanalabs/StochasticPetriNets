package org.processmining.tests.plugins.stochasticnet;

import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin;

public class EnricherTest {

	/**
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testLearnWeights() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Parallel_Loop_A-D");
	}
	
	@Test
	@Ignore
	public void testExperiment() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Parallel_Loop_A-D");
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);
		
		PerformanceEnricherExperimentPlugin enrichmentPlugin = new PerformanceEnricherExperimentPlugin();
		enrichmentPlugin.performExperiment(null, net, initialMarking);
	}
	
	@Test
	@Ignore
	public void testEvaluation() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("evaluation");
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);
		
		PerformanceEnricherExperimentPlugin enrichmentPlugin = new PerformanceEnricherExperimentPlugin();
		enrichmentPlugin.performExperiment(null, net, initialMarking);
	}
	
	/**
	 * A small experiment for quick debugging (small statespace -> without loop) 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testSmallExperiment() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Only_A");
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);
		
		PerformanceEnricherExperimentPlugin enrichmentPlugin = new PerformanceEnricherExperimentPlugin();
		enrichmentPlugin.performExperiment(null, net, initialMarking);
	}
}
