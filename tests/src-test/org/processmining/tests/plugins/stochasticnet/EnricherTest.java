package org.processmining.tests.plugins.stochasticnet;

import org.junit.Ignore;
import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin.ExperimentType;

public class EnricherTest {

	
	@Test
	@Ignore
	public void testExperiment() throws Exception {
		TestUtils.runExperimentAndSaveOutput(ExperimentType.TRACE_SIZE_EXPERIMENT, "Parallel_Loop_A-D");	
	}
	
	@Test
	@Ignore
	public void testEvaluation() throws Exception {
		TestUtils.runExperimentAndSaveOutput(ExperimentType.TRACE_SIZE_EXPERIMENT, "evaluation");		
	}



	@Test
	@Ignore
	public void testNoisyEvaluation() throws Exception {
		TestUtils.runExperimentAndSaveOutput(ExperimentType.NOISE_LEVEL_EXPERIMENT, "evaluation");
	}
	
	/**
	 * A small experiment for quick debugging (small statespace -> without loop) 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testSmallExperiment() throws Exception {
		TestUtils.runExperimentAndSaveOutput(ExperimentType.TRACE_SIZE_EXPERIMENT, "Only_A");	
	}
}
