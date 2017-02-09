package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.distribution.ApproximateDensityDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.DiagonalDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.numeric.ConvolutionHelper;

import javax.swing.*;
import java.util.*;

/**
 * Provides statistical analysis for outliers using non-parametric density estimations
 * with foundations in:
 * <p>
 * Dit-Yan Yeung and C. Chow. Parzen-Window Network Intrusion Detectors. In Pattern Recognition, 2002.
 * Proceedings. 16th International Conference on, volume 4, pages 385–388 vol.4, 2002.
 * <p>
 * Extensions are made to exploit dependencies in business processes.
 *
 * @author Andreas Rogge-Solti
 */
public class CaseStatisticsAnalyzer {
    private static final int SAMPLE_SIZE = 10000;

    private double outlierRate;

    private CaseStatisticsList caseStatistics;

    private StochasticNet stochasticNet;

    private Map<CaseStatistics, List<ReplayStep>> numberOfIndividualOutliers;
    /**
     * The cutoff values for the loglikelihoods of samples that are considered as outliers in a process
     */
    private Map<Transition, Double> logLikelihoodCutoffs;
    private Map<Transition, GaussianKernelDistribution> logLikelihoodDistributions;

    private Map<String, ApproximateDensityDistribution> logLikelihoodApproximations;

    private Marking initialMarking;

    public CaseStatisticsAnalyzer() {
        // empty constructor, use for testing purposes only
    }

    public CaseStatisticsAnalyzer(StochasticNet stochasticNet, Marking initialMarking, CaseStatisticsList statistics) {
        this.caseStatistics = statistics;
        this.outlierRate = CaseStatistics.DEFAULT_OUTLIER_RATE;
        this.stochasticNet = stochasticNet;
        this.initialMarking = initialMarking;
        this.logLikelihoodDistributions = new HashMap<Transition, GaussianKernelDistribution>();
        this.logLikelihoodApproximations = new HashMap<String, ApproximateDensityDistribution>();

        initList();
    }

    public CaseStatisticsList getOrderedList() {
        return caseStatistics;
    }

    public double getOutlierRate() {
        return outlierRate;
    }

    public void setOutlierRate(double outlierRate) {
        this.outlierRate = outlierRate;
    }

    public CaseStatisticsList getCaseStatistics() {
        return caseStatistics;
    }

    public void setCaseStatistics(CaseStatisticsList caseStatistics) {
        this.caseStatistics = caseStatistics;
    }

    public StochasticNet getStochasticNet() {
        return stochasticNet;
    }

    public int getMaxActivityCount() {
        int maxActivities = 0;
        for (CaseStatistics cs : caseStatistics) {
            maxActivities = Math.max(maxActivities, cs.getReplaySteps().size());
        }
        return maxActivities;
    }

    public int getOutlierCount(CaseStatistics cs) {
        int outliers = 0;
        if (numberOfIndividualOutliers.containsKey(cs)) {
            outliers = numberOfIndividualOutliers.get(cs).size();
        }
        return outliers;
    }

