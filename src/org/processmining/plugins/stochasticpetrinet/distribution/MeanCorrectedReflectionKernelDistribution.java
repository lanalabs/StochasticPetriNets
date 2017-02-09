package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class MeanCorrectedReflectionKernelDistribution extends GaussianReflectionKernelDistribution {
    private static final long serialVersionUID = 1376651647162154117L;

    protected double meanValue;
    protected double scale = 1;
    private static final double tolerance = 0.1;

    public MeanCorrectedReflectionKernelDistribution() {
        super();
    }

    public MeanCorrectedReflectionKernelDistribution(double threshold) {
        super(threshold);
    }

    public MeanCorrectedReflectionKernelDistribution(double threshold, double precision) {
        super(threshold, precision);
    }

    protected void updateKernels() {
        super.updateKernels();
        DescriptiveStatistics stats = new DescriptiveStatistics(getDoubleArray(sampleValues));
        meanValue = stats.getMean();
        correctKernelWeights();
    }

    private void correctKernelWeights() {
        double error = getMeanError();
        double oldError = error;
        double scaleAdjustment = 0.1;
        while (Math.abs(error) > tolerance) {
            if (error < 0) { // need to decrease slope
                scaleAdjustment = -scaleAdjustment;
            }
            scale += scaleAdjustment;
            oldError = error;
            error = getMeanError();
            if (Math.abs(error) > Math.abs(oldError)) {
                // too big change!
                // restore slope and try again with smaller change:
                scale -= scaleAdjustment;
                scaleAdjustment = scaleAdjustment / 2.0;
            } else {
                // error reduced successfully:
            }
            if (Math.abs(oldError) > 2 * Math.abs(error)) {
                scaleAdjustment = scaleAdjustment / 2.0;
            }
        }
    }

    private double getMeanError() {
        double[] samples = sample(10000);
        DescriptiveStatistics stats = new DescriptiveStatistics(samples);
        return stats.getMean() - meanValue;
    }

    public double density(double x) {
        return scale * super.density(scale * x);
    }

    public double sample() {
        // TODO Auto-generated method stub
        return scale * super.sample();
    }


}
