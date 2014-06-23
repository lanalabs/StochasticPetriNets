package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.plugins.filter.context.LoadAnnotationPlugin;
import org.processmining.plugins.filter.loops.LoopsLinearizerPlugin;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class FilterTest {

	@Test
	public void testLoopsLinearizer() throws Exception {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		
		addEvent("a",trace);
		addEvent("b",trace);
		addEvent("c",trace);
		addEvent("b",trace);
		addEvent("d",trace);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		addEvent("a",trace2);
		addEvent("b",trace2);
		addEvent("c",trace2);
		addEvent("a",trace2);
		addEvent("d",trace2);
		
		log.add(trace);
		log.add(trace2);
		
		LoopsLinearizerPlugin linearizer = new LoopsLinearizerPlugin();
		XLog log2 = linearizer.unrollHeadless(null, log);
		
		Assert.assertEquals("a, b, c, b, d", StochasticNetUtils.debugTrace(log.get(0)));
		Assert.assertEquals("a, b, c, b_2, d", StochasticNetUtils.debugTrace(log2.get(0)));
		
		Assert.assertEquals("a, b, c, a, d", StochasticNetUtils.debugTrace(log.get(1)));
		Assert.assertEquals("a, b, c, a_2, d", StochasticNetUtils.debugTrace(log2.get(1)));
	}

	private XEvent addEvent(String name, XTrace trace, long time){
		XEvent e = addEvent(name, trace);
		XTimeExtension.instance().assignTimestamp(e, time);
		return e;
	}
	
	private XEvent addEvent(String name, XTrace trace) {
		XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
		XConceptExtension.instance().assignName(e, name);
		trace.add(e);
		return e;
	}
	
	@Test
	public void testContextAnnotator(){
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		
		addEvent("a",trace, 10);
		addEvent("b",trace, 21);
		addEvent("c",trace, 50);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		addEvent("a",trace2, 15);
		addEvent("b",trace2, 25);
		addEvent("c",trace2, 55);
		
		XTrace trace3 = XFactoryRegistry.instance().currentDefault().createTrace();
		addEvent("a",trace3, 20);
		addEvent("b",trace3, 30);
		addEvent("c",trace3, 60);
		
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
}
