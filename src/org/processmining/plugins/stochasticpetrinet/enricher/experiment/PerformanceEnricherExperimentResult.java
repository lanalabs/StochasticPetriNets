package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class PerformanceEnricherExperimentResult {

	private Map<ExecutionPolicy,Map<Integer, ModelComparisonResult>> results;
	
	public PerformanceEnricherExperimentResult(){
		results = new HashMap<StochasticNet.ExecutionPolicy, Map<Integer,ModelComparisonResult>>();
	}
	
	public void add(int traceSize, ExecutionPolicy policy, StochasticNet net, StochasticNet learnedNet) {
		if (!results.containsKey(policy)){
			results.put(policy, new HashMap<Integer, PerformanceEnricherExperimentResult.ModelComparisonResult>());
		}
		results.get(policy).put(traceSize, getComparisonResult(net,learnedNet));
		
	}
	
	private ModelComparisonResult getComparisonResult(StochasticNet net, StochasticNet learnedNet) {
		ModelComparisonResult result = new ModelComparisonResult();
		// go through all weights:
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				TimedTransition ttLearned = getTimedTransitionFromNet(learnedNet, tt);
				
				result.weightDifferences.addValue(tt.getWeight()-ttLearned.getWeight());
				result.firstMomentDifferences.addValue(tt.getDistribution().getNumericalMean()-ttLearned.getDistribution().getNumericalMean());
			}
		}
		return result;
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
