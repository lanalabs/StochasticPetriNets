package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
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

	private void addEvent(String name, XTrace trace) {
		XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
		XConceptExtension.instance().assignName(e, name);
		trace.add(e);
	}
}
