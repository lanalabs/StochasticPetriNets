package org.processmining.plugins.stochasticpetrinet.distribution.timeseries;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.AnotherAbstractRealDistribution;

/**
 * This is a "stateful" distribution that knows it's current time.
 * Depending on its time, it has a value which is determined by the subclass (e.g. {@link SinusoidalSeries})
 * <p>
 * BEWARE: the class creating samples needs to update the current state to the sampling timestamp!
 *
 * @author Andreas Rogge-Solti
 */
public abstract class StatefulTimeseriesDistribution extends AnotherAbstractRealDistribution {
    private static final long serialVersionUID = 4176351496287089192L;


    long currentTime;

    protected RealDistribution noiseDistribution;

    protected boolean onlyPositive = true;

    public StatefulTimeseriesDistribution(double noiseStandardDeviation) {
        this.currentTime = 0;
        this.noiseDistribution = new NormalDistribution(0, noiseStandardDeviation);
    }

    public StatefulTimeseriesDistribution(RealDistribution noiseDist) {
        this.currentTime = 0;
        this.noiseDistribution = noiseDist;
    }

    public double density(double x) {
        return 0;
    }

    /**
     * Sub-classes need to implement this.
     * It returns the value of the time series model given the current time
     *
     * @param currentTime the current time as a POSIX timestamp (millis since 01-01-1970)
     * @return double the value of the underlying series at the given point in time.
     */
    protected abstract double getCurrentSeriesValue(long currentTime);

    public double getSupportLowerBound() {
        if (Double.isInfinite(noiseDistribution.getSupportLowerBound())) {
            return noiseDistribution.getSupportLowerBound();
        }
        return getCurrentSeriesValue(currentTime) - noiseDistribution.getSupportLowerBound();
    }

    public double getSupportUpperBound() {
        if (Double.isInfinite(noiseDistribution.getSupportUpperBound())) {
            return noiseDistribution.getSupportUpperBound();
        }
        return getCurrentSeriesValue(currentTime) + noiseDistribution.getSupportUpperBound();
    }

    @SuppressWarnings("deprecation")
    public boolean isSupportLowerBoundInclusive() {
        return noiseDistribution.isSupportLowerBoundInclusive();
    }

    @SuppressWarnings("deprecation")
    public boolean isSupportUpperBoundInclusive() {
        return noiseDistribution.isSupportUpperBoundInclusive();
    }

    public boolean isSupportConnected() {
        return noiseDistribution.isSupportConnected();
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public double sample() {
        return getCurrentSeriesValue(currentTime) + noiseDistribution.sample();
    }

    public double sample(long time) {
        return getCurrentSeriesValue(time) + noiseDistribution.sample();
    }

    public double cumulativeProbability(double x) {
        return noiseDistribution.cumulativeProbability(x - getCurrentSeriesValue(currentTime));
    }

}
