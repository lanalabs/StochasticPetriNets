package org.processmining.tests.plugins.stochasticnet.distribution;

import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.distribution.DistributionUtils;

public class DistributionUtilTest {

	@Test
	public void testNormalization() throws Exception {
		int mean = 10, sd = 1;
		RealDistribution realDist = new NormalDistribution(mean, sd);
		
		Map<Integer, Double> discreteDist = DistributionUtils.discretizeDistribution(realDist, 1);
//		for (Integer key : discreteDist.keySet()){
//			System.out.println("Bin "+key+" -> "+discreteDist.get(key));
//		}
		double twoSigma = 0;
		double threeSigma = 0;
		for (int i = 0; i <= 20; i++){
			threeSigma += (Math.abs(mean - i) <=3)?discreteDist.get(i):0;
			twoSigma += (Math.abs(mean - i) <=2)?discreteDist.get(i):0;
		}
//		System.out.println(threeSigma);
//		System.out.println(twoSigma);
		Assert.assertTrue(threeSigma > 0.997 && threeSigma < 1);
		Assert.assertTrue(twoSigma > 0.95 && twoSigma < 0.99);
		
	}
}
