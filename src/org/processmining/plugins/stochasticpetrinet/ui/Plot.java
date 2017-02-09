package org.processmining.plugins.stochasticpetrinet.ui;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.SimpleHistogramDistribution;

import java.util.Vector;

/**
 * Plot to display the probability functions.
 *
 * @author Andreas Rogge-Solti
 */
public class Plot {
    private Vector<RealDistribution> weightedPlots;

    private String name;

    private double weight = 1;

    private double maxX = 1, minX = 0, maxY = 0;


    public Plot(String name) {
        weightedPlots = new Vector<RealDistribution>();
        this.name = name;
    }

    public void add(RealDistribution wnd) {
        if (wnd != null) {
            weightedPlots.add(wnd);
        }
        updateMaxValues();
    }

    public void setUnivariateRealFunctions(Vector<RealDistribution> wnds) {
        weightedPlots = wnds;
        updateMaxValues();
    }

    public RealDistribution getTimeSpecification(int index) {
        return weightedPlots.elementAt(index);
    }

    public int size() {
        return weightedPlots.size();
    }

    public String getName() {
        return this.name;
    }

    /**
     * Returns the value of the weighted density functions
     *
     * @param x
     * @return
     */
    public double getVal(double x) {
        double result = 0;

        // FIXME: weights time dependent!
        for (RealDistribution wnd : weightedPlots) {
            try {
                result += wnd.density(x);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        maxY = Math.max(maxY, result * weight);
        return result * weight;
    }

    /**
     * @return Maximum value for x at which the plot value is greater than 0, beyond that threshold, it is 0.
     */
    public double getXMax() {
        return maxX;
    }

    public double getXMin() {
        return minX;
    }

    public double getYMax() {
        return maxY;
    }

    private void updateMaxValues() {
        maxX = 0.01;
        minX = 0;
        for (RealDistribution dist : weightedPlots) {
            if (dist != null) {
                if (dist instanceof SimpleHistogramDistribution) {
                    maxX = Math.max(maxY, dist.getSupportUpperBound());
                    minX = Math.min(minX, dist.getSupportLowerBound());
                } else {
                    try {
                        if (Double.isInfinite(dist.getSupportLowerBound())) {
                            minX = Math.min(minX, dist.inverseCumulativeProbability(0.00001));
                        } else {
                            minX = Math.min(minX, dist.getSupportLowerBound());
                        }
                        if (Double.isInfinite(dist.getSupportUpperBound())) {
                            if (dist instanceof LogNormalDistribution) {
                                // very heavy tailed distribution: to be able to see something useful, we clip already at 3 * the mean.
                                LogNormalDistribution lnorm = (LogNormalDistribution) dist;
                                double realMean = Math.pow(Math.E, lnorm.getScale());
                                maxX = Math.max(maxX, 3 * realMean);
                            } else {
                                maxX = Math.max(maxX, dist.inverseCumulativeProbability(0.99999));
                            }
                        } else {
                            maxX = Math.max(maxX, dist.getSupportUpperBound());
                        }
                    } catch (Exception e) {
                        maxX = Math.max(maxX, 50);
                        minX = Math.min(minX, 0);
                    }
                }
            }
        }
//		for (UnivariateRealFunction wnd : weightedPlots){
//			
//			if (wnd == null){
//				throw new RuntimeException("?");
//			}
//			try {
//				double[] support = wnd.effective_support(1e-5);
//				if (support[1]>maxX){
//					maxX = support[1];
//				}
//				maxY = Math.max(maxY, wnd.p(new double[]{wnd.expected_value()}));
//			} catch (Exception e) {
//				e.printStackTrace();
//			} 
//		}
    }

    /**
     * Sets the scaling factor for the plot.
     * In a process model the branch probability is the weight.
     * It should always be between 0 and 1.
     *
     * @param weight
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

}
