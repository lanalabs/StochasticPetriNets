package org.processmining.plugins.stochasticpetrinet.enricher.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkingBasedSelectionWeightCostFunction implements CostFunction {

	private Map<Integer, List<short[]>> markingsInWhichTransitionWasSelected;
	private Map<short[], int[]> markingBasedSelectionCounts;

	public MarkingBasedSelectionWeightCostFunction(Map<short[], int[]> markingBasedSelections) {
		this.markingBasedSelectionCounts = markingBasedSelections;
		this.markingsInWhichTransitionWasSelected = new HashMap<Integer, List<short[]>>();
		for (short[] marking : markingBasedSelections.keySet()) {
			for (int idx = 0; idx < markingBasedSelections.get(marking).length; idx++){
				int count = markingBasedSelections.get(marking)[idx];
				
				if (count > 0) {
					if (!markingsInWhichTransitionWasSelected.containsKey(idx)) {
						markingsInWhichTransitionWasSelected.put(idx, new ArrayList<short[]>());
					}
					markingsInWhichTransitionWasSelected.get(idx).add(marking);
				}
			}
		}
	}

	/**
	 * Simply return the average error (not squared)
	 */
	public double getPartialDerivation(double[] theta, int i) {
		List<short[]> markingsInWhichTransitionIFired = markingsInWhichTransitionWasSelected.get(i);
		double errorSum = 0;
		int errorCount = 0;
		for (short[] marking : markingsInWhichTransitionIFired){
			List<Integer> transitionIndices = new ArrayList<Integer>();
			int[] transitionCounts = markingBasedSelectionCounts.get(marking);
			int sumOfFiringsInMarking = 0;
			int ownFirings = 0;
			for (int idx = 0; idx < transitionCounts.length; idx++){
				if (idx == i){
					ownFirings = transitionCounts[idx];
				}
				if (transitionCounts[idx] > 0){
					transitionIndices.add(idx);
				}
				sumOfFiringsInMarking += transitionCounts[idx];
			}
			assert sumOfFiringsInMarking > 0; // we should only have markings here, where firings occurred!
			
			// condition: at least two transitions should have fired in current marking to impose restrictions on the weight!
			if (transitionIndices.size()>1){
				// normalize:
				errorCount++;
				double toBeRatio = ownFirings / (double)sumOfFiringsInMarking;
				
				double weightSum = 0;
				for (int index : transitionIndices){
					weightSum += theta[index];
				}
				double currentRatio = theta[i]/weightSum;
				errorSum += currentRatio-toBeRatio;
			}
		}
		if (errorCount == 0){
			return 0;
		} else {
			return errorSum / errorCount; // return average error
		}
	}

	public double getCost(double[] theta) {
		double errorSum = 0;
		int errorCount = 0;
		for (short[] marking : markingBasedSelectionCounts.keySet()){
			//scale both firing counts and theta weights to sum to 1:
			List<Integer> transitionIndices = new ArrayList<Integer>();
			int[] transitionCounts = markingBasedSelectionCounts.get(marking);
			double sumOfFiringsInMarking = 0;
			double sumOfWeights = 0;
			for (int idx = 0; idx < transitionCounts.length; idx++){
				if (transitionCounts[idx] > 0){
					sumOfFiringsInMarking += transitionCounts[idx];
					sumOfWeights += theta[idx];
					transitionIndices.add(idx);
				}
			}
			errorCount++;
			for (Integer idx : transitionIndices){
				double scaledWeight = theta[idx] / sumOfWeights;
				double scaledFiringRate = transitionCounts[idx] / sumOfFiringsInMarking;
				errorSum += Math.pow(scaledWeight-scaledFiringRate, 2);
			}
			assert sumOfFiringsInMarking > 0; // we should only have markings here, where firings occurred!
		}
		return errorSum / (2*errorCount); // return average error
	}

}