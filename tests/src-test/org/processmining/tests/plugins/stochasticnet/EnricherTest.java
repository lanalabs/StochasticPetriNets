package org.processmining.tests.plugins.stochasticnet;

import java.util.Iterator;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.filter.context.LoadAnnotationPlugin;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherPlugin;
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
		for (int i = 0; i < 20; i++){
			TestUtils.runExperimentAndSaveOutput(ExperimentType.NOISE_LEVEL_EXPERIMENT, "evaluation");
		}
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
	
	@Test
	public void testEnricherLoad() {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		
		TestUtils.addEvent("a",trace, 10);
		TestUtils.addEvent("b",trace, 21);
		TestUtils.addEvent("c",trace, 50);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("a",trace2, 15);
		TestUtils.addEvent("b",trace2, 25);
		TestUtils.addEvent("c",trace2, 55);
		
		XTrace trace3 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("a",trace3, 20);
		TestUtils.addEvent("b",trace3, 30);
		TestUtils.addEvent("c",trace3, 60);
		
		log.add(trace);
		log.add(trace2);
		log.add(trace3);
		
		// assume lexical ordering??
		LoadAnnotationPlugin loadAnnotation = new LoadAnnotationPlugin();
		XLog annotatedLog = loadAnnotation.addNumberOfInstancesInSystemToLogHeadless(null, log);
		
		
		Petrinet net = new PetrinetImpl("test");
		Place pStart = net.addPlace("pStart");
		Transition tA = net.addTransition("a");
		net.addArc(pStart, tA);
		Place p1 = net.addPlace("p1");
		net.addArc(tA,p1);
		Transition tB = net.addTransition("b");
		net.addArc(p1,tB);
		Place p2 = net.addPlace("p2");
		net.addArc(tB, p2);
		Transition tC = net.addTransition("c");
		net.addArc(p2,tC);
		Place pEnd = net.addPlace("pEnd");
		net.addArc(tC,pEnd);
		Marking initialMarking = new Marking();
		initialMarking.add(pStart);

		Manifest manifest = (Manifest)StochasticNetUtils.replayLog(null, net, annotatedLog, true, true);

		PerformanceEnricherConfig enricherConfig = new PerformanceEnricherConfig(DistributionType.GAUSSIAN_KERNEL, TimeUnit.MILLISECONDS, ExecutionPolicy.RACE_ENABLING_MEMORY, null, true);
		Object[] netAndMarking = PerformanceEnricherPlugin.transform(null, manifest, enricherConfig);
		
		StochasticNet enrichedNet = (StochasticNet) netAndMarking[0];
		Iterator<Transition> iter = enrichedNet.getTransitions().iterator();
		iter.next();
		System.out.println(((TimedTransition)iter.next()).getTrainingData());
	}
}
