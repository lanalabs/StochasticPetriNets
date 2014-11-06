package org.processmining.tests.plugins.stochasticnet;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.stochasticpetrinet.analyzer.anomaly.AnomalousIntervalsComputer;
import org.processmining.plugins.stochasticpetrinet.analyzer.anomaly.AnomalousIntervalsComputerPlugin;

public class AnomalyIntervalTest {

	@Test
	public void testAnomaliesNormal() throws Exception {
		AnomalousIntervalsComputer anomalyComputer = new AnomalousIntervalsComputer();
		
		RealDistribution dist = new NormalDistribution(5, 1);
		
		List<Pair<Double,Double>> anomalylists = anomalyComputer.getAnomalousIntervalsForDistribution(dist, Math.log(dist.density(2)));
		
		printAnomaliesToConsole(anomalylists);
	}

	private void printAnomaliesToConsole(List<Pair<Double, Double>> anomalylists) {
		for (Pair<Double,Double> anomalyInterval : anomalylists){
			System.out.println("Anomaly from "+ anomalyInterval.getFirst()+" to "+ anomalyInterval.getSecond());
		}
	}
	
	@Test
	public void testAnomaliesInNet() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		
		AnomalousIntervalsComputer anomalyComputer = new AnomalousIntervalsComputer();
		
		Map<Transition, List<Pair<Double,Double>>> anomalylists = anomalyComputer.getAnomalousIntervals(null, net, 0.1);
		
		for (Transition t : anomalylists.keySet()){
			List<Pair<Double,Double>> anomalies = anomalylists.get(t);
			System.out.println("\nAnomalies of "+t.getLabel()+":");
			printAnomaliesToConsole(anomalies);
		}
	}
	
	@Test
	public void testAnomaliesInNetJSON() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		
		Double anomalyRate = 0.1;
		
		AnomalousIntervalsComputer anomalyComputer = new AnomalousIntervalsComputer();
		
		Map<Transition, List<Pair<Double,Double>>> anomalylists = anomalyComputer.getAnomalousIntervals(null, net, anomalyRate);
		
		AnomalousIntervalsComputerPlugin computer = new AnomalousIntervalsComputerPlugin();
		System.out.println(computer.getJSONForAnomalies(anomalylists, net.getLabel(), anomalyRate));
	}
	
}
