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
		Map<String, int[]> markingBasedSelections = new HashMap<String, int[]>();
		
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
		
		markingBasedSelections.put(Arrays.toString(m1), selectionsM1);
		markingBasedSelections.put(Arrays.toString(m2), selectionsM2);
		markingBasedSelections.put(Arrays.toString(m3), selectionsM3);
		markingBasedSelections.put(Arrays.toString(m4), selectionsM4);
		markingBasedSelections.put(Arrays.toString(m5), selectionsM5);
		markingBasedSelections.put(Arrays.toString(m6), selectionsM6);
		markingBasedSelections.put(Arrays.toString(m7), selectionsM7);
		
		double[] theta = new double[6];
		Arrays.fill(theta, 1);
		
		NormalizedPositiveGradientDescent npgd = new NormalizedPositiveGradientDescent();
		MarkingBasedSelectionWeightCostFunction costFunction = new MarkingBasedSelectionWeightCostFunction(markingBasedSelections);
		npgd.optimize(theta, costFunction);
		System.out.println("weights: "+Arrays.toString(theta));
	}
	
	@Test
	public void testOptimizer2() throws Exception {
		Map<String, int[]> markingBasedSelections = new HashMap<String, int[]>();
		
		int[] selectionsM1 = new int[]{15,7,8};
		int[] selectionsM2 = new int[]{0,5,10};
		
		short[] m1 = new short[]{1,0};
		short[] m2 = new short[]{0,1};
		
		markingBasedSelections.put(Arrays.toString(m1), selectionsM1);
		markingBasedSelections.put(Arrays.toString(m2), selectionsM2);
		
		double[] theta = new double[3];
		Arrays.fill(theta, 1);
		
		NormalizedPositiveGradientDescent npgd = new NormalizedPositiveGradientDescent();
		MarkingBasedSelectionWeightCostFunction costFunction = new MarkingBasedSelectionWeightCostFunction(markingBasedSelections);
		npgd.optimize(theta, costFunction);
		System.out.println("weights: "+Arrays.toString(theta));
	}
	
	@Test
	public void testOptimizer3() throws Exception {
		Map<String, int[]> markingBasedSelections = new HashMap<String, int[]>();
		
		int[] selectionsM1 = new int[]{1,4};
		int[] selectionsM2 = new int[]{2,5};
		
		short[] m1 = new short[]{1,0};
		short[] m2 = new short[]{0,1};
		
		markingBasedSelections.put(Arrays.toString(m1), selectionsM1);
		markingBasedSelections.put(Arrays.toString(m2), selectionsM2);
		
		double[] theta = new double[2];
		Arrays.fill(theta, 1);
		
		NormalizedPositiveGradientDescent npgd = new NormalizedPositiveGradientDescent();
		MarkingBasedSelectionWeightCostFunction costFunction = new MarkingBasedSelectionWeightCostFunction(markingBasedSelections);
		npgd.optimize(theta, costFunction);
		System.out.println("weights: "+Arrays.toString(theta));
	}
}
