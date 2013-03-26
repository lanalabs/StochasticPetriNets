package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

public class TruncatedDistributionFactory {

	public static RealDistribution getConstrainedWrapper(RealDistribution dist){
		return getConstrainedWrapper(dist, 0);
	}

	public static RealDistribution getConstrainedWrapper(RealDistribution dist, double constraint) {
		if (dist instanceof ExponentialDistribution){
			return new MemorylessTruncatedWrapper(dist,constraint);
		} else {
			return new TruncatedWrapper(dist,constraint);
		}
	}
}
