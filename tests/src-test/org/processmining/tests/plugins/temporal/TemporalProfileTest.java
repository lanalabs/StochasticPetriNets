package org.processmining.tests.plugins.temporal;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;
import org.processmining.plugins.temporal.miner.TemporalProfile;
import org.processmining.plugins.temporal.miner.TemporalProfileOptimizer;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class TemporalProfileTest {

	@Test
	public void testProfileGenerator() throws Exception {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		XConceptExtension.instance().assignName(trace, "tr1");
		TestUtils.addEvent("A", trace, 7);
		TestUtils.addEvent("B", trace, 10);
		TestUtils.addEvent("C", trace, 12);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		XConceptExtension.instance().assignName(trace2, "tr2");
		TestUtils.addEvent("A", trace2, 15);
		TestUtils.addEvent("C", trace2, 20);
		TestUtils.addEvent("B", trace2, 21);
		TestUtils.addEvent("D", trace2, 25);
		
		log.add(trace);
		log.add(trace2);
		
		TemporalProfile profile = new TemporalProfile(log);
		
		TemporalProfileOptimizer optimizer = new TemporalProfileOptimizer();
		optimizer.getLocallyOptimalModel(profile);
	}
}