    /**
     * Returns the likelihood ratio of the {@link ReplayStep} x stemming from an error distribution,
     * or from the original distribution.
     * Assume that there is but one child (could use weighted average of scores for multiple children).
     * <p>
     * Let's assume an error distribution that can shift the duration of this step and also affect the duration of the next step.
     * We compare the joint probability of the two durations x and y (y is the activity that follows x)
     * in the original model that we learned from historical observations
     * with the distribution that results when we add an error along the y=-x line. Latter is correct because, if x is a measurement error, it also
     * affects the duration of the child in a conversely. For example, when the end of x is mistakenly measured later, than the duration of y
     * is also affected (it is shorter than expected).
     *
     * @param x                        {@link ReplayStep} to compute the error score for
     * @param assumedErrorDistribution the {@link RealDistribution} that is assumed as noise in the data for measurement errors
     * @param assumedErrorRate         the rate of error occurrence (must be between 0 inclusive and 1 exclusive)
     * @return densities of the models:
     * <ul>
     * <li>index 0 contains density of p(x,y) original model,</li>
     * <li>index 1 contains density of p(x,y) error-model</li>
     * <li>index 2 contains the weighted ratio for x,y according to the assumed error rate</li>
     * <p>
     * <li>index 3 contains density of original p(x)</li>
     * <li>index 4 contains density of error-model for p(x)</li>
     * <li>index 5 contains the weighted ratio according for x to the assumed error rate</li>
     * <p>
     * <li>index 6 contains density of original p(y)</li>
     * <li>index 7 contains density of error-model for p(y)</li>
     * <li>index 8 contains the weighted ratio according for y to the assumed error rate</li>
     * <ul>
     */
    public double[] getModelDensities(ReplayStep x, RealDistribution assumedErrorDistribution, double assumedErrorRate) {

        assert (assumedErrorRate >= 0 && assumedErrorRate <= 1);
        ApproximateDensityDistribution errorDistributionX = null;
        ApproximateDensityDistribution errorDistributionY = null;
        String errorDistName = x.transition.getLabel() + "_error";
        if (logLikelihoodApproximations.containsKey(errorDistName)) {
            errorDistributionX = logLikelihoodApproximations.get(errorDistName);
        } else {
            errorDistributionX = (ApproximateDensityDistribution) ConvolutionHelper.getConvolvedDistribution(x.transition.getDistribution(), assumedErrorDistribution);
            logLikelihoodApproximations.put(errorDistName, errorDistributionX);
        }


        double[] values = computeWeightedScoreBetweenDistributions(x, assumedErrorRate, x.transition.getDistribution(), errorDistributionX);
        double[] retVal = new double[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
        for (int i = 0; i < values.length; i++) {
            retVal[3 + i] = values[i];
        }

        if (x.children.size() > 0) {
            // TODO: improve by looking at multiple children (current experiments are mostly sequential)
            ReplayStep y = x.children.iterator().next();
            RealDistribution distributionAlongDiagonalProjectedToX = new DiagonalDistribution(x.transition.getDistribution(),
                    y.transition.getDistribution(), x.duration + y.duration);
            RealDistribution errorDistributionProjectedToX = ConvolutionHelper.getConvolvedDistribution(distributionAlongDiagonalProjectedToX, assumedErrorDistribution);
            values = computeWeightedScoreBetweenDistributions(x, assumedErrorRate, distributionAlongDiagonalProjectedToX,
                    errorDistributionProjectedToX);
            for (int i = 0; i < values.length; i++) {
                retVal[i] = values[i];
            }

            String errorDistNameY = y.transition.getLabel() + "_error";
            if (logLikelihoodApproximations.containsKey(errorDistNameY)) {
                errorDistributionY = logLikelihoodApproximations.get(errorDistNameY);
            } else {
                errorDistributionY = (ApproximateDensityDistribution) ConvolutionHelper.getConvolvedDistribution(y.transition.getDistribution(), assumedErrorDistribution);
                logLikelihoodApproximations.put(errorDistNameY, errorDistributionY);
            }
            values = computeWeightedScoreBetweenDistributions(y, assumedErrorRate, y.transition.getDistribution(), errorDistributionY);
            for (int i = 0; i < values.length; i++) {
                retVal[6 + i] = values[i];
            }
        }
        return retVal;
    }

    private double[] computeWeightedScoreBetweenDistributions(ReplayStep x, double assumedErrorRate,
                                                              RealDistribution distributionAlongDiagonalProjectedToX, RealDistribution errorDistributionProjectedToX) {
        double densityXYAssumingOriginal = distributionAlongDiagonalProjectedToX.density(x.duration);
        double densityXYAssumingError = errorDistributionProjectedToX.density(x.duration);
        double densitySum = (1 - assumedErrorRate) * densityXYAssumingOriginal + assumedErrorRate * densityXYAssumingError;
        double errorScore = 0;
        if (densitySum > 0) {
            errorScore = assumedErrorRate * densityXYAssumingError / densitySum;
        } else {
            // value is completely inexplicable according to both models! It must be an error.
            errorScore = 1;
        }
        return new double[]{densityXYAssumingOriginal, densityXYAssumingError, errorScore};
    }

    /**
     * Let X be this node's random duration variable having the value x.
     * Assume that step is an outlier by itself (i.e., p(X = x) very low compared to the usual values)
     * Let parents be a function assigning the parents to a random duration.
     * <p>
     * We compare the probability of P(children | X) with the marginal probability of P(children | parents(X) ).
     * If we see that the marginal probability is higher than the one given X=x, we assume that it
     * is a single (measurement) error in the log. In the other case, we assume that X fits with the following events and is just a regular outlier.
     *
     * @param step {@link ReplayStep}
     *             <p>
     *             Example:
     *             <p>
     *             U   V   <- parents (if there are more than one, it was a parallel split)
     *             \ /
     *             X   <- variable
     *             / \
     *             Y   Z   <- children (if there are more than one, the process forked into multiple parallel branches)
     *             <p>
     *             here, we compute P(Y=y,Z=z | X=x) and compare it with integral over X of P(Y=y, Z=z, X | U=u, V=v)
     *             That is, we compare
     *             u   v
     *             \ /
     *             x          with     X    <- and integrate over all the values of X
     *             / \                 / \
     *             y   z               y   z
     * @return boolean indicating, whether the outlier is likely to be an error.
     */
    public boolean isOutlierLikelyToBeAnError(ReplayStep step) {
        long now = System.currentTimeMillis();
        double upperBound = getUpperBound(step.transition.getDistribution());


        if (step.children.size() > 0) {
            // compare probabilities of different models:
            // 1. the model that the value is correct
            // 2. the model that the value was wrong (marginal over the joint distribution X+Y)

            double pValueOfChildrenGivenX = 1;
            double pValueOfAlternativeHypothesis = 1; // with X being integrated out
            for (ReplayStep childStep : step.children) {
                pValueOfChildrenGivenX *= getPValueOfStepIntegral(childStep);
                RealDistribution convolvedDist = ConvolutionHelper.getConvolvedDistribution(step.transition.getDistribution(), childStep.transition.getDistribution());
                pValueOfAlternativeHypothesis *= computePValueByApproximateIntegration(convolvedDist, step.duration + childStep.duration);
            }


//			UnivariateFunction function;
//			double probabilityOfChildrenMarginalOverAllValuesOfX = -1;
//			if (step.children.size()==0) {
//				function = new FastDensityFunction(step);
//				probabilityOfChildrenMarginalOverAllValuesOfX = function.value(step.duration);
//			} else if (step.children.size()==1) {
//				function = new FastDensityFunction(step, step.children.iterator().next());
//				probabilityOfChildrenMarginalOverAllValuesOfX = function.value(step.duration+step.children.iterator().next().duration);
//			} else {
//				function = new ReplayStepDensityFunction(step);
//				UnivariateIntegrator integrator = new TrapezoidIntegrator();
//				probabilityOfChildrenMarginalOverAllValuesOfX = integrator.integrate(TrapezoidIntegrator.DEFAULT_MAX_ITERATIONS_COUNT, function, -1, upperBound);
//			}

            //		System.out.println("orig probability: "+probabilityOfChildrenGivenX+", marginal probability: "+probabilityOfChildrenMarginalOverAllValuesOfX);
            //		System.out.println("integration took "+(System.currentTimeMillis()-now)+"ms");
//			return probabilityOfChildrenMarginalOverAllValuesOfX > probabilityOfChildrenGivenX;
            return pValueOfAlternativeHypothesis > pValueOfChildrenGivenX;
        } else {
            return getPValueOfStepIntegral(step) < 0.05;
        }
    }

//	/**
//	 * TODO: Replace with integral (problem: Don't know the exact shape of the density of the log-likelihoods, really)
//	 * 
//	 * @param step
//	 * @return
//	 */
//	public double getPValueOfStep(ReplayStep step){
//		if (step.transition.getDistributionType().equals(DistributionType.IMMEDIATE)){
//			if (step.duration==0){
//				return 1;
//			} else {
//				return 0;
//			}
//		}
//		int size = 10000;
//		double[] samples = step.transition.getDistribution().sample(size);
//		for (int i = 0; i < samples.length; i++){
//			samples[i] = Math.log(step.transition.getDistribution().density(samples[i]));
//		}
//		Arrays.sort(samples);
//		
//		// find likelihood of the observed sample and see, how many fall below this
//		double lSample = Math.log(step.density);
//		for (int i = 0; i < samples.length; i++){
//			if (samples[i] > lSample){
//				return i/(double)size;
//			}
//		}
//		return 1;
//	}

    public double getPValueOfStepIntegral(ReplayStep step) {
        if (step.transition.getDistributionType().equals(DistributionType.IMMEDIATE)) {
            if (step.duration == 0) {
                return 1;
            } else {
                return 0;
            }
        }
        if (step.transition.getDistribution() instanceof NormalDistribution) {
            // symmetric distribution
            double mean = ((NormalDistribution) step.transition.getDistribution()).getMean();
            double cumulativeProbability = step.transition.getDistribution().cumulativeProbability(step.duration);
            if (step.duration > mean) {
                cumulativeProbability = 1 - cumulativeProbability;
            }
            return 2 * cumulativeProbability;
        } else if (step.transition.getDistribution() instanceof ExponentialDistribution) {
            return 1 - step.transition.getDistribution().cumulativeProbability(step.duration);
        } else {
            return computePValueByApproximateIntegration(step);
        }
    }

    public double computePValueByApproximateIntegration(ReplayStep step) {
        return computePValueByApproximateIntegration(step.transition.getDistribution(), step.duration);
    }

    public double computePValueByApproximateIntegration(RealDistribution dist, double x) {
        ApproximateDensityDistribution approximateDistribution = null;
        if (this.logLikelihoodApproximations.containsKey(dist.toString())) {
            approximateDistribution = this.logLikelihoodApproximations.get(dist.toString());
        } else {
            int size = 10000;
            double[] samples = dist.sample(size);
            for (int i = 0; i < samples.length; i++) {
                samples[i] = Math.log(dist.density(samples[i]));
            }
            GaussianKernelDistribution kernelApproximation = new GaussianKernelDistribution();
            kernelApproximation.addValues(samples);
            approximateDistribution = new ApproximateDensityDistribution(kernelApproximation, true);
            this.logLikelihoodApproximations.put(dist.toString(), approximateDistribution);
        }
        TrapezoidIntegrator integrator = new TrapezoidIntegrator();
        if (dist.density(x) == 0) {
            return 0;
        }
        double logLk = Math.log(dist.density(x));
        return integrator.integrate(TrapezoidIntegrator.DEFAULT_MAX_ITERATIONS_COUNT, approximateDistribution, Math.min(approximateDistribution.getSupportLowerBound(), logLk - 1), logLk);
    }

    private double getUpperBound(RealDistribution distribution) {
        if (distribution instanceof GaussianKernelDistribution) {
            return ((GaussianKernelDistribution) distribution).getReasonableUpperBound();
        }
        if (distribution instanceof NormalDistribution) {
            return ((NormalDistribution) distribution).getMean() + 5 * ((NormalDistribution) distribution).getStandardDeviation();
        }
        if (distribution instanceof ExponentialDistribution) {
            return distribution.inverseCumulativeProbability(0.999);
        }
        return distribution.getSupportUpperBound();
    }

    public List<ReplayStep> getIndividualOutlierSteps(CaseStatistics selectedCaseStatistics) {
        return numberOfIndividualOutliers.get(selectedCaseStatistics);
    }

    public List<ReplayStep> getRegularSteps(CaseStatistics selectedCaseStatistics) {
        List<ReplayStep> regularTransitions = new ArrayList<ReplayStep>();
        List<ReplayStep> outliers = getIndividualOutlierSteps(selectedCaseStatistics);
        for (ReplayStep rs : selectedCaseStatistics.getReplaySteps()) {
            if (!outliers.contains(rs)) {
                regularTransitions.add(rs);
            }
        }
        return regularTransitions;
    }

    public Marking getInitialMarking() {
        return initialMarking;
    }

    public Double getLogLikelihoodCutoff(TimedTransition tt) {
        if (logLikelihoodCutoffs.containsKey(tt)) {
            return logLikelihoodCutoffs.get(tt);
        }
        return 0.0;
    }

    public RealDistribution getLogLikelihoodDistribution(TimedTransition transition) {
        return logLikelihoodDistributions.get(transition);
    }

    public void updateStatistics(double outlierRate) {
        if (outlierRate < 0 || outlierRate >= 1) {
            throw new IllegalArgumentException("Please choose values between 0.0 and 1.0 for the outlier rate!");
        } else {
            this.outlierRate = outlierRate;

            initList();
        }
    }

    private void initList() {
        updateLikelihoodCutoffs();


        numberOfIndividualOutliers = new HashMap<CaseStatistics, List<ReplayStep>>();
        for (CaseStatistics cs : caseStatistics) {
            List<ReplayStep> outlierSteps = new ArrayList<ReplayStep>();
            for (ReplayStep step : cs.getReplaySteps()) {
                if (step.transition != null && step.transition.getDistribution() != null) {
                    double logLikelihoodOfActivity = Math.log(step.density);
                    double logLikelihoodCutoff = logLikelihoodCutoffs.get(step.transition);
                    if (logLikelihoodOfActivity < logLikelihoodCutoff) {
                        // outlier
                        outlierSteps.add(step);
                    }
                }
            }
            numberOfIndividualOutliers.put(cs, outlierSteps);
        }
        // order cases by number of outliers first and then rank them in this group by overall likelihood
        Collections.sort(caseStatistics, new CaseComparator(numberOfIndividualOutliers));
    }

    public void updateLikelihoodCutoffs() {
        logLikelihoodCutoffs = new HashMap<Transition, Double>();
        for (Transition t : stochasticNet.getTransitions()) {
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                    // ignore immediate transitions
                } else {
                    double[] loglikelihoods = null;
                    if (logLikelihoodDistributions.containsKey(tt)) {
                        // we already have sampled from the distribution, just recompute the cutoff
                        loglikelihoods = new double[SAMPLE_SIZE];
                        List<Double> oldSampleList = logLikelihoodDistributions.get(tt).getValues();
                        for (int i = 0; i < oldSampleList.size(); i++) {
                            loglikelihoods[i] = oldSampleList.get(i);
                        }
                    } else {
                        RealDistribution d = tt.getDistribution();
                        if (d == null) {
                            String tName = tt.getLabel() == null ? tt.getId().toString() : tt.getLabel();
                            String error = "Transition " + tName + " has no distribution! Can't compute anomaly intervals.";
                            JOptionPane.showMessageDialog(null, error, "Model assumption error", JOptionPane.ERROR_MESSAGE);
                            throw new IllegalArgumentException(error);
                        }
                        double[] samples = d.sample(SAMPLE_SIZE);
                        loglikelihoods = new double[samples.length];
                        for (int i = 0; i < samples.length; i++) {
                            loglikelihoods[i] = Math.log(d.density(samples[i]));
                        }
                        GaussianKernelDistribution logLikelihoodDistribution = new GaussianKernelDistribution();
                        logLikelihoodDistribution.addValues(loglikelihoods);
                        this.logLikelihoodDistributions.put(tt, logLikelihoodDistribution);
                    }

                    // threshold is based on probability (we can use the value of the i-th entry in an ordered set
                    // i is the ratio determined by i/SAMPLE_SIZE = outlierRate
                    double index = outlierRate * SAMPLE_SIZE;
                    double remainder = index - Math.floor(index);

                    Arrays.sort(loglikelihoods);

                    double logLikelihoodAtCutoff = (1 - remainder) * loglikelihoods[(int) Math.ceil(index)]
                            + (remainder) * loglikelihoods[(int) Math.floor(index)];
                    logLikelihoodCutoffs.put(tt, logLikelihoodAtCutoff);
                }
            }
        }
    }

    /**
     * Compares cases by individual outliers and ranks them by their loglikelihood.
     *
     * @author Andreas Rogge-Solti
     */
    private class CaseComparator implements Comparator<CaseStatistics> {

        private Map<CaseStatistics, List<ReplayStep>> numberOfIndividualOutliers;

        public CaseComparator(Map<CaseStatistics, List<ReplayStep>> individualOutliers) {
            this.numberOfIndividualOutliers = individualOutliers;
        }

        public int compare(CaseStatistics o1, CaseStatistics o2) {
            int outlierCount1 = 0;
            int outlierCount2 = 0;
            if (numberOfIndividualOutliers.containsKey(o1)) {
                outlierCount1 = numberOfIndividualOutliers.get(o1).size();
            }
            if (numberOfIndividualOutliers.containsKey(o2)) {
                outlierCount2 = numberOfIndividualOutliers.get(o2).size();
            }
            if (outlierCount1 != outlierCount2) {
                return outlierCount2 - outlierCount1;
            }
            // compare by loglikelihood to rank them:
            return o1.getLogLikelihood().compareTo(o2.getLogLikelihood());
        }
    }
}
