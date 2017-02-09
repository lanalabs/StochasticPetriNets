package org.processmining.plugins.stochasticpetrinet.distribution.timeseries;

import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ARMATimeSeries extends StatefulTimeseriesDistribution {
    private static final long serialVersionUID = 5268955755794729237L;


    protected LinkedList<Double> values;

    protected LinkedList<Double> errors;

    static Random rand = new Random();

    protected long lastTime = Long.MAX_VALUE;

    protected double[] arWeights;
    protected double[] maWeights;

    /**
     * order of autoregressive part
     */
    private int n;

    /**
     * order of moving average part
     */
    private int m;


    public ARMATimeSeries(double noiseStandardDeviation) {
        this(1, 1, noiseStandardDeviation);
    }

    public ARMATimeSeries(int n, int m, double noise) {
        super(new NormalDistribution(0, noise));
        this.n = n;
        this.m = m;
        this.values = new LinkedList<>();
        this.errors = new LinkedList<>();
        initWeightsRandomly();
    }

    private void initWeightsRandomly() {
        this.arWeights = new double[n];
        this.maWeights = new double[m];
        double sumArWeights = 0;
        double sumMaWeights = 0;
        for (int i = 0; i < n; i++) {
            arWeights[i] = (rand.nextDouble() - 0.5) / (1.5 * (i + 1));
            sumArWeights += arWeights[i];
        }
        for (int i = 0; i < m; i++) {
            maWeights[i] = (2 * rand.nextDouble() - 1) / (1.5 * (i + 1));
            sumMaWeights += maWeights[i];
        }
        if (sumArWeights > 0.99 || sumArWeights < -0.99) {
            for (int i = 0; i < n; i++) {
                arWeights[i] = arWeights[i] / (sumArWeights + 0.01);
            }
        }
        if (sumMaWeights > 0.99 || sumMaWeights < -0.99) {
            for (int i = 0; i < m; i++) {
                maWeights[i] = maWeights[i] / (sumMaWeights + 0.01);
            }
        }
    }

    protected double getCurrentSeriesValue(long currentTime) {
        if (lastTime == Long.MAX_VALUE) {
            lastTime = currentTime - Math.max(n, m) - 1;
            values.add(Math.abs(noiseDistribution.sample()));
            errors.add(values.get(0) / (1 + rand.nextDouble()));
        }
        return predict(currentTime);
    }

    private double predict(long currentTime) {
        if (lastTime < currentTime) {
            int h = (int) (currentTime - lastTime);
            for (int i = 1; i <= h; i++) {
                double newError = noiseDistribution.sample() * 5;
                double newVal = newError + getAR() + getMA();
                if (newVal < 0) newVal = -newVal;
                if (newError < 0) newError = -newError;

                errors.add(newError);
                values.add(newVal);
            }
            lastTime = currentTime;
            return values.getLast();
        } else {
            // we already know the value:
            return values.get((int) (values.size() - 1 - (lastTime - currentTime)));
        }

    }

    private double getAR() {
        Iterator<Double> iter = values.descendingIterator();
        int n = 0;
        double sum = 0;
        while (iter.hasNext() && n < this.n) {
            Double d = iter.next();
            sum += d * arWeights[n++];
        }
        return sum;
    }

    private double getMA() {
        Iterator<Double> iter = errors.descendingIterator();
        int m = 0;
        double sum = 0;
        while (iter.hasNext() && m < this.m) {
            Double d = iter.next();
            sum += d * maWeights[m++];
        }
        return sum;
    }

    public double[] getArWeights() {
        return arWeights;
    }

    public void setArWeights(double[] arWeights) {
        this.arWeights = arWeights;
    }

    public double[] getMaWeights() {
        return maWeights;
    }

    public void setMaWeights(double[] maWeights) {
        this.maWeights = maWeights;
    }

    public void addValue(double value, double error) {
        this.values.add(value);
        this.errors.add(error);
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    /* Only use hourly random walks */
    public void setCurrentTime(long currentTime) {
        super.setCurrentTime(TimeUnit.MILLISECONDS.toHours(currentTime));
    }

    public double[] getParameters() {
        double[] parameters = new double[4 + n + m + 2 * values.size()];
        parameters[0] = n;
        parameters[1] = m;
        parameters[2] = Math.sqrt(noiseDistribution.getNumericalVariance());
        parameters[3] = lastTime;
        for (int i = 0; i < n; i++) {
            parameters[4 + i] = arWeights[i];
        }
        for (int i = 0; i < m; i++) {
            parameters[4 + n + i] = maWeights[i];
        }
        int i = 0;
        for (Double val : values) {
            parameters[4 + n + m + i++] = val;
        }
        for (Double err : errors) {
            parameters[4 + n + m + i++] = err;
        }
        return parameters;
    }
}
