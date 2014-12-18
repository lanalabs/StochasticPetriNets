package org.processmining.tests.plugins.stochasticnet.measure;

import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.stochasticpetrinet.measures.ComputedMeasures;
import org.processmining.plugins.stochasticpetrinet.measures.MeasurePlugin;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class EntropyTest {

	@Test
	public void testEntropySequence() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}

	@Test
	public void testEntropyLoopyFreeChoice() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("loopy_free_choice", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
}
