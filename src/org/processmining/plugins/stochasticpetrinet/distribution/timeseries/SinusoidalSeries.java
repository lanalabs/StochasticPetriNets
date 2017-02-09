package org.processmining.plugins.stochasticpetrinet.distribution.timeseries;

import org.apache.commons.math3.util.FastMath;

import java.util.concurrent.TimeUnit;

public class SinusoidalSeries extends StatefulTimeseriesDistribution {
    private static final long serialVersionUID = 1622318399256882698L;

    private double origin;
    private double period; // in days
    private double amplitude;

    public SinusoidalSeries(double amplitude) {
        this(amplitude, 1);
    }

    public SinusoidalSeries(double amplitude, double period) {
        this(amplitude, period, 0);
    }

    public SinusoidalSeries(double amplitude, double period, double origin) {
        this(amplitude, period, origin, 1);
    }

    /**
     * A sinusoidal sequence that obviously has seasonal behavior
     *
     * @param amplitude the amplitude of the sinus wave
     * @param period    the period of the season in days
     * @param origin
     */
    public SinusoidalSeries(double amplitude, double period, double origin, double noise) {
        super(noise);
        this.amplitude = amplitude;
        this.period = period;
        this.origin = origin;
    }


    protected double getCurrentSeriesValue(long currentTime) {
        return origin + amplitude * FastMath.sin((currentTime / period) / TimeUnit.DAYS.toMillis(1) * 2 * Math.PI);
    }

}
