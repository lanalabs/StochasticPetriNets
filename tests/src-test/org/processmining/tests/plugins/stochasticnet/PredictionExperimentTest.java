package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentConfig;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentPlugin;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class PredictionExperimentTest {

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
		config.setTimeUnitFactor(StochasticNetUtils.UNIT_CONVERSION_FACTORS[2]);
		config.setWorkerCount(4);
		
		// generate some traces
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(config.getSimulatedTraces(), config.getTimeUnitFactor());
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, model);
		Semantics<Marking,Transition> semantics = StochasticNetUtils.getSemantics(model);
		
		XLog simulatedLog = simulator.simulate(null, model, semantics, simConfig, initialMarking); 
		
		experimentPlugin.predictWithConfig(TestUtils.getDummyConsoleProgressContext(), model, simulatedLog, config);
	}
}
