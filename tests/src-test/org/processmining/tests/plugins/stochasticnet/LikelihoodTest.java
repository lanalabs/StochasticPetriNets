package org.processmining.tests.plugins.stochasticnet;

import java.io.File;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;
import org.processmining.plugins.stochasticpetrinet.analyzer.LikelihoodAnalyzer;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class LikelihoodTest {

	@Test
	@Ignore // needs lpsolve55 in library java.library.path
	public void testLikelihood() throws Exception{
		StochasticNet stochasticNet = loadNet();
		
		XLog log = createSampleLog(stochasticNet);
		
//		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, stochasticNet);
//		Marking finalMarking = StochasticNetUtils.getFinalMarking(null, stochasticNet);
		
		double logLikelihood = LikelihoodAnalyzer.getLogLikelihood(null, log, stochasticNet, 0);
		System.out.println("Loglikelihood: "+ logLikelihood);
		
		double logLikelihood2 = LikelihoodAnalyzer.getLogLikelihood(null, log, stochasticNet, 1);
		System.out.println("Loglikelihood: "+ logLikelihood2);
		Assert.assertEquals(logLikelihood2, logLikelihood+7, 0.01);
	}

	private StochasticNet loadNet() throws Exception {
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/simpleNetOneActivity_v0.2.pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);
		List<PNMLNet> nets = pnml.getNet();
		PNMLNet net = nets.get(0);
		
		String netId = net.getId();
		Assert.assertEquals("testNet", netId);
		
		
		StochasticNetDeserializer deserializer = new StochasticNetDeserializer();
		Object[] netAndMarking = deserializer.convertToNet(null, pnml, null, false);
		StochasticNet stochasticNet = (StochasticNet) netAndMarking[0];
		return stochasticNet;
	}
	
	@Test 
	@Ignore // needs lpsolve55 in library java.library.path 
	public void testLikelihoodJointReplay() throws Exception {
		StochasticNet stochasticNet = loadNet();
		XLog log = createSampleLog(stochasticNet);
		double logLikelihood = LikelihoodAnalyzer.getLogLikelihood(null, log, stochasticNet, 0);
		System.out.println("Loglikelihood: "+ logLikelihood);
		
		double logLikelihood2 = LikelihoodAnalyzer.getLogLikelihood(null, log, stochasticNet, 1);
		System.out.println("Loglikelihood: "+ logLikelihood2);
		
		CaseStatisticsList logLikelihoods = LikelihoodAnalyzer.getLogLikelihoods(null, log, stochasticNet);
		System.out.println("Loglikelihood: "+ logLikelihoods.get(0));
		System.out.println("Loglikelihood: "+ logLikelihoods.get(1));		
		
		Assert.assertEquals(logLikelihood, logLikelihoods.get(0).getLogLikelihood(), 0.0001);
		Assert.assertEquals(logLikelihood2, logLikelihoods.get(1).getLogLikelihood(), 0.0001);
	}

	private XLog createSampleLog(StochasticNet stochasticNet) {
		String instance = "1";
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		trace.add(createEvent("startEvent", instance, new Date(0)));
		trace.add(createEvent("A", instance, new Date((long)(1*stochasticNet.getTimeUnit().getUnitFactorToMillis()))));

		instance = "2";
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		trace2.add(createEvent("startEvent", instance, new Date(0)));
		trace2.add(createEvent("A", instance, new Date((long)(5*stochasticNet.getTimeUnit().getUnitFactorToMillis()))));
		
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		log.add(trace);
		log.add(trace2);
		return log;
	}

	private XEvent createEvent(String name, String instance, Date time) {
		XEvent event = XFactoryRegistry.instance().currentDefault().createEvent();
		XTimeExtension.instance().assignTimestamp(event, time);
		XConceptExtension.instance().assignName(event, name);
		XConceptExtension.instance().assignInstance(event, instance);
		XLifecycleExtension.instance().assignStandardTransition(event, StandardModel.COMPLETE);
		return event;
	}
}
