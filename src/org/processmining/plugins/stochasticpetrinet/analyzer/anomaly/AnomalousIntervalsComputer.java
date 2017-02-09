package org.processmining.plugins.stochasticpetrinet.analyzer.anomaly;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsAnalyzer;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnomalousIntervalsComputer {

    private static final double MAX_NUM_SCANS = 1000;
    private static final double EPSILON = 1e-12;


    public Map<Transition, List<Pair<Double, Double>>> getAnomalousIntervals(PluginContext context, StochasticNet net, double outlierRate) {

        try {
            CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(net, StochasticNetUtils.getInitialMarking(context, net), new CaseStatisticsList());
            analyzer.setOutlierRate(outlierRate);
            analyzer.updateLikelihoodCutoffs();

            Map<Transition, List<Pair<Double, Double>>> anomalousTransitionIntervals = new HashMap<>();

            for (Transition t : net.getTransitions()) {
                if (t instanceof TimedTransition) {
                    List<Pair<Double, Double>> anomalyList = new ArrayList<>();
                    TimedTransition tt = (TimedTransition) t;
                    if (tt.getDistributionType().equals(DistributionType.IMMEDIATE) || tt.getDistributionType().equals(DistributionType.DETERMINISTIC)) {
                        Double value = tt.getDistribution() == null ? 0.0 : tt.getDistribution().getNumericalMean();
                        Pair<Double, Double> anomalyBelow = new Pair<Double, Double>(Double.NEGATIVE_INFINITY, value - PerformanceEnricher.EPSILON);
                        Pair<Double, Double> anomaly = new Pair<Double, Double>(value + PerformanceEnricher.EPSILON, Double.POSITIVE_INFINITY);
                        anomalyList.add(anomalyBelow);
                        anomalyList.add(anomaly);
                    } else if (tt.getDistributionType().equals(DistributionType.UNIFORM)) {
                        // we will have no chance to find any outliers inside the domain of the uniform distribution. Its outliers are below the start and above the end.
                        UniformRealDistribution uniformDist = (UniformRealDistribution) tt.getDistribution();
                        anomalyList.add(new Pair<Double, Double>(Double.NEGATIVE_INFINITY, uniformDist.getSupportLowerBound()));
                        anomalyList.add(new Pair<Double, Double>(uniformDist.getSupportUpperBound(), Double.POSITIVE_INFINITY));
                    } else {
                        RealDistribution d = tt.getDistribution();
                        double threshold = analyzer.getLogLikelihoodCutoff(tt);
                        anomalyList = getAnomalousIntervalsForDistribution(d, threshold);
                    }
                    anomalousTransitionIntervals.put(t, anomalyList);
                }
            }
            return anomalousTransitionIntervals;
        } catch (IllegalArgumentException iae) {
            if (context != null) {
                context.getFutureResult(0).cancel(true);
            }
        }
        return null;
    }


    public List<Pair<Double, Double>> getAnomalousIntervalsForDistribution(RealDistribution d, double threshold) {
        List<Pair<Double, Double>> anomalousRegions = new ArrayList<>();
        ShiftedDistribution function = new ShiftedDistribution(d, threshold);
        anomalousRegions = findIntervalsBelowZero(function, d.inverseCumulativeProbability(0.00001), d.inverseCumulativeProbability(0.9999));
//		anomalousRegions = findIntervalsBelowZero(function, 0.0, d.inverseCumulativeProbability(0.9999));
        return anomalousRegions;
    }


    /**
     * The idea is to look for
     *
     * @param function   the function where we want to find
     * @param lowerBound
     * @param upperBound
     * @return
     */
    public List<Pair<Double, Double>> findIntervalsBelowZero(UnivariateFunction function, Double lowerBound, Double upperBound) {
        if (Double.isInfinite(function.value(lowerBound))) {
            // density is zero, log density is negative infinity.
            // it is an anomaly below
            lowerBound += EPSILON;
        }
        if (Double.isNaN(function.value(lowerBound))) {
            lowerBound += EPSILON;
        }
        if (Double.isNaN(function.value(upperBound))) {
            System.out.println("debug me!");
        }


        // use MAX_NUM_SCANS equidistant points between lower bound and upper bound as a limited scanning approach
        boolean lastObsWasPositive = false;

        double interval = (upperBound - lowerBound) / MAX_NUM_SCANS;

        Double lastNegativeValue = Double.NEGATIVE_INFINITY;

        List<Pair<Double, Double>> negativeIntervals = new ArrayList<>();

        double lastValue = lowerBound;

        for (int i = 0; i < MAX_NUM_SCANS; i++) {
            double x = lowerBound + (i * interval);
            boolean thisObsIsPositive = function.value(x) >= 0;
            if (lastObsWasPositive ^ thisObsIsPositive) { // change detected
                if (thisObsIsPositive) { // interval finished
                    double cut = findCutValue(function, Math.max(lastNegativeValue, lowerBound), x);
                    negativeIntervals.add(new Pair<Double, Double>(lastNegativeValue, cut));
                } else { // interval started
                    double cut = findCutValue(function, lastValue, x);
                    lastNegativeValue = cut;
                }
            }
            lastValue = x;
            lastObsWasPositive = thisObsIsPositive;
        }
        if (!lastObsWasPositive) {
            negativeIntervals.add(new Pair<Double, Double>(lastNegativeValue, Double.POSITIVE_INFINITY));
        }
        return negativeIntervals;
    }


    /**
     * Applies a recursive narrowing of the interval, until the precision is fine enough
     * to specify the root (the value for x, where f(x) = 0) accurately.
     *
     * @param function
     * @param max
     * @param x
     * @return
     */
    private double findCutValue(UnivariateFunction function, double r, double s) {
        if (r == 0 && s == 0) {
            return 0.0;
        }
        if (Double.isNaN(r) || Double.isNaN(s)) {
            System.out.println("Debug me!");
        }
        System.out.println("searching in [" + r + ";\t" + s + "]");
        double fs = function.value(s);
        double fr = function.value(r);

        double t = (s * fr - r * fs) / (fr - fs);
        if (s - r < EPSILON || Math.abs(s - t) < EPSILON || Math.abs(r - t) < EPSILON) {
            return t;
        } else {
            if (fs * function.value(t) < 0) {
                return findCutValue(function, t, s);
            } else {
                return findCutValue(function, r, t);
            }
        }
    }


}
