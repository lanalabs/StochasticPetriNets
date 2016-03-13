package org.processmining.tests.plugins.stochasticnet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherPlugin;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionData;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentConfig;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentPlugin;
import org.processmining.plugins.stochasticpetrinet.prediction.experiment.PredictionExperimentResult;

public class OptimalSelectionPredictionTest {


	private static final int FOLDS_IN_EXPERIMENT = 10;
	
	private static final int RANDOM_COUNT = 100;
	
	private static int predictionTypes = 2;

	private Random random;
	
	public OptimalSelectionPredictionTest(){
		this.random = new Random(System.currentTimeMillis());
	}
	
	@Test
	public void testIndices(){
		testN_choose_K(10, 2);
		testN_choose_K(5, 2);
		testN_choose_K(7,3);
		testN_choose_K(8,7);
	}

	public void testN_choose_K(int n, int k) {
		List<List<Integer>> possibilities = fillPossibilities(n,k,0);
		System.out.println("n ("+n+") choose k ("+k+"):");
		System.out.println(possibilities);
		Assert.assertEquals(ArithmeticUtils.binomialCoefficient(n, k), possibilities.size());
	}
	
	public void runAllCombinations() throws Exception{
		Object[] netAndMarking = TestUtils.loadModel("surgery", true);
		XLog log = TestUtils.loadLog("surgery.xes");
		
		Set<String> randomlySelectableTransitions = new HashSet<String>();
		randomlySelectableTransitions.add("Departure of Lock+complete");
		randomlySelectableTransitions.add("Arrival in OR+complete");
		randomlySelectableTransitions.add("Arrival in recovery+complete");
		randomlySelectableTransitions.add("End of induction+complete");
		randomlySelectableTransitions.add("Start of emergence+complete");
		randomlySelectableTransitions.add("Suturation+complete");
		randomlySelectableTransitions.add("End of emergence+complete");
		randomlySelectableTransitions.add("Start of induction+complete");
		randomlySelectableTransitions.add("Departure of OR+complete");
		randomlySelectableTransitions.add("Do antibiotica prophelaxe+complete");
		randomlySelectableTransitions.add("Incision+complete");
		
		String[] mustHaveTransitions = new String[]{"Arrival in Lock+complete", "Departure of recovery+complete"};
		
		for (int i = 0; i <= randomlySelectableTransitions.size(); i++){
			testExperiment(log, netAndMarking, i, mustHaveTransitions, randomlySelectableTransitions);
		}
	}

