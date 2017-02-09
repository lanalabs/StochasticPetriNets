package org.processmining.plugins.stochasticpetrinet.analyzer.anomaly;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;

/**
 * We shift the distribution downwards so that the outliers are below zero.
 * Used to calculate intervals of outliers numerically by Illinois-type approximations.
 *
 * @author Andreas Rogge-Solti
 */
public class ShiftedDistribution implements UnivariateFunction {

    private double shift;
    private RealDistribution dist;

    public ShiftedDistribution(RealDistribution dist, double shift) {
        this.dist = dist;
        this.shift = shift;
    }

    public double value(double x) {
        return Math.log(dist.density(x)) - shift;
    }
}