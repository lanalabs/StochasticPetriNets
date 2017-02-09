package org.processmining.plugins.stochasticpetrinet.distribution.numeric;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.plugins.stochasticpetrinet.analyzer.ReplayStep;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ReplayStepDensityFunction implements UnivariateFunction {

    private ReplayStep step;
    private Map<Double, RealDistribution> durationsAndDistributions;

    public ReplayStepDensityFunction(ReplayStep step) {
        this.step = step;
        durationsAndDistributions = new HashMap<Double, RealDistribution>();
        for (ReplayStep childStep : step.children) {
            if (childStep.transition.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                System.out.println("debug");
            }
            durationsAndDistributions.put(childStep.duration, childStep.transition.getDistribution());
        }
    }

    public double value(double x) {
        double densityOfX = step.transition.getDistribution().density(x);
        for (Entry<Double, RealDistribution> child : durationsAndDistributions.entrySet()) {
            densityOfX *= child.getValue().density(step.duration + child.getKey() - x);
        }
        return densityOfX;
    }
}