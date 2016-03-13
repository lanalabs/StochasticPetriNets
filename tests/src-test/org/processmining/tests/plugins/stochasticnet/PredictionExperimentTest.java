package org.processmining.tests.plugins.stochasticnet;


import org.deckfour.xes.model.XLog;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentConfig;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentPlugin;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class PredictionExperimentTest {

	@Test
	public void testBinarySearch(){
		double[] sortedValues = new double[]{1,2,3,4};
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 0) == 0);
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 1) == 0);
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 1.1) == 0);
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 2) == 1);
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 2.1) == 1);
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 3) == 2);
		
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 3.9) == 2);
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 4) == 3);
		
		Assert.assertTrue(StochasticNetUtils.getIndexBinarySearch(sortedValues, 4.1) == 3);
		
	}
	
//	@Test
	public void testExperimentParallel2() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		PredictionExperimentPlugin experimentPlugin = new PredictionExperimentPlugin();
		
		PredictionExperimentConfig config = new PredictionExperimentConfig();
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setLearnedDistributionType(DistributionType.HISTOGRAM);
		config.setLearnSPNFromData(true);
		config.setMonitoringIterations(40);
		config.setResultFileName("gspn_comparison_result_40_histogram.csv");
		config.setTimeUnitFactor(TimeUnit.MINUTES);
		config.setWorkerCount(4);
		
		// generate some traces
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(config.getSimulatedTraces(), config.getTimeUnitFactor());
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, model);
		Semantics<Marking,Transition> semantics = StochasticNetUtils.getSemantics(model);
		
		XLog simulatedLog = simulator.simulate(null, model, semantics, simConfig, initialMarking); 
		
		experimentPlugin.predictWithConfig(StochasticNetUtils.getDummyConsoleProgressContext(), model, simulatedLog, config);
	}
}
