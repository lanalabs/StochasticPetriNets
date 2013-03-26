package org.processmining.tests.plugins.stochasticnet;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.distribution.TruncatedDistributionFactory;

public class TruncatedDistributionTest {

	public static final int SAMPLESIZE = 10000;
	public static final double TOLERANCE = 0.2;
	
	@Test
	public void testExponential() throws Exception {
		
		double constraint = 1000;
		
		RealDistribution dist = new ExponentialDistribution(5);
		DescriptiveStatistics originalStats = new DescriptiveStatistics(dist.sample(SAMPLESIZE));
		
		RealDistribution truncatedDist = TruncatedDistributionFactory.getConstrainedWrapper(dist,constraint);
		DescriptiveStatistics truncatedStats = new DescriptiveStatistics(truncatedDist.sample(SAMPLESIZE));
		
		System.out.println("original mean: "+originalStats.getMean()+",\t truncated mean: "+truncatedStats.getMean()+" (constr.: "+constraint+")");
		double absDifference = Math.abs((truncatedStats.getMean()-constraint)-originalStats.getMean());
		System.out.println("Absolute difference: "+absDifference);
		Assert.assertTrue(absDifference<TOLERANCE);
	}
	
	@Test
	public void testNormal() throws Exception {
		double mean = 10, sd = 2;
		RealDistribution dist = new NormalDistribution(mean, sd);
		DescriptiveStatistics originalStats = new DescriptiveStatistics(dist.sample(SAMPLESIZE));
		
		RealDistribution truncatedDist = TruncatedDistributionFactory.getConstrainedWrapper(dist, mean);
		DescriptiveStatistics truncatedStats = new DescriptiveStatistics(truncatedDist.sample(SAMPLESIZE));
		
		System.out.println("difference: " + (truncatedStats.getMean() - originalStats.getMean()));
		System.out.println("numerical mean: "+truncatedDist.getNumericalMean());
		Assert.assertTrue(Math.abs(truncatedStats.getMean()-truncatedDist.getNumericalMean())<TOLERANCE);
	}
}
