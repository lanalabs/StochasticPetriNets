package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;

public class DistributionUtils {

	/**
	 * Used for numerical integration.
	 * given a distribution with a density, it returns a function of x * density(x)
	 * @param dist
	 * @return
	 */
	public static UnivariateFunction getWeightedFunction(final RealDistribution dist){
		UnivariateFunction function = new UnivariateFunction() {
			public double value(double x) {
				return x*(dist.density(x));
			}
		};
		return function;
	}
}
