package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class SimpleHistogramDistribution extends AbstractRealDistribution {
    private static final long serialVersionUID = -8306239084071667404L;

    double binwidth;

    SortedMap<Integer, Integer> binsAndValues;
    long sampleSize;

    double mean = 0;

    private double[] samples;

    public SimpleHistogramDistribution() {
        this(0.1);
    }

    public SimpleHistogramDistribution(double binwidth) {
        super(new MersenneTwister());
        assert (binwidth > 0);
        this.binwidth = binwidth;
        binsAndValues = new TreeMap<Integer, Integer>();
        sampleSize = 0;
        samples = new double[0];
    }


    public void addValues(double[] observation) {
        int currentSampleSize = samples.length;
        this.samples = Arrays.copyOf(samples, currentSampleSize + observation.length);
        for (int i = 0; i < observation.length; i++) {
            samples[currentSampleSize + i] = observation[i];
            addValue(observation[i], false);
        }
        this.mean = calcNumericalMean();
    }

    public void addValue(double val) {
        addValue(val, true);
    }

    public String toString() {
        return "Histogram with bin width " + binwidth + " and values:\n" + Arrays.toString(samples);
    }

    public void addValue(double val, boolean updateMeanAndSamples) {
        Integer index = getIndex(val);
        if (!binsAndValues.containsKey(index)) {
            binsAndValues.put(index, 1);
        } else {
            binsAndValues.put(index, binsAndValues.get(index) + 1);
        }
        sampleSize++;
        if (updateMeanAndSamples) {
            this.mean = calcNumericalMean();
            this.samples = Arrays.copyOf(samples, samples.length + 1);
            this.samples[samples.length - 1] = val;
        }
    }

    int getIndex(double d) {
        return (int) Math.round(d / binwidth);
    }

    double getValue(int index) {
        return index * binwidth;
    }

    public double probability(double x) {
        int index = getIndex(x);
        return binsAndValues.containsKey(index) ? (binsAndValues.get(index) / sampleSize) : 0;
    }

    public double density(double x) {
        int index = getIndex(x);
        return binsAndValues.containsKey(index) ? (binsAndValues.get(index) / (sampleSize * binwidth)) : 0;
    }

    public double cumulativeProbability(double x) {
        int index = getIndex(x);
        Iterator<Integer> iter = binsAndValues.keySet().iterator();
        int currentIndex = -1;
        double cumulativeProbability = 0;
        while (iter.hasNext() && currentIndex < index) {
            currentIndex = iter.next();
            cumulativeProbability += binsAndValues.get(currentIndex);
        }
        return cumulativeProbability / sampleSize;
    }

    public double sample() {
        int nextInt = random.nextInt((int) (sampleSize - 1));
        return samples[nextInt];
//		int cumulative = 0;
//		Iterator<Integer> iter = binsAndValues.keySet().iterator();
//		int currentIndex = 0;
////		cumulative+=binsAndValues.get(currentIndex);
//		while (iter.hasNext() && nextInt>=cumulative){
//			currentIndex = iter.next();
//			cumulative+=binsAndValues.get(currentIndex);
//		}
//		return getValue(currentIndex);
    }

    public double sample(double constraint) {
        if (constraint > samples[samples.length - 1]) {
            return constraint;
        }

        // find index of constraint and sample above:
        // assume that samples are ordered.
        int indexOfConstraint = constraint > 0 ? StochasticNetUtils.getIndexBinarySearch(samples, constraint) : 0;

        if (constraint > samples[indexOfConstraint]) {
            indexOfConstraint++;
        }

        int randomDraw = indexOfConstraint + random.nextInt(samples.length - indexOfConstraint);
        return samples[randomDraw];

//		
//		Iterator<Integer> iter = binsAndValues.keySet().iterator();
//		int currentIndex = iter.next();
//		int upUntilIndex = 0;
//		while (iter.hasNext() && getValue(currentIndex) < constraint){
//			upUntilIndex += binsAndValues.get(currentIndex);
//			currentIndex = iter.next();
//		}
//		if (getValue(currentIndex)>= constraint){
//			if (upUntilIndex == sampleSize-1){
//				// one sample:
//				return getValue(currentIndex);
//			}
//			int nextInt = randomData.nextInt(upUntilIndex, (int) (sampleSize-1));
//			int cumulative = upUntilIndex+binsAndValues.get(currentIndex);
////			cumulative+=binsAndValues.get(currentIndex);
//			while (iter.hasNext() && nextInt>=cumulative){
//				currentIndex = iter.next();
//				cumulative+=binsAndValues.get(currentIndex);
//			}
//			return getValue(currentIndex);
//		} else {
//			return constraint;
//		}
    }

    private double calcNumericalMean() {
        double mean = 0;
        for (Integer key : binsAndValues.keySet()) {
            mean += (key * binwidth) * binsAndValues.get(key);
        }
        return mean / sampleSize;
    }

    public double getNumericalMean() {
        return mean;
    }

    public double getNumericalVariance() {
        double[] values = new double[(int) sampleSize];
        int index = 0;
        for (Integer key : binsAndValues.keySet()) {
            double val = getValue(key);
            for (int i = 0; i < binsAndValues.get(key); i++) {
                values[index++] = val;
            }
        }
        DescriptiveStatistics stats = new DescriptiveStatistics(values);
        return stats.getVariance();
    }

    public double getSupportLowerBound() {
        return getValue(binsAndValues.keySet().iterator().next()) - binwidth / 2.0;
    }

    public double getSupportUpperBound() {
        int lastKey = -1;
        for (Integer i : binsAndValues.keySet()) {
            lastKey = i;
        }
        return getValue(lastKey) + binwidth / 2.0;
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public boolean isSupportConnected() {
        return false;
    }
}
