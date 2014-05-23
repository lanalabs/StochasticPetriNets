package org.processmining.tests.plugins.stochasticnet.distribution;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.distribution.numeric.ConvolutionHelper;

public class ConvolutionTest {

	@Test
	public void testConvolutionOfTwoNormalDistributions(){
		NormalDistribution nDist1 = new NormalDistribution(50, 1);
		
		NormalDistribution nDist2 = new NormalDistribution(50, 1);
		
		RealDistribution convolvedDist = ConvolutionHelper.getConvolvedDistribution(nDist1, nDist2);
		
		Assert.assertEquals(nDist1.getMean()+nDist2.getMean(), convolvedDist.getNumericalMean(), 0.4);
	}
}
