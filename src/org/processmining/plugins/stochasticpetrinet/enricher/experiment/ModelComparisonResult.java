package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.LinkedList;
import java.util.List;

public class ModelComparisonResult {

    DescriptiveStatistics weightDifferences;
    DescriptiveStatistics firstMomentDifferences;
    double meanTraceFitness;

    public ModelComparisonResult() {
        weightDifferences = new DescriptiveStatistics();
        firstMomentDifferences = new DescriptiveStatistics();
    }

    /**
     * Averages out multiple {@link ModelComparisonResult}s
     *
     * @param results
     * @return
     */
    public static ModelComparisonResult getAverage(ModelComparisonResult... results) {
        ModelComparisonResult average = new ModelComparisonResult();
        DescriptiveStatistics averageFitness = new DescriptiveStatistics();
        for (ModelComparisonResult r : results) {
            if (Double.isNaN(r.firstMomentDifferences.getMean())) {
                double[] values = stripNaNs(r.firstMomentDifferences.getValues());
                DescriptiveStatistics stats = new DescriptiveStatistics(values);
                average.firstMomentDifferences.addValue(stats.getMean());
            } else {
                average.firstMomentDifferences.addValue(r.firstMomentDifferences.getMean());
            }
            if (Double.isNaN(r.weightDifferences.getMean())) {
                double[] values = stripNaNs(r.weightDifferences.getValues());
                DescriptiveStatistics stats = new DescriptiveStatistics(values);
                average.weightDifferences.addValue(stats.getMean());
            } else {
                average.weightDifferences.addValue(r.weightDifferences.getMean());
            }
            averageFitness.addValue(r.meanTraceFitness);
        }
        average.meanTraceFitness = averageFitness.getMean();
        return average;
    }

    private static double[] stripNaNs(double[] values) {
        List<Double> nonNaNs = new LinkedList<Double>();
        for (double val : values) {
            if (!Double.isNaN(val)) {
                nonNaNs.add(val);
            }
        }
        double[] newVals = new double[nonNaNs.size()];
        int i = 0;
        for (Double d : nonNaNs) {
            newVals[i++] = d;
        }
        return newVals;
    }
}