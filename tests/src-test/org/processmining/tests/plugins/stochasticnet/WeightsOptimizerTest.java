package org.processmining.tests.plugins.stochasticnet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.MarkingBasedSelectionWeightCostFunction;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.NormalizedPositiveGradientDescent;

public class WeightsOptimizerTest {

	@Test
	public void testOptimizer() throws Exception {
		Map<short[], int[]> markingBasedSelections = new HashMap<short[], int[]>();
		
		int[] selectionsM1 = new int[]{0,0,0,0,19,0};
		int[] selectionsM2 = new int[]{3,6,10,0,0,0};
		int[] selectionsM3 = new int[]{3,5,0,2,0,0};
		int[] selectionsM4 = new int[]{1,1,0,0,0,0};
		int[] selectionsM5 = new int[]{0,0,9,0,0,0};
		int[] selectionsM6 = new int[]{0,0,0,17,0,0};
		int[] selectionsM7 = new int[]{0,0,0,0,0,19};
		
		short[] m1 = new short[]{1,0,0,0,0,0,0};
		short[] m2 = new short[]{0,1,1,0,0,0,0};
		short[] m3 = new short[]{0,0,1,1,0,0,0};
		short[] m4 = new short[]{0,0,1,0,0,1,0};
		short[] m5 = new short[]{0,1,0,0,1,0,0};
		short[] m6 = new short[]{0,0,0,1,1,0,0};
		short[] m7 = new short[]{0,0,0,0,1,5,0};
		
		markingBasedSelections.put(m1, selectionsM1);
		markingBasedSelections.put(m2, selectionsM2);
		markingBasedSelections.put(m3, selectionsM3);
		markingBasedSelections.put(m4, selectionsM4);
		markingBasedSelections.put(m5, selectionsM5);
		markingBasedSelections.put(m6, selectionsM6);
		markingBasedSelections.put(m7, selectionsM7);
		
		double[] theta = new double[6];
		Arrays.fill(theta, 1);
		
		NormalizedPositiveGradientDescent npgd = new NormalizedPositiveGradientDescent();
		MarkingBasedSelectionWeightCostFunction costFunction = new MarkingBasedSelectionWeightCostFunction(markingBasedSelections);
		npgd.optimize(theta, costFunction);
		System.out.println("weights: "+Arrays.toString(theta));
		
	}
}
