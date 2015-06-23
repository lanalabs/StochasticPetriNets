package org.processmining.tests.plugins.stochasticnet;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.StochasticNetSemantics;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.generator.Generator;
import org.processmining.plugins.stochasticpetrinet.generator.GeneratorConfig;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

/**
 * Collects run times for prediction of different models.
 * comparing exponential, normal, gaussian_kernel, and log_spline density estimators
 *  
 * @author Andreas Rogge-Solti
 *
 */
public class PredictionPerformanceTest {

	public static final int[] MODEL_SIZES = new int[]{1,3,10,30,100,300,1000,3000,10000};
//	public static final int[] MODEL_SIZES = new int[]{1,5,10,100,500,1000,2000};
	
	// LOG_SPLINE omitted
	public static final DistributionType[] TYPES_TO_COMPARE = new DistributionType[]{DistributionType.GAUSSIAN_KERNEL,DistributionType.EXPONENTIAL, DistributionType.NORMAL, DistributionType.UNIFORM,DistributionType.LOGNORMAL};

	private static final String SEPARATOR = ";";
	
	private static final int REPETITIONS = 30;
	
	@Test
	public void testPerformance(){
		Map<DistributionType,Map<Integer, List<Double>>> predictionDurations = new HashMap<StochasticNet.DistributionType, Map<Integer,List<Double>>>();
		
		GeneratorConfig config = new GeneratorConfig();
		config.setContainsLoops(false);
		config.setImmedateTransitionsInvisible(false);
		config.setDegreeOfParallelism(20);
		config.setDegreeOfExclusiveChoices(40);
		config.setDegreeOfSequences(40);
		config.setCreateDedicatedImmediateStartTransition(true);
		for (DistributionType type : TYPES_TO_COMPARE){
			Map<Integer, List<Double>> predictionDurationsForType = new HashMap<Integer, List<Double>>();
			config.setDistributionType(type);
			Generator generator = new Generator(10); 
			for (int modelSize : MODEL_SIZES){
				DescriptiveStatistics stats = new DescriptiveStatistics();
				for (int rep = 0; rep < REPETITIONS; rep++){
					long durationOfPrediction = getPredictionDuration(TimeUnit.MINUTES, config, type,
							predictionDurationsForType, generator, modelSize);
					stats.addValue(durationOfPrediction);
					String time = (durationOfPrediction > 1000)?String.format("%.02f", durationOfPrediction/1000.)+"s":(durationOfPrediction)+" ms"; 
					System.out.println("Prediction of 1 trace took "+time+".");
				}
				// store averages only...
				predictionDurationsForType.get(modelSize).add(stats.getMean());
			}
			predictionDurations.put(type, predictionDurationsForType);
			printResult(predictionDurations);
		}
		printResult(predictionDurations);
	}

	public void printResult(Map<DistributionType, Map<Integer, List<Double>>> predictionDurations) {
		String result = "Model-Size"+SEPARATOR;
		for (DistributionType type : TYPES_TO_COMPARE){
			result += type.toString() + SEPARATOR;
		}
		result += "\n";
		for (int i : MODEL_SIZES){
			result += i + SEPARATOR;
			for (DistributionType type : TYPES_TO_COMPARE){
				if (predictionDurations.containsKey(type) && predictionDurations.get(type).containsKey(i)){
					List<Double> performanceDurations = predictionDurations.get(type).get(i);
					DescriptiveStatistics stats = new DescriptiveStatistics(StochasticNetUtils.getAsDoubleArray(performanceDurations));
					result += stats.getMean()+SEPARATOR;
				} else {
					result += "NaN"+SEPARATOR;
				}
			}
			result += "\n";
		}
		System.out.println("--------------------");
		System.out.println(result);
	}

	public long getPredictionDuration(TimeUnit timeUnit, GeneratorConfig config, DistributionType type,
			Map<Integer, List<Double>> predictionDurationsForType, Generator generator, int modelSize) {
		if (!predictionDurationsForType.containsKey(modelSize)){
			predictionDurationsForType.put(modelSize, new LinkedList<Double>());
		}
		config.setTransitionSize(modelSize);
		Object[] netAndMarkings = generator.generateStochasticNet(config);
		
		StochasticNet net = (StochasticNet) netAndMarkings[0];
		Marking initialMarking = (Marking) netAndMarkings[1];
		Marking finalMarking = (Marking) netAndMarkings[2];
		
//		// add an immediate transition before first transition:
//		Place initPlace = initialMarking.iterator().next();
//		Transition startTransition = net.addImmediateTransition("t_start");
//		startTransition.setInvisible(config.isImmedateTransitionsInvisible());
//		PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge = net.getOutEdges(initPlace).iterator().next();
//		Transition firstTransition = (Transition) outEdge.getTarget();
//		net.removeArc(initPlace, firstTransition);
//		net.addArc(initPlace, startTransition);
//		Place firstPlace = net.addPlace("firstPlace");
//		net.addArc(startTransition, firstPlace);
//		net.addArc(firstPlace, firstTransition);
		
		// generate a sample trace:
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig simConfig = new PNSimulatorConfig(1, timeUnit);
		StochasticNetSemantics semantics = new EfficientStochasticNetSemanticsImpl();
		semantics.initialize(net.getTransitions(), initialMarking);
		long traceStart = 0;
		
		long beforeSimulation = System.currentTimeMillis();
		
		XTrace trace = (XTrace) simulator.simulateOneTrace(net, semantics, simConfig, initialMarking, traceStart, 0, 1, false, finalMarking);
		long traceEnd = XTimeExtension.instance().extractTimestamp(trace.get(trace.size()-1)).getTime();
		traceStart = XTimeExtension.instance().extractTimestamp(trace.get(0)).getTime();
		long monitoringTime = traceStart + (traceEnd - traceStart )/ 2;
		
		long afterSimulation = System.currentTimeMillis();
		System.out.println("Simulating 1 trace took "+(afterSimulation-beforeSimulation)+" ms. ("+modelSize+" transitions, "+type.toString()+")");
		
		XTrace observedSubTrace = StochasticNetUtils.getSubTrace(trace, monitoringTime);
		
		TimePredictor predictor = new TimePredictor(true);
//		StochasticNetUtils.useCache(true);
		// start performance analysis:
		long beforePrediction = System.currentTimeMillis();
		Pair<Double,Double> predictedDurationAndConfidence = predictor.predict(net, observedSubTrace, new Date(monitoringTime), initialMarking);
		double confidenceBandLower = predictedDurationAndConfidence.getFirst()-predictedDurationAndConfidence.getSecond()/2;
		double confidenceBandHigher = predictedDurationAndConfidence.getFirst()+predictedDurationAndConfidence.getSecond()/2;
		System.out.println("predicted "+predictedDurationAndConfidence.getFirst()+" +- "+predictedDurationAndConfidence.getSecond()/2+" around "+((confidenceBandHigher/predictedDurationAndConfidence.getFirst()-1.)*100)+" % of error..");
		long afterPrediction = System.currentTimeMillis();
		long durationOfPrediction = afterPrediction-beforePrediction;
		return durationOfPrediction;
	}
}
