package org.processmining.tests.plugins.stochasticnet.measure;

import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.stochasticpetrinet.measures.ComputedMeasures;
import org.processmining.plugins.stochasticpetrinet.measures.MeasureConfig;
import org.processmining.plugins.stochasticpetrinet.measures.MeasurePlugin;
import org.processmining.plugins.stochasticpetrinet.measures.MeasureProvider;
import org.processmining.plugins.stochasticpetrinet.measures.entropy.EntropyCalculatorApproximate;
import org.processmining.plugins.stochasticpetrinet.measures.entropy.EntropyCalculatorQuantile;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class EntropyTest {

	@Test
	public void testEntropySequence() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}

	@Test
	public void testEntropyLoopyFreeChoice() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("loopy_free_choice", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	@Test
	public void testEntropy50NoLoops() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("50_no_loops", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		for (int i = 0 ; i < 50; i++){
			ComputedMeasures measures = plugin.getMeasure(null, model, new MeasureConfig(new EntropyCalculatorApproximate()));
			System.out.println(measures);
		}
	}
	
	@Test
	public void testEntropy10Exclusive() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("10_exclusive", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		for (int i = 0 ; i < 50; i++){
			ComputedMeasures measures = plugin.getMeasure(null, model, new MeasureConfig(new EntropyCalculatorApproximate()));
			System.out.println(measures);
		}
	}
	
	@Test
	public void testLoopyFreeChoice() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("loopy_free_choice", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		for (int i = 0 ; i < 50; i++){
			ComputedMeasures measures = plugin.getMeasure(null, model, new MeasureConfig(new EntropyCalculatorApproximate()));
			System.out.println(measures);
		}
	}
	
	@Test
	public void testEntropy01() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test01_xor_split", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	@Test
	public void testEntropy02() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test02_and_AB", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	/**
	 * Two branches a 2 activities:
	 * up: A,B
	 * down: C,D
	 * 
	 * Outcomes: ABCD (0.25), ACBD (0.125), ACDB (0.125), CABD (0.125), CADB (0.125), CDAB (0.25)
	 * 
	 * Entropy = 2 * 1/4 * log² (4) + 4* 1/8 *log² (8) = 2.5
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEntropy02a() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test02_and_ABCD", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	/**
	 * Process with 3 branches a 2 activities.
	 * Combinations: 6 activities -> 6! 
	 * always two in sequence (6! / (2! * 2! * 2!)) = 6! / 6 = 5! = 120 possible outcomes in a list-abstraction
	 * Entropy = 120 * 1/120 * log²(120) = 6.906890596
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEntropy02b() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test02_and_ABCDEF", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	/**
	 * Process with 3 branches a 1 activity.
	 * Combinations: 3 activities -> 3! = 6 
	 * Entropy = 6 * 1/6 * log²(6) = 2,584962501
	 * 
	 * @throws Exception
	 */
	@Test
	public void testEntropy02c() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test02_and_ABC", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	@Test
	public void testEntropy03() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test03_and_ABCD_skip", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}
	
	@Test
	public void testEntropy04() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("test04_loop_ABC", true);
		
		StochasticNet model = (StochasticNet) netAndMarking[0];
		
		MeasurePlugin plugin = new MeasurePlugin();
		ComputedMeasures measures = plugin.getMeasure(null, model);
		System.out.println(measures);
	}


	@Test
	public void testEntropyPQL() throws Exception {

		double quantile = 0.8;

		System.out.println("RUNNING WITH STATE SPACE QUANTILE: "+quantile);
		for (int i = 1; i <= 10; i++) {
			long start = System.currentTimeMillis();
			Object[] netAndMarking = TestUtils.loadModel("pql/"+i, true);

			StochasticNet model = (StochasticNet) netAndMarking[0];

			MeasurePlugin plugin = new MeasurePlugin();
			MeasureConfig config = new MeasureConfig(new EntropyCalculatorQuantile(quantile));
			ComputedMeasures measures = plugin.getMeasure(null, model, config);

			System.out.println("-----------");
			System.out.println("model "+i+":  (took "+(System.currentTimeMillis()-start)+"ms)");
			System.out.println("-----------");
			System.out.println(measures);
			System.out.println("-----------");
		}
	}
}
