package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.plugins.filter.context.LoadAnnotationPlugin;
import org.processmining.plugins.filter.loops.LoopsLinearizerPlugin;
import org.processmining.plugins.filter.noise.AccurateNoiseFilter;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class FilterTest {

	@Test
	public void testLoopsLinearizer() throws Exception {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		
		TestUtils.addEvent("a",trace);
		TestUtils.addEvent("b",trace);
		TestUtils.addEvent("c",trace);
		TestUtils.addEvent("b",trace);
		TestUtils.addEvent("d",trace);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("a",trace2);
		TestUtils.addEvent("b",trace2);
		TestUtils.addEvent("c",trace2);
		TestUtils.addEvent("a",trace2);
		TestUtils.addEvent("d",trace2);
		
		log.add(trace);
		log.add(trace2);
		
		LoopsLinearizerPlugin linearizer = new LoopsLinearizerPlugin();
		XLog log2 = linearizer.unrollHeadless(null, log);
		
		Assert.assertEquals("a, b, c, b, d", StochasticNetUtils.debugTrace(log.get(0)));
		Assert.assertEquals("a, b, c, b_2, d", StochasticNetUtils.debugTrace(log2.get(0)));
		
		Assert.assertEquals("a, b, c, a, d", StochasticNetUtils.debugTrace(log.get(1)));
		Assert.assertEquals("a, b, c, a_2, d", StochasticNetUtils.debugTrace(log2.get(1)));
	}

	@Test
	public void testContextAnnotator(){
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
		
		XTrace annotatedTrace = annotatedLog.get(0);
		XTrace annotatedTrace2 = annotatedLog.get(1);
		XTrace annotatedTrace3 = annotatedLog.get(2);
		
		Assert.assertEquals(1, ((XAttributeDiscrete)annotatedTrace.get(0).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(2, ((XAttributeDiscrete)annotatedTrace2.get(0).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(3, ((XAttributeDiscrete)annotatedTrace3.get(0).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		
		Assert.assertEquals(3, ((XAttributeDiscrete)annotatedTrace.get(1).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(3, ((XAttributeDiscrete)annotatedTrace2.get(1).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(3, ((XAttributeDiscrete)annotatedTrace3.get(1).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		
		Assert.assertEquals(3, ((XAttributeDiscrete)annotatedTrace.get(2).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(2, ((XAttributeDiscrete)annotatedTrace2.get(2).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
		Assert.assertEquals(1, ((XAttributeDiscrete)annotatedTrace3.get(2).getAttributes().get(LoadAnnotationPlugin.CONTEXT_LOAD)).getValue());
	}
	
	@Test
	public void testNoiseFilter() throws Exception {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		log.add(trace);
		final int MINUTES = 60000;
		TestUtils.addEvent("a",trace, 10*MINUTES);
		TestUtils.addEvent("b",trace, 20*MINUTES);
		TestUtils.addEvent("c",trace, 30*MINUTES);
		TestUtils.addEvent("d",trace, 40*MINUTES);
		TestUtils.addEvent("e",trace, 50*MINUTES);
		TestUtils.addEvent("f",trace, 60*MINUTES);
		TestUtils.addEvent("g",trace, 70*MINUTES);
		TestUtils.addEvent("h",trace, 80*MINUTES);
		TestUtils.addEvent("i",trace, 90*MINUTES);
		TestUtils.addEvent("j",trace, 100*MINUTES);
		
		System.out.println("Inserting noise. Original trace:\n"
				+ StochasticNetUtils.debugTrace(trace));
		
		// remove only
		AccurateNoiseFilter filter = new AccurateNoiseFilter(0.0, 1, 1);
		for (int noisePercent = 5; noisePercent < 100; noisePercent+= 5){
			filter.setNoise(noisePercent/100.);
			XLog noisyLog = filter.insertNoise(log);
			XTrace noisyTrace = noisyLog.get(0);
			System.out.println("inserted "+noisePercent+" percent noise. Noisy trace:\n"
					+ StochasticNetUtils.debugTrace(noisyTrace));
			Assert.assertEquals((100-noisePercent)/100., noisyTrace.size()/(double)trace.size(),0.06);	
		}
		
		// add only
		filter.setDeletionInsertionRatio(0.0);
		for (int noisePercent = 5; noisePercent < 100; noisePercent+= 5){
			filter.setNoise(noisePercent/100.);
			XLog noisyLog = filter.insertNoise(log);
			XTrace noisyTrace = noisyLog.get(0);
			System.out.println("inserted "+noisePercent+" percent noise. Noisy trace:\n"
					+ StochasticNetUtils.debugTrace(noisyTrace));
			Assert.assertEquals((100+noisePercent)/200., noisyTrace.size()/(2*(double)trace.size()),0.04);	
		}
		
		// remove half, add half
		filter.setDeletionInsertionRatio(0.5);
		for (int noisePercent = 0; noisePercent <= 100; noisePercent+= 5){
			filter.setNoise(noisePercent/100.);
			XLog noisyLog = filter.insertNoise(log);
			XTrace noisyTrace = noisyLog.get(0);
			System.out.println("inserted "+noisePercent+" percent noise. Noisy trace:\n"
					+ StochasticNetUtils.debugTrace(noisyTrace));
			Assert.assertEquals(0.5, noisyTrace.size()/(2*(double)trace.size()),0.06);	
		}
	}
}