	public void testExperiment(XLog log, Object[] netAndMarking, int k, String[] mustHaveTransitions, Set<String> selectableTransitions) throws Exception {
		
		
//		String[] selectedTransitions = new String[]{"Incision+complete","Suturation+complete"};
//		String[] selectedTransitions = new String[]{"Suturation+complete"};
//		
//		
//		
//		
//		DescriptiveStatistics meanOptimalAbsoluteErrorStats = predictWithFilter(netAndMarking, log, mustHaveTransitions,
//				selectedTransitions);
		
		// do k random selections and average them
		Map<String[],DescriptiveStatistics[]> predictionErrors = new HashMap<String[],DescriptiveStatistics[]>();
		int n = selectableTransitions.size();
		boolean optimal = false;
		long number = ArithmeticUtils.binomialCoefficient(n, k);
		if (number > 150){
			for (int i = 0; i < RANDOM_COUNT; i++){
				String[] randomlySelected = selectRandomly(selectableTransitions,k);
				predictionErrors.put(randomlySelected, predictWithFilter(netAndMarking, log, mustHaveTransitions,randomlySelected));
			}	
		} else {
			// enumerate solutions (by looking at all, we are guaranteed to have found the optimal one:
			optimal = true;
			List<List<Integer>> possibilities = fillPossibilities(n,k,0);
			String[] selectableTransitionsArray = new String[k];
			selectableTransitionsArray = selectableTransitions.toArray(selectableTransitionsArray);
			for (List<Integer> indices : possibilities){
				String[] enumeratedlySelected = new String[k];
				for (int i = 0; i < indices.size(); i++){
					enumeratedlySelected[i] = selectableTransitionsArray[indices.get(i)];
				}
				predictionErrors.put(enumeratedlySelected, predictWithFilter(netAndMarking, log, mustHaveTransitions,enumeratedlySelected));
			}
		}
		DescriptiveStatistics meanErrors = new DescriptiveStatistics();
		DescriptiveStatistics meanAbsoluteErrors = new DescriptiveStatistics();
		DescriptiveStatistics rootMeanSquareErrors = new DescriptiveStatistics();
		
		String result = "combination;mean errors;mean absolute errors;root mean square errors\n";
		for (String[] configuration : predictionErrors.keySet()){
			DescriptiveStatistics[] results = predictionErrors.get(configuration);
			result += Arrays.toString(configuration)+";"+results[0].getMean()+";"+results[1].getMean()+";"+Math.sqrt(results[2].getMean())+"\n";
			meanErrors.addValue(results[0].getMean());
			meanAbsoluteErrors.addValue(results[1].getMean());
			rootMeanSquareErrors.addValue(Math.sqrt(results[2].getMean()));
		}
		
		System.out.println("optimal value: "+ meanAbsoluteErrors.getMin());
		System.out.println("worst value: "+ meanAbsoluteErrors.getMax());
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File("result_surgery3_"+k+(optimal?"_optimal":"_random")+".csv")));
		writer.write(result);
		writer.flush();
		writer.close();
		
		System.out.println(result);
		
		System.out.println("mean value of random selections: "+ meanAbsoluteErrors.getMean());
		
		System.out.println("optimal yields errors of "+(meanAbsoluteErrors.getMin()/meanAbsoluteErrors.getMean()*100)+" size of the random average");
		
		
	}

	private List<List<Integer>> fillPossibilities(int n, int k, int currentMax) {
		List<List<Integer>> possibilities = new LinkedList<List<Integer>>();
		if (k > 0){
			for (int i = currentMax; i <= n-k; i++){
				List<List<Integer>> remaining = fillPossibilities(n, k-1, i+1);
				for (List<Integer> remainder : remaining){
					remainder.add(0, i);
					possibilities.add(remainder);
				}
			}
		} else {
			possibilities.add(new LinkedList<Integer>());
		}
		return possibilities;
	}

	private String[] selectRandomly(Set<String> randomlySelectableTransitions, int length) {
		List<String> copy = new LinkedList<String>(randomlySelectableTransitions);
		
		String[] randomSelection = new String[length];
		for (int i = 0; i < length; i++){
			randomSelection[i] = copy.remove(random.nextInt(copy.size()));
		}
		return randomSelection;
	}

	public DescriptiveStatistics[] predictWithFilter(Object[] netAndMarking, XLog log, String[] mustHaveTransitions,
			String[] selectedTransitions) {
		String[] availableTransitions = new String[mustHaveTransitions.length+selectedTransitions.length];
		for (int i = 0; i < availableTransitions.length; i++){
			availableTransitions[i] = (i < mustHaveTransitions.length) ? mustHaveTransitions[i] : selectedTransitions[i-mustHaveTransitions.length];
		}
		
		
		List<String> availableTransitionsList = new LinkedList<String>();
		availableTransitionsList.addAll(Arrays.asList(availableTransitions));

		
		/**
		 * Departure of Lock+complete
		 * Arrival in OR+complete
		 * Patient ordered+complete
		 * Arrival in recovery+complete
		 * Start+complete
		 * End of induction+complete
		 * Start of emergence+complete
		 * Suturation+complete
		 * End of emergence+complete
		 * Departure of recovery+complete
		 * Start of induction+complete
		 * Departure of OR+complete
		 * Arrival in Lock+complete
		 * End+complete
		 * Do antibiotica prophelaxe+complete
		 * Incision+complete
		 */
		
		XEventClassifier classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
		System.out.println("--> Event classes -->");
		XEventClasses eventClasses = logInfo.getEventClasses();
		List<XEventClass> classes = new LinkedList<XEventClass>(eventClasses.getClasses());
		for (XEventClass eClass: classes){
			System.out.println(eClass.getId());
		}
		System.out.println("<-- Event classes <--\n");
		
		
		Petrinet net = (Petrinet) netAndMarking[0];
		List<Transition> transitions = new LinkedList<Transition>(net.getTransitions());
		System.out.println("--> Transitions -->");
		for (Transition transition : transitions){
			System.out.println(transition.getLabel());
		}
		System.out.println("<-- Transitions <--\n");
		
		// collect average prediction error (take 40 periodic predictions)
		
		
		// select subset of events and set visibility of transitions accordingly
		Map<XEventClass,Transition> eventTransitionMappings = new HashMap<XEventClass, Transition>(); 
		for (Transition transition : transitions){
			String label = transition.getLabel();
			XEventClass match = null;
			for (XEventClass eClass: classes){
				String classLabel = eClass.getId();
				if (classLabel.startsWith(label)){
					match = eClass;
					break;
				}
			}
			if (match != null){
				eventTransitionMappings.put(match, transition);
			}
		}
		// filter log
		XLog optimallyfilteredLog = TestUtils.filter(log, classifier, availableTransitions);
		// update visibility of transitions
		for (Transition transition : net.getTransitions()){
			String label = transition.getLabel();
			boolean isInvisible = true;
			for (String availableTransition : availableTransitions){
				if (availableTransition.startsWith(label)){
					if (availableTransition.equalsIgnoreCase(label) || availableTransition.substring(0, availableTransition.length()-9).equalsIgnoreCase(label)){
						isInvisible = false;
					}
				}
			}
			transition.setInvisible(isInvisible);
		}
		
		// perform prediction experiment
		PredictionExperimentPlugin experimentPlugin = new PredictionExperimentPlugin();
		
		PredictionExperimentConfig config = new PredictionExperimentConfig();
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setLearnedDistributionType(DistributionType.HISTOGRAM);
		config.setLearnSPNFromData(true);
		config.setMonitoringIterations(40);
		config.setResultFileName("gspn_comparison_result_40_histogram.csv");
		config.setTimeUnitFactor(TimeUnit.HOURS);
		config.setWorkerCount(1);
		
		PredictionExperimentResult result = predictWithConfig(StochasticNetUtils.getDummyConsoleProgressContext(), net, optimallyfilteredLog, config);
		
		DescriptiveStatistics meanErrorStats = new DescriptiveStatistics();
		DescriptiveStatistics meanAbsoluteErrorStats = new DescriptiveStatistics();
		DescriptiveStatistics squareErrorStats = new DescriptiveStatistics();
		long errorCount = 0;
		// iterate through cases
		for (List<PredictionData> predictionsByCase: result.getPredictionResults()){

			// iterate through monitoring/prediction iterations
			for (PredictionData predictions : predictionsByCase){
				Date predictionDate = predictions.getPredictionDates()[1]; // use only constrained prediction
				
				if (predictionDate != null){
					long predictionError = predictionDate.getTime()-predictions.getCaseEndDate().getTime();
					meanErrorStats.addValue(predictionError / config.getTimeUnitFactor().getUnitFactorToMillis());	
					meanAbsoluteErrorStats.addValue(Math.abs(predictionError) / config.getTimeUnitFactor().getUnitFactorToMillis());
					squareErrorStats.addValue(Math.pow(predictionError/config.getTimeUnitFactor().getUnitFactorToMillis(),2));
					
				} else {
					errorCount++;
				}
			}
		}
		System.out.println("------------");
		System.out.println("Finished prediction with the transitions: ");
		System.out.println(Arrays.toString(availableTransitions));
		System.out.println("mean error = \t"+meanErrorStats.getMean());
		System.out.println("mean absolute error = \t"+meanAbsoluteErrorStats.getMean());
		System.out.println("root mean square error = \t"+Math.sqrt(squareErrorStats.getMean()));
		System.out.println("error count = "+errorCount);
		System.out.println("------------");
		return new DescriptiveStatistics[]{meanErrorStats,meanAbsoluteErrorStats,squareErrorStats};
	}
	
	private PredictionExperimentResult predictWithConfig(PluginContext context,
			Petrinet model, XLog log, PredictionExperimentConfig config) {
		PredictionExperimentResult result = new PredictionExperimentResult();
		result.setPredictionResults(new LinkedList<List<PredictionData>>());
		// 10-fold validation:
		for (int kFold = 0; kFold < FOLDS_IN_EXPERIMENT; kFold++){
			XLog trainingLog = StochasticNetUtils.filterTracesBasedOnModulo(log, FOLDS_IN_EXPERIMENT, kFold, false);
			XLog predictionLog = StochasticNetUtils.filterTracesBasedOnModulo(log, FOLDS_IN_EXPERIMENT, kFold, true);

			// mine stochastic Petri Net from training Log (9/10 of original traces)
			StochasticNet stochasticNet = null;
			Marking finalPlainMarking = StochasticNetUtils.getFinalMarking(context, model);
			Marking finalStochasticMarking = null;
			Marking initialStochasticMarking = null;
			
				Manifest manifest = (Manifest)StochasticNetUtils.replayLog(context, model, trainingLog, true, true);
				PerformanceEnricherConfig performanceEnricherConfig = new PerformanceEnricherConfig(config.getLearnedDistributionType(), config.getTimeUnitFactor(),config.getExecutionPolicy(), null);
				Object[] netAndMarking = PerformanceEnricherPlugin.transform(context, manifest, performanceEnricherConfig);
				performanceEnricherConfig.setType(DistributionType.EXPONENTIAL);
				Object[] exponentialNetAndMarking = PerformanceEnricherPlugin.transform(context, manifest, performanceEnricherConfig);
				stochasticNet = (StochasticNet) netAndMarking[0];
				Marking initialExponentialNetMarking = (Marking) exponentialNetAndMarking[1];
				stochasticNet.getAttributeMap().put(StochasticNetUtils.ITERATION_KEY, kFold);
				
				initialStochasticMarking = (Marking) netAndMarking[1];
				StochasticNetUtils.cacheInitialMarking(stochasticNet, initialStochasticMarking);
				
				finalStochasticMarking = new Marking();
				Marking finalExponentialNetMarking = new Marking();
				for (Place p : stochasticNet.getPlaces()){
					for (Place markingPlace : finalPlainMarking){
						if (p.getLabel().equals(markingPlace.getLabel())){
							finalStochasticMarking.add(p);
							finalExponentialNetMarking.add(p);
						}
					}
				}
				StochasticNetUtils.cacheFinalMarking(stochasticNet, finalStochasticMarking);
				if (context != null){
					context.addConnection(new FinalMarkingConnection(stochasticNet, finalStochasticMarking));
				}
			double meanDurationMillies = StochasticNetUtils.getMeanDuration(trainingLog);
			
			PredictionExperimentResult predictions = predict(context, stochasticNet, predictionLog, config, meanDurationMillies);
			result.getPredictionResults().addAll(predictions.getPredictionResults());
		}
		return result;
	}

	private PredictionExperimentResult predict(PluginContext context, StochasticNet model, XLog log, PredictionExperimentConfig config, double meanDuration){
		PredictionExperimentResult result = new PredictionExperimentResult();
		List<List<PredictionData>> predictionsAndRealValues = new ArrayList<List<PredictionData>>();
		
		if (context != null){
			context.getProgress().setIndeterminate(true);
			context.getProgress().setMinimum(0);
			context.getProgress().setValue(0);
			context.getProgress().setMaximum(log.size());
			context.getProgress().setIndeterminate(false);
		}
			
		
		// prediction period is such that we do equally distributed monitoring iterations half before and half after the process mean duration
		Double predictionPeriod = (meanDuration/config.getMonitoringIterations())*2;
		
		PredictionExperimentWorker[] workers = new PredictionExperimentWorker[config.getWorkerCount()];
		
		ExecutorService executor = Executors.newFixedThreadPool(workers.length);
		int traceId = 0;
		for (int i = 0; i < workers.length; i++){
			XLog testLog = StochasticNetUtils.filterTracesBasedOnModulo(log, workers.length, i, true);
			workers[i] = new PredictionExperimentWorker(context, testLog, config, predictionPeriod, model, meanDuration, traceId, log.size());
			traceId += testLog.size();
			
			executor.execute(workers[i]);
		}
		executor.shutdown();
		while (!executor.isTerminated()){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
			}
		}
		for (int i = 0; i < workers.length; i++){
			predictionsAndRealValues.addAll(workers[i].predictionsAndRealValues);
		}
		result.setPredictionResults(predictionsAndRealValues);
		
		return result;
	}
	
	private class PredictionExperimentWorker implements Runnable{

		private XLog log;
		private PluginContext context;
		private PredictionExperimentConfig config;
		private Double predictionPeriod;
		private Marking initialMarking;
		private TimePredictor predictor;
		private StochasticNet model;
		private Double meanDuration;
		private int traceId;
		private int traceCount;
		
		public List<List<PredictionData>> predictionsAndRealValues = new ArrayList<List<PredictionData>>();
		
		
		
		public PredictionExperimentWorker(PluginContext context, 
				XLog testLog, 
				PredictionExperimentConfig config, 
				Double predictionPeriod, 
				StochasticNet model,
				Double meanDuration,
				int traceId,
				int traceCount){
			this.context = context;
			this.log = testLog;
			this.config = config;
			this.predictionPeriod = predictionPeriod;
			this.initialMarking = StochasticNetUtils.getInitialMarking(context, model);
			this.predictor = new TimePredictor(true);
			this.model = model;
			this.meanDuration = meanDuration;
			this.traceId = traceId;
			this.traceCount = traceCount;
		}
		
		public void run() {
			double pos = 0;
			long start = System.currentTimeMillis();
			NumberFormat format = NumberFormat.getPercentInstance();
			DateFormat formatter = new SimpleDateFormat("HH:mm:ss",Locale.ROOT);
			String estimate = "(unknown)";
			for (XTrace trace : log){
				traceId++;
				if (pos > 0){
					long now = System.currentTimeMillis();
					long passed = now-start;
					long passedMilliesPerIteration = passed / (int)pos;
					long estimated = passedMilliesPerIteration*(log.size()-(int)pos);
					Date date = new Date(estimated);
					estimate = formatter.format(date);
				}
				String percent = format.format(pos++/log.size());
				String s="Finished prediction of trace "+(traceId)+" of "+traceCount+" ("+percent+") rem. time:"+estimate;
				if (context != null){
					context.getProgress().inc();
//					context.log("Starting prediction of trace "+traceId+"...");
					context.log("Starting prediction of trace "+(traceId)+" of "+traceCount+" ("+percent+")... rem. time:"+estimate);
				}
				if (trace.size() >= 2){
					List<PredictionData> predictionsAndRealValuesForThisTrace = new ArrayList<PredictionData>(); 
					XEvent event = trace.get(trace.size()-1);
					
					Long realTerminationTime = XTimeExtension.instance().extractTimestamp(event).getTime(); 
					
					event = trace.get(0);
					Long realStartTime = XTimeExtension.instance().extractTimestamp(event).getTime();
					
					for (int i = 0; i < config.getMonitoringIterations(); i++){
						Date currentTime = new Date(realStartTime+i*predictionPeriod.longValue());
						Double realRemainingDuration = (double)(realTerminationTime-currentTime.getTime());
						if (realRemainingDuration >= 0){
							s+=".";
							XTrace subTrace = StochasticNetUtils.getSubTrace(trace, currentTime.getTime());
							
							event = subTrace.get(subTrace.size()-1);
					
							Pair<Double,Double> constrainedPredictionAndConfidence = predictor.predict(model, subTrace, currentTime, initialMarking, false);
							
							Double predictedValueConstrained = (double) Math.max(constrainedPredictionAndConfidence.getFirst().longValue(),currentTime.getTime());
							
							PredictionData predictions = new PredictionData(traceId, new Date(realStartTime), new Date(realTerminationTime), new Date(currentTime.getTime()),predictionTypes);
							predictions.getPredictionDates()[0] = new Date(Math.max((long)(realStartTime+meanDuration), currentTime.getTime()));
							predictions.getPredictionDates()[1] = new Date(predictedValueConstrained.longValue());
							predictionsAndRealValuesForThisTrace.add(predictions);
						}
					}
//					System.out.println(s);
					predictionsAndRealValues.add(predictionsAndRealValuesForThisTrace);
				} else {
					System.out.println("Trace "+trace+" has less than 2 events!");
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		OptimalSelectionPredictionTest test = new OptimalSelectionPredictionTest();
		test.runAllCombinations();
	}
}
