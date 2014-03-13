package org.processmining.plugins.stochasticpetrinet.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.RealDistribution;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics.ReplayStep;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;

/**
 * Provides statistical analysis for outliers using non-parametric density estimations 
 * with foundations in:
 * 
 * Dit-Yan Yeung and C. Chow. Parzen-Window Network Intrusion Detectors. In Pattern Recognition, 2002. 
 * Proceedings. 16th International Conference on, volume 4, pages 385–388 vol.4, 2002.
 * 
 * Extensions are made to exploit dependencies in business processes.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class CaseStatisticsAnalyzer {

	
	private static final int SAMPLE_SIZE = 10000;

	private double outlierRate;
	
	private CaseStatisticsList caseStatistics;

	private StochasticNet stochasticNet;
	
	private Map<CaseStatistics, List<TimedTransition>> numberOfIndividualOutliers;
	private Map<Transition, Double> logLikelihoodCutoffs;
	private Map<Transition, GaussianKernelDistribution> logLikelihoodDistributions;
	private Map<CaseStatistics, List<TimedTransition>> outlierCount;

	private Marking initialMarking;

	
	public CaseStatisticsAnalyzer(StochasticNet stochasticNet, Marking initialMarking, CaseStatisticsList statistics){
		this.caseStatistics = statistics;
		this.outlierRate = CaseStatistics.DEFAULT_OUTLIER_RATE;
		this.stochasticNet = stochasticNet;
		this.initialMarking = initialMarking;
		this.logLikelihoodDistributions = new HashMap<Transition, GaussianKernelDistribution>();
		
		initList();
	}
	
	public CaseStatisticsList getOrderedList(){
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
		for (CaseStatistics cs : caseStatistics){
			maxActivities = Math.max(maxActivities, cs.getReplaySteps().size());
		}
		return maxActivities;
	}

	public int getOutlierCount(CaseStatistics cs) {
		int outliers = 0; 
		if (outlierCount.containsKey(cs)){
			outliers = outlierCount.get(cs).size();
		}
		return outliers;
	}

	public List<TimedTransition> getIndividualOutlierTransitions(CaseStatistics selectedCaseStatistics) {
		return numberOfIndividualOutliers.get(selectedCaseStatistics);
	}

	public List<TimedTransition> getRegularTransitions(CaseStatistics selectedCaseStatistics) {
		List<TimedTransition> regularTransitions = new ArrayList<TimedTransition>();
		List<TimedTransition> outliers = getIndividualOutlierTransitions(selectedCaseStatistics);
		for (ReplayStep rs : selectedCaseStatistics.getReplaySteps()){
			if (!outliers.contains(rs.transition)){
				regularTransitions.add(rs.transition);
			}
		}
		return regularTransitions;
	}

	public Marking getInitialMarking() {
		return initialMarking;
	}

	public Double getLogLikelihoodCutoff(TimedTransition tt) {
		return logLikelihoodCutoffs.get(tt);
	}

	public RealDistribution getLogLikelihoodDistribution(TimedTransition transition) {
		return logLikelihoodDistributions.get(transition);
	}

	public void updateStatistics(double outlierRate) {
		if (outlierRate < 0 || outlierRate >= 1){
			throw new IllegalArgumentException("Please choose values between 0.0 and 1.0 for the outlier rate!");
		} else {
			this.outlierRate = outlierRate;
			
			initList();
		}
	}
	
	private void initList() {
		logLikelihoodCutoffs = new HashMap<Transition, Double>();
		for (Transition t : stochasticNet.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
					// ignore immediate transitions
				} else {
					double[] loglikelihoods = null;
					if (logLikelihoodDistributions.containsKey(tt)){
						// we already have sampled from the distribution, just recompute the cutoff
						loglikelihoods = new double[SAMPLE_SIZE];
						List<Double> oldSampleList = logLikelihoodDistributions.get(tt).getValues();
						for (int i = 0; i < oldSampleList.size(); i++){
							loglikelihoods[i] = oldSampleList.get(i);
						}
					} else {
						RealDistribution d = tt.getDistribution();
						double[] samples = d.sample(SAMPLE_SIZE);
						loglikelihoods = new double[samples.length];
						for (int i = 0; i < samples.length; i++){
							loglikelihoods[i] = Math.log(d.density(samples[i]));
						}
						GaussianKernelDistribution logLikelihoodDistribution = new GaussianKernelDistribution();
						logLikelihoodDistribution.addValues(loglikelihoods);
						this.logLikelihoodDistributions.put(tt, logLikelihoodDistribution);
					}
					
					// threshold is based on probability (we can use the value of the i-th entry in an ordered set
					// i is the ratio determined by i/SAMPLE_SIZE = outlierRate 
					double index = outlierRate * SAMPLE_SIZE;
					double remainder = index-Math.floor(index);
					
					Arrays.sort(loglikelihoods);
					
					double logLikelihoodAtCutoff = (1-remainder) * loglikelihoods[(int)Math.ceil(index)]
					                              + (remainder) * loglikelihoods[(int)Math.floor(index)];
					logLikelihoodCutoffs.put(tt, logLikelihoodAtCutoff);
				}
			}
		}
		
		
		numberOfIndividualOutliers = new HashMap<CaseStatistics, List<TimedTransition>>();
		for (CaseStatistics cs : caseStatistics){
			List<TimedTransition> outlierTransitions = new ArrayList<TimedTransition>();
			for (ReplayStep step : cs.getReplaySteps()){
				if (step.transition != null){
					double logLikelihoodOfActivity = Math.log(step.density);
					if (logLikelihoodOfActivity < logLikelihoodCutoffs.get(step.transition)){
						// outlier
						outlierTransitions.add(step.transition);
					}
				}
			}
			numberOfIndividualOutliers.put(cs, outlierTransitions);
		}
		// order cases by number of outliers first and then rank them in this group by overall likelihood
		Collections.sort(caseStatistics, new CaseComparator(numberOfIndividualOutliers));
	}
	
	/**
	 * 
	 * Compares cases by individual outliers and ranks them by their loglikelihood. 
	 * 
	 * @author Andreas Rogge-Solti
	 *
	 */
	private class CaseComparator implements Comparator<CaseStatistics>{

		private Map<CaseStatistics, List<TimedTransition>> numberOfIndividualOutliers;

		public CaseComparator(Map<CaseStatistics, List<TimedTransition>> individualOutliers){
			this.numberOfIndividualOutliers = individualOutliers;
		}
		
		public int compare(CaseStatistics o1, CaseStatistics o2) {
			int outlierCount1 = 0;
			int outlierCount2 = 0;
			if (numberOfIndividualOutliers.containsKey(o1)){
				outlierCount1 = numberOfIndividualOutliers.get(o1).size();
			}
			if (numberOfIndividualOutliers.containsKey(o2)){
				outlierCount2 = numberOfIndividualOutliers.get(o2).size();
			}
			if (outlierCount1 != outlierCount2){
				return outlierCount2-outlierCount1;
			}
			// compare by loglikelihood to rank them:
			return o1.getLogLikelihood().compareTo(o2.getLogLikelihood());
		}
	}
}
