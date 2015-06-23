package org.processmining.tests.plugins.stochasticnet.distribution;

import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.SimpleHistogramDistribution;

public class HistorgramTest {

	@Test
	public void testSampling() throws Exception {
		SimpleHistogramDistribution dist = new SimpleHistogramDistribution();
		
		NormalDistribution nDist = new NormalDistribution(10, 2);
		
		double[] samples = nDist.sample(20000);
		Arrays.sort(samples);
		
		dist.addValues(samples);
		
		for (int i = 0; i < 1000000000; i++){
			StochasticNetUtils.sampleWithConstraint(dist, "empty", nDist.sample());
		}
	}
}
