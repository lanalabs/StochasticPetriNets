package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class PerformanceEnricherExperimentResult {

	private Map<Integer, Map<ExecutionPolicy, ModelComparisonResult>> results;
	
	String SEPARATOR = ";";
	
	public PerformanceEnricherExperimentResult(){
		results = new TreeMap<Integer, Map<ExecutionPolicy, ModelComparisonResult>>();
	}
	
	public void add(int traceSize, ExecutionPolicy policy, StochasticNet net, StochasticNet learnedNet) {
		if (!results.containsKey(traceSize)){
			results.put(traceSize, new HashMap<ExecutionPolicy, PerformanceEnricherExperimentResult.ModelComparisonResult>());
		}
		results.get(traceSize).put(policy, getComparisonResult(net,learnedNet));
	}
	
	private ModelComparisonResult getComparisonResult(StochasticNet net, StochasticNet learnedNet) {
		ModelComparisonResult result = new ModelComparisonResult();
		// go through all weights:
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				TimedTransition ttLearned = getTimedTransitionFromNet(learnedNet, tt);
				
				result.weightDifferences.addValue(Math.pow(tt.getWeight()-ttLearned.getWeight(),2));
				if (tt.getDistributionType().equals(DistributionType.IMMEDIATE) || ttLearned.getDistributionType().equals(DistributionType.IMMEDIATE)){
					System.out.println("transition "+tt.getLabel()+"("+tt.getDistributionType()+"/"+ttLearned.getDistributionType()+") weight: "+tt.getWeight()+",\t learnedWeight: "+ttLearned.getWeight());
				} else {
					System.out.println("transition "+tt.getLabel()+"("+tt.getDistributionType()+"/"+ttLearned.getDistributionType()+") weight: "+tt.getWeight()+",\t learnedWeight: "+ttLearned.getWeight()+",\t mean: "+tt.getDistribution().getNumericalMean()+",\t learned mean: "+ttLearned.getDistribution().getNumericalMean());
				}
				if (tt.getDistribution() != null && ttLearned.getDistribution() != null){
					result.firstMomentDifferences.addValue(Math.pow(tt.getDistribution().getNumericalMean()-ttLearned.getDistribution().getNumericalMean(),2));
				} else if (tt.getDistributionType().equals(DistributionType.IMMEDIATE) && ttLearned.getDistributionType().equals(DistributionType.IMMEDIATE)){
					result.firstMomentDifferences.addValue(0);
				} else {
					result.firstMomentDifferences.addValue(1);
				}
			}
		}
		return result;
	}
	
	public String getResultsCSV(){
		StringBuilder builder = new StringBuilder();
		
		builder.append("TraceSize"+SEPARATOR);
		for (ExecutionPolicy policy : ExecutionPolicy.values()){
			builder.append("MSE weights ("+policy.shortName()+")"+SEPARATOR+"MSE means ("+policy.shortName()+")"+SEPARATOR);
		}
		builder.append("\n");
		
		
		for (Integer traceSize : results.keySet()){
			builder.append(traceSize+SEPARATOR);
			
			Map<ExecutionPolicy, ModelComparisonResult> result = results.get(traceSize);
			
			for (ExecutionPolicy policy : ExecutionPolicy.values()){
				ModelComparisonResult r = result.get(policy);
				builder.append(r.weightDifferences.getMean()+SEPARATOR+r.firstMomentDifferences.getMean()+SEPARATOR);
			}
			builder.append("\n");
		}
		return builder.toString();
		
	}

	private TimedTransition getTimedTransitionFromNet(StochasticNet learnedNet, TimedTransition tt) {
		for (Transition t : learnedNet.getTransitions()){
			if (t.getLabel().equals(tt.getLabel())){
				return (TimedTransition) t;
			}
		}
		return null;
	}

	class ModelComparisonResult {
		public ModelComparisonResult(){
			weightDifferences = new DescriptiveStatistics();
			firstMomentDifferences = new DescriptiveStatistics();
		}
		DescriptiveStatistics weightDifferences;
		DescriptiveStatistics firstMomentDifferences;
	}

}
