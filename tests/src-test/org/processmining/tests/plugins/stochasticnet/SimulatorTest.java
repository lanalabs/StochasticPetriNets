package org.processmining.tests.plugins.stochasticnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.converter.ConvertDistributionsPlugin;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class SimulatorTest {

	@Test
	public void testFiringPolicyPreselection_vs_Race() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Race_AB",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];
		
		PNSimulator simulator = new PNSimulator();
		// do a simulation with global preselection:
		PNSimulatorConfig config = new PNSimulatorConfig(1000,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.GLOBAL_PRESELECTION);
		XLog logPreselection = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, marking);
		
		PNSimulatorConfig configRace = new PNSimulatorConfig(1000,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_ENABLING_MEMORY);
		XLog logRace = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configRace, marking);
		Map<String, Integer> counts = getEventCounts(logPreselection, "A","B");
		System.out.println("------- running testFiringPolicyPreselection_vs_Race() --------");
		System.out.println("Preselection policy: \t count A:"+counts.get("A")+",\t countB:"+counts.get("B")+ " (should be around 1:4, or 200 : 800)");
		// preselection is based on weights which are 1:4
		Assert.assertEquals(1.0/4, counts.get("A")/(double)counts.get("B"), 0.02);
		
		counts = getEventCounts(logRace, "A","B");
		System.out.println("Race policy: \t\t count A:"+counts.get("A")+",\t countB:"+counts.get("B")+ " (should be around 7:1, or 875 : 125)");
		// race is based on firing faster, which is 7:1, in the case of the two uniform intervals (1-3), and (2-4)
		Assert.assertEquals(1.0/7, counts.get("B")/(double)counts.get("A"), 0.03); 
	}
	
	@Test
	public void testFiringPolicyAgeMemory_vs_Resampling() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Race_AB_Loop", true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];
		
		PNSimulator simulator = new PNSimulator();
		// do a simulation with global preselection:
		PNSimulatorConfig configAgeMemory = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_AGE_MEMORY);
		XLog logAgeMemory = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configAgeMemory, marking);
		PNSimulatorConfig configEnablingMemory = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_ENABLING_MEMORY);
		XLog logEnablingMemory = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configEnablingMemory, marking);
		
		PNSimulatorConfig configResampling = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_RESAMPLING);
		XLog logResampling = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configResampling, marking);
		Map<String, Integer> countsAge = getEventCounts(logAgeMemory, "A","B");
		Map<String, Integer> countsEnabling = getEventCounts(logEnablingMemory, "A","B");
		Map<String, Integer> countsResampling = getEventCounts(logResampling, "A","B");
		System.out.println("------- running testFiringPolicyAgeMemory_vs_Resampling() --------");
		System.out.println("Race - resampling: \t count A:"+countsResampling.get("A")+",\t countB:"+countsResampling.get("B")+ " (B has no opportunity to fire)");
		Assert.assertTrue(countsResampling.get("B") == 0);
		System.out.println("Race - enabling: \t count A:"+countsEnabling.get("A")+",\t countB:"+countsEnabling.get("B")+ " (B has no opportunity to fire)");
		Assert.assertTrue(countsEnabling.get("B") == 0);

		
		System.out.println("Race - age policy: \t count A:"+countsAge.get("A")+",\t countB:"+countsAge.get("B")+ " (B can fire, but never in the first iteration!)");
		Assert.assertTrue(countsAge.get("B") > 0);
	}
	
	@Test
	public void testFiringPolicyEnablingMemory_vs_Resampling() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Race_A_parallelTo_B_Loop", true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];
		
		PNSimulator simulator = new PNSimulator();
		// do a simulation with global preselection:
		PNSimulatorConfig configAgeMemory = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_AGE_MEMORY);
		XLog logAgeMemory = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configAgeMemory, marking);
		PNSimulatorConfig configEnablingMemory = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_ENABLING_MEMORY);
		XLog logEnablingMemory = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configEnablingMemory, marking);
		PNSimulatorConfig configResampling = new PNSimulatorConfig(1,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_RESAMPLING);
		XLog logResampling = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), configResampling, marking);
		
		Map<String, Integer> countsAge = getEventCounts(logAgeMemory, "A","B");
		Map<String, Integer> countsEnabling = getEventCounts(logEnablingMemory, "A","B");
		Map<String, Integer> countsResampling = getEventCounts(logResampling, "A","B");
		System.out.println("------- running testFiringPolicyEnablingMemory_vs_Resampling() --------");
		System.out.println("Race - resampling: \t count A:"+countsResampling.get("A")+",\t countB:"+countsResampling.get("B")+ " (A has no opportunity to fire)");
		Assert.assertTrue(countsResampling.get("A") == 0);
		Assert.assertTrue(countsResampling.get("B") > 5);
		System.out.println("Race - enabling: \t count A:"+countsEnabling.get("A")+",\t countB:"+countsEnabling.get("B")+ " (A has opportunity to fire and should end process soon)");
		Assert.assertTrue(countsEnabling.get("A") == 1);
		Assert.assertTrue(countsEnabling.get("B") > 0);
		System.out.println("Race - age policy: \t count A:"+countsAge.get("A")+",\t countB:"+countsAge.get("B")+ " (should not differ much from enabling policy!)");
		Assert.assertTrue(countsAge.get("A") == 1);
		
	}
	
	@Test
	public void testSimulatingComplexModel() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Parallel_Loop_A-F",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];
		
		PNSimulator simulator = new PNSimulator();
		
		int traces = 1000;
		
		// do a simulation with global preselection:
		PNSimulatorConfig config = new PNSimulatorConfig(traces,TimeUnit.MINUTES,0,1,1000,ExecutionPolicy.RACE_ENABLING_MEMORY);
		XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, marking);
		
		Assert.assertNotNull(log);
		Assert.assertEquals(traces, log.size());
	}
	
	@Test
	public void testSimulatingComplexModelComplete() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Parallel_Loop_A-F",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];
		
		PNSimulator simulator = new PNSimulator();
		
		int traces = 1000;
		
		// do a simulation with global preselection:
		PNSimulatorConfig config = new PNSimulatorConfig(traces,TimeUnit.MINUTES,0,1,15,ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setDeterministicBoundedStateSpaceExploration(true);
		XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, marking, StochasticNetUtils.getFinalMarking(null, net));
		for (XTrace trace : log){
			System.out.println(StochasticNetUtils.debugTrace(trace));
		}
		
		Assert.assertNotNull(log);
		Assert.assertEquals(156, log.size());
		
	}
	
	@Test
	public void testLoopyCompleteModel() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("loopy_realistic", true);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition && ((TimedTransition) t).getDistributionType().equals(DistributionType.IMMEDIATE)){
				t.setInvisible(true);
			}
		}
		
		PNSimulator simulator = new PNSimulator();
		
		int traces = 1000;
		
		// do a simulation with global preselection:
		PNSimulatorConfig config = new PNSimulatorConfig(traces,TimeUnit.MINUTES,0,1,15,ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setDeterministicBoundedStateSpaceExploration(true);
		XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, initialMarking, StochasticNetUtils.getFinalMarking(null, net));
		for (XTrace trace : log){
			System.out.println(StochasticNetUtils.debugTrace(trace));
		}
		
		Assert.assertNotNull(log);
		Assert.assertEquals(96, log.size());
		
	}
	
	@Test
	public void testPNMLModel() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Diagramm_Vortrag_Signavio",true);
		StochasticNet net = ((StochasticNet) netAndMarking[0]);
		Marking marking = (Marking) netAndMarking[1];
			
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(1000,TimeUnit.MINUTES,0,1,1000);
		XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), simConfig, marking);
		
		assertEquals(1000, log.size());
		
		for(XTrace xTrace : log) {
			assertNotNull(xTrace);
			System.out.print(xTrace.size()+",");
		}
	}
	
	@Test
	public void testPNMLModelThomas() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Thomas_Beispiel",true);
		StochasticNet net = ((StochasticNet) netAndMarking[0]);
		Marking marking = (Marking) netAndMarking[1];
			
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(100,TimeUnit.MINUTES,0,1,100);
		XLog log = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), simConfig, marking);
		
		assertEquals(100, log.size());
		
		for(XTrace xTrace : log) {
			assertNotNull(xTrace);
			System.out.print(xTrace.size()+",");
			Assert.assertEquals(10, xTrace.size());
		}
		System.out.println(" ");
		
		Object[] strippedNetAndMarking = ConvertDistributionsPlugin.stripStochasticInformation(null, net);
		Petrinet strippedNet = (Petrinet) strippedNetAndMarking[0];
		Marking strippedMarking = (Marking) strippedNetAndMarking[1];
		
		XLog simpleLog = simulator.simulate(null, strippedNet, new StochasticNetSemanticsImpl(), simConfig, strippedMarking);
		assertEquals(100, simpleLog.size());
		
		for(XTrace xTrace : simpleLog) {
			assertNotNull(xTrace);
			System.out.print(xTrace.size()+",");
		}
	}
	
	

	private Map<String, Integer> getEventCounts(XLog logPreselection, String... names) {
		Map<String,Integer> counts = new HashMap<String, Integer>();
		for (String name : names){
			counts.put(name, 0);
		}
		for (XTrace trace : logPreselection){
			for (XEvent event : trace){
				for (String name : names){
					if (XConceptExtension.instance().extractName(event).equals(name)){
						counts.put(name, counts.get(name)+1);
					}
				}
			}
		}
		return counts;
	}


	

}
