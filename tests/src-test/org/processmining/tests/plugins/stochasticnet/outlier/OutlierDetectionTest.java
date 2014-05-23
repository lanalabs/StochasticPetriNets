package org.processmining.tests.plugins.stochasticnet.outlier;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.deckfour.xes.model.XLog;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.framework.packages.PackageManager;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsAnalyzer;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;
import org.processmining.plugins.stochasticpetrinet.analyzer.LikelihoodAnalyzer;
import org.processmining.plugins.stochasticpetrinet.analyzer.ReplayStep;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.numeric.FastDensityFunction;
import org.processmining.plugins.stochasticpetrinet.distribution.numeric.ReplayStepDensityFunction;
import org.processmining.plugins.stochasticpetrinet.generator.Generator;
import org.processmining.plugins.stochasticpetrinet.generator.GeneratorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class OutlierDetectionTest {
	
	private static Random rand = new Random(100);
	
	/**
	 * The error rates to check in the experiment
	 */
	private static final int[] ERROR_RATES = new int[]{0,1,2,4,6,8,10,12,14,16,18,20};
	//private static final int[] ERROR_RATES = new int[]{5};

	public enum ErrorType{
		NONE, STRUCTURAL_ERROR, TIME_ERROR, ERROR_NEIGHBOR;
	}
	
	@Test
	public void testSimpleSequence() throws Exception{
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/simpleNet_3_activities.pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);
		List<PNMLNet> nets = pnml.getNet();
		PNMLNet net = nets.get(0);
		
		String netId = net.getId();
		Assert.assertEquals("testNet", netId);
		
		StochasticNetDeserializer deserializer = new StochasticNetDeserializer();
		Object[] netAndMarking = deserializer.convertToNet(null, pnml, null, false);
		StochasticNet stochasticNet = (StochasticNet) netAndMarking[0];
		
		PNSimulator sim = new PNSimulator();
		XLog log = sim.simulate(null,stochasticNet, StochasticNetUtils.getSemantics(stochasticNet), new PNSimulatorConfig(1), StochasticNetUtils.getInitialMarking(null, stochasticNet));
		
		CaseStatisticsList csl = LikelihoodAnalyzer.getLogLikelihoods(null, log, stochasticNet);
		
		CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(stochasticNet, StochasticNetUtils.getInitialMarking(null, stochasticNet), csl);
		ReplayStep step = csl.get(0).getReplaySteps().get(2);
		List<ReplayStep> steps = analyzer.getIndividualOutlierSteps(csl.get(0));
		
		boolean likelytoBeError = analyzer.isOutlierLikelyToBeAnError(step);
		System.out.println(likelytoBeError);
		
		double error = 5;
		shiftStep(step, error);
		System.out.println("\n...after adding a +"+error+":\n");
		
		likelytoBeError = analyzer.isOutlierLikelyToBeAnError(step);
		System.out.println(likelytoBeError);
		
//		XLog logWithError = (XLog) log.clone();
//		XTrace trace = logWithError.get(0);
//		XEvent e = trace.get(trace.size()-2);
//		XTimeExtension.instance().assignTimestamp(e, XTimeExtension.instance().extractTimestamp(e).getTime()+(long)(5*stochasticNet.getTimeUnit().getUnitFactorToMillis()));
//		
//		CaseStatisticsList csl2 = LikelihoodAnalyzer.getLogLikelihoods(null, logWithError, stochasticNet);
		
		// net containing three activities in sequence A,B,C
		
//		ReplayStep step1 = new ReplayStep(transition, duration, density, predecessorSteps)
	}

	static boolean shiftStep(ReplayStep step, double error) {
		step.duration += error;
		boolean affectsOrdering = step.duration < 0; // if the duration becomes negative, we know a reordering happened
		
		step.density = step.transition.getDistribution().density(step.duration);
		for (ReplayStep childStep : step.children){
			childStep.duration -= error;
			affectsOrdering = affectsOrdering || childStep.duration < 0; 
			// if the duration of the child becomes negative, this event was moved past the child in the ordering (that's easy to detect with an alignment)
			childStep.density = childStep.transition.getDistribution().density(childStep.duration);
		}
		return affectsOrdering;
	}
	
	@Test
	public void testPValue(){
		// timed transition with normal distribution mean=10, sd=1:
		TimedTransition tt = new TimedTransition("A", null,null,1,1,DistributionType.NORMAL,10,1);
		
		ReplayStep step = new ReplayStep(tt,9,tt.getDistribution().density(7),new HashSet<ReplayStep>());
		CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer();
		double p = analyzer.getPValueOfStepIntegral(step);
		double pApprox = analyzer.computePValueByApproximateIntegration(step);
		System.out.println("p-value of getting a "+step.duration+" under the assumption of a normal distribution with mean 10 and sd 1:");
		System.out.println("exact: "+p);
		System.out.println("approximate:"+pApprox);
		Assert.assertEquals(p, pApprox, 0.1);
	}
	
	@Test
	public void testExperiment(){
		// parameters: log size and model size, failure rate
		// Assumptions: training data with / without outliers
		int traceSize = 10000;
		
		DistributionType[] types = new DistributionType[]{DistributionType.NORMAL, DistributionType.EXPONENTIAL};
		
		for (DistributionType type : types){
			int[] modelSizes = new int[]{1,2,5,10,20,50,100};
			for (int size : modelSizes){
				runExperiment(traceSize/size, size, type);
			}
		}
	}

	private void runExperiment(int traceSize, int activityCount, DistributionType type) {
		
		String experimentName = activityCount+"_activities_"+type.toString(); 
		Generator generator = new Generator(1);
		
		GeneratorConfig config = new GeneratorConfig();
		config.setContainsLoops(false);
		config.setDegreeOfLoops(0);
		config.setDegreeOfExclusiveChoices(1); // 10 %
		config.setDegreeOfSequences(6);  // 60%
		config.setDegreeOfParallelism(3);  // 30%
		config.setDistributionType(type);
		config.setTimeUnit(TimeUnit.MINUTES);
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setTransitionSize(activityCount);
		config.setCreateDedicatedImmediateStartTransition(true);
		
		Object[] netAndMarking = generator.generateStochasticNet(config);
		StochasticNet stochasticNet = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1]; 
		
		PnmlExportStochasticNet exporter = new PnmlExportStochasticNet();
		try {
			exporter.exportPetriNetToPNMLFile(null, stochasticNet, new File("/home/andi/WorkspaceTex/TimeAnomalyBPM/experiment/model_"+experimentName+".pnml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		/** Shut off auto update - it is very slow and runs each time*/
		PackageManager.getInstance().setAutoUpdate(false);
		
		
		PNSimulator sim = new PNSimulator();
		XLog log = sim.simulate(null,stochasticNet, StochasticNetUtils.getSemantics(stochasticNet), new PNSimulatorConfig(traceSize,stochasticNet), initialMarking);
		
		List<RetrievalStats> ourStatsList = new ArrayList<RetrievalStats>();
		List<RetrievalStats> ourStatsNeighborsList = new ArrayList<RetrievalStats>();
		List<RetrievalStats> ourStatsRelaxedList = new ArrayList<RetrievalStats>();
		List<RetrievalStats> simpleZStatsList = new ArrayList<RetrievalStats>();
		List<RetrievalStats> structuredStatsList = new ArrayList<RetrievalStats>();
		
		List<Object[]> sortedRanking = new ArrayList<Object[]>();
		
		for (int errorPercentage : ERROR_RATES){
			
			CaseStatisticsList csl = LikelihoodAnalyzer.getLogLikelihoods(null, log, stochasticNet);
			CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(stochasticNet, initialMarking, csl);
			
			
			System.out.println("analyzing generated log with outlier rate: "+analyzer.getOutlierRate());
			
			int totalOutlierCount = 0;
			int totalErrorCount = 0;
			for (CaseStatistics cs : csl){
				List<ReplayStep> outlierSteps = analyzer.getIndividualOutlierSteps(cs);
				totalOutlierCount += outlierSteps.size();
				for (ReplayStep step : outlierSteps){
					if (analyzer.isOutlierLikelyToBeAnError(step)){
						totalErrorCount++;
					}
				}
			}
			
			System.out.println("outliers: "+totalOutlierCount+", out of which "+totalErrorCount+" are identified as measurement errors.");
			
			System.out.println("inserting "+errorPercentage+"% noise.");
			
			Map<ReplayStep, ErrorType> errorLabel = new HashMap<ReplayStep, ErrorType>();
			
			int errorCount = 0;
			int errorsAffectingOrder = 0;
			for (CaseStatistics cs : csl){
				double caseDuration = cs.getCaseDuration();
				for (ReplayStep rs : cs.getReplaySteps()){
					errorLabel.put(rs, ErrorType.NONE);
					if (rs.transition.getDistributionType().equals(DistributionType.IMMEDIATE)){
						// ignore first replay step
					} else if (rand.nextDouble() <= errorPercentage/100.){
						// insert error:
						//double error = (rand.nextDouble()-0.5)*caseDuration/6.;
						double error = (rand.nextGaussian())*caseDuration/6.;
						boolean affectingOrder = shiftStep(rs, error);
						if (affectingOrder){
							errorsAffectingOrder++;
						}
						errorLabel.put(rs, (affectingOrder?ErrorType.STRUCTURAL_ERROR:ErrorType.TIME_ERROR));
						errorCount++;
					}
				}
			}
			System.out.println("inserted "+errorCount+" errors, out of which "+errorsAffectingOrder+" affect the order (easily detectable).");
			System.out.println("done.");
			
			RetrievalStats ourStatsStrict = new RetrievalStats();
			ourStatsStrict.errorRate = errorPercentage;
			RetrievalStats ourStatsAlsoOutliers = new RetrievalStats();
			ourStatsAlsoOutliers.errorRate = errorPercentage;
			RetrievalStats ourStatsNeighbors = new RetrievalStats();
			ourStatsNeighbors.errorRate = errorPercentage;
			RetrievalStats structuralStats = new RetrievalStats();
			structuralStats.errorRate = errorPercentage;
			RetrievalStats simpleZStats = new RetrievalStats();
			simpleZStats.errorRate = errorPercentage;
	
			// update analyzer:
			analyzer.updateStatistics(analyzer.getOutlierRate());
			for (CaseStatistics cs : csl){
				List<ReplayStep> outlierSteps = analyzer.getIndividualOutlierSteps(cs);
				totalOutlierCount += outlierSteps.size();
				
				Map<ReplayStep,Boolean> isOutlierError = new HashMap<ReplayStep, Boolean>();
				for (ReplayStep step : cs.getReplaySteps()){
					isOutlierError.put(step, outlierSteps.contains(step) && analyzer.isOutlierLikelyToBeAnError(step));
				}
				for (ReplayStep step : cs.getReplaySteps()){
					if (step.transition.getDistributionType().equals(DistributionType.IMMEDIATE)){
						
					} else {
						boolean isOutlier = outlierSteps.contains(step);
						boolean isStrictError = isOutlierError.get(step);
						
						// error
						ErrorType errorType = ErrorType.NONE;
						if (!errorLabel.containsKey(step)){
							System.out.println("Should be in there! Debug me!");
						} else {
							errorType = errorLabel.get(step);
						}
						if (errorType.equals(ErrorType.STRUCTURAL_ERROR)){
							// assume we can use structural means to detect this (alignments / conformance stuff)
							ourStatsStrict.tp++;
							ourStatsAlsoOutliers.tp++;
							ourStatsNeighbors.tp++;
							structuralStats.tp++;
							simpleZStats.tp++;
							if (errorPercentage == ERROR_RATES[6]){
								sortedRanking.add(new Object[]{new Double(1.1), step, errorType});
							}
						} else {
							if (errorPercentage == ERROR_RATES[6]){
								sortedRanking.add(new Object[]{new Double(1-analyzer.getPValueOfStepIntegral(step)), step, errorType});	
							}
							
							// time errors and no errors:
							if (errorType.equals(ErrorType.TIME_ERROR)){
								structuralStats.fn++;
							} else {
								// no error
								structuralStats.tn++;
							}
							handleOutlierStats(ourStatsAlsoOutliers, isOutlier, errorType);
							handleSimpleZStats(simpleZStats, step, errorType);
							boolean isOutlierAndStrictError = isOutlier && isStrictError;
							handleOutlierStats(ourStatsStrict, isOutlierAndStrictError, errorType);
							
							boolean isNeighborAnError = isNeighborAnError(isOutlierError, step);
							handleNeighborStats(ourStatsNeighbors, isOutlierAndStrictError, errorType, isNeighborAnError);
						}
					}
				}
			}
			ourStatsList.add(ourStatsStrict);
			ourStatsRelaxedList.add(ourStatsAlsoOutliers);
			simpleZStatsList.add(simpleZStats);
			ourStatsNeighborsList.add(ourStatsNeighbors);
			structuredStatsList.add(structuralStats);
			
		}
		Collections.sort(sortedRanking, new Comparator<Object[]>(){
			public int compare(Object[] o1, Object[] o2) {
				return ((Double)o1[0]).compareTo((Double)o2[0]);
			}
		});
		
		printRetrievalStats(ourStatsList, "ourStats: (only \"real\" errors)");
		printRetrievalStats(ourStatsNeighborsList, "ourStats: (neighbors count too)");
		printRetrievalStats(ourStatsRelaxedList, "ourStats (outliers only)");
		printRetrievalStats(structuredStatsList, "structured Stats: (baseline)");
		printRetrievalStats(simpleZStatsList, "simple Z-value outlier detection statistic:");
		
		
		System.out.println("\n--------------------");
		
		File file = new File("/home/andi/WorkspaceTex/TimeAnomalyBPM/experiment/roc_"+experimentName+".dat");
		try {
			printRocDataToWriter(sortedRanking, new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void testFunctions(){
		Generator generator = new Generator(1);
		
		GeneratorConfig config = new GeneratorConfig();
		config.setContainsLoops(false);
		config.setDegreeOfLoops(0);
		config.setDegreeOfExclusiveChoices(0); // 10 %
		config.setDegreeOfSequences(6);  // 60%
		config.setDegreeOfParallelism(0);  // 30%
		config.setDistributionType(DistributionType.NORMAL);
		config.setTimeUnit(TimeUnit.MINUTES);
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setTransitionSize(2);
		config.setCreateDedicatedImmediateStartTransition(true);
		
		Object[] netAndMarking = generator.generateStochasticNet(config);
		StochasticNet stochasticNet = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1]; 
		
		PNSimulator sim = new PNSimulator();
		XLog log = sim.simulate(null,stochasticNet, StochasticNetUtils.getSemantics(stochasticNet), new PNSimulatorConfig(1,stochasticNet), initialMarking);
		CaseStatisticsList csl = LikelihoodAnalyzer.getLogLikelihoods(null, log, stochasticNet);
		
		CaseStatistics cs = csl.get(0);
		ReplayStep rs = cs.getReplaySteps().get(0);
		ReplayStep rs1 = cs.getReplaySteps().get(1);
		ReplayStep rs2 = cs.getReplaySteps().get(2);
		
		CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(stochasticNet, initialMarking, csl);
		analyzer.isOutlierLikelyToBeAnError(rs1);
		
		Assert.assertEquals(1,rs1.children.size());
		
		UnivariateFunction function = new FastDensityFunction(rs1, rs1.children.iterator().next());
		UnivariateFunction f2 = new ReplayStepDensityFunction(rs1);
	}

	private void printRocDataToWriter(List<Object[]> sortedRanking, Writer writer) {
		BufferedWriter bw = new BufferedWriter(writer);
		try {
			bw.write("p-value;transition;label\n");
			for (Object[] element : sortedRanking){
				bw.write(element[0]+";"+((ReplayStep)element[1]).transition.getLabel()+";"+(!((ErrorType)element[2]).equals(ErrorType.NONE)?"T":"F")+"\n");
			}
			bw.flush();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	@Test
	public void testOutlierCategory(){
		int SAMPLE_SIZE = 10000;
		double threshold = 0.0027;
		NormalDistribution nDist = new NormalDistribution(10, 1);
		double[] samples = nDist.sample(SAMPLE_SIZE);
		int outliers = 0;
		int outliersLogLikelihood = 0;
		for (double s : samples) {
			double zValue = (s-nDist.getMean())/nDist.getStandardDeviation();
			outliers = outliers + (Math.abs(zValue) > 3?1:0);
		}
		
		double[] loglikelihoods = new double[samples.length];
		for (int i = 0; i < samples.length; i++){
			loglikelihoods[i] = Math.log(nDist.density(nDist.sample()));
		}
		GaussianKernelDistribution logLikelihoodDistribution = new GaussianKernelDistribution();
		logLikelihoodDistribution.addValues(loglikelihoods);
		
		double index = threshold * SAMPLE_SIZE;
		double remainder = index-Math.floor(index);
		Arrays.sort(loglikelihoods);
		double logLikelihoodAtCutoff = (1-remainder) * loglikelihoods[(int)Math.ceil(index)]
		                              + (remainder) * loglikelihoods[(int)Math.floor(index)];
		for (double s : samples) {
			double lValue = Math.log(nDist.density(s));
			outliersLogLikelihood = outliersLogLikelihood + (lValue < logLikelihoodAtCutoff?1:0);
		}
		System.out.println("Outliers (Z-value) = "+outliers+" (expected 0.27% = "+0.0027*SAMPLE_SIZE+")");
		
		System.out.println("Outliers (LogLikelihood) = "+outliersLogLikelihood+" (expected 1% of "+SAMPLE_SIZE+" = "+(threshold*SAMPLE_SIZE)+")");
		
	}
	
	

	private void handleNeighborStats(RetrievalStats stats, boolean isError, ErrorType errorType,
			boolean isNeighborAnError) {
		if (isError){
			// true
			if (errorType.equals(ErrorType.TIME_ERROR)){
				stats.tp++; // correctly detected
			} else {
				assert(errorType.equals(ErrorType.NONE));
				if (isNeighborAnError){
					stats.tn++; // correctly detected (detected the neighbor)
				} else {
					stats.fp++; // should not have been detected
				}
			}
		} else {
			// false
			if (errorType.equals(ErrorType.TIME_ERROR)){
				if (isNeighborAnError){
					stats.tp++; // neighbor indicates that there is a problem here
				} else {
					stats.fn++; // should have been detected
				}
			} else {
				assert(errorType.equals(ErrorType.NONE));
				stats.tn++; // correctly not detected
			}
		}
	}

	private void printRetrievalStats(List<RetrievalStats> ourStatsList, String name) {
		System.out.println("\n--------------------");
		System.out.println(name+"\n--------------------");
		System.out.println(ourStatsList.get(0).getHeader());
		for (RetrievalStats ourStats : ourStatsList){
			System.out.println(ourStats.toString());	
		}
	}

	private void handleSimpleZStats(RetrievalStats simpleZStats, ReplayStep step, ErrorType errorType) {
		if (step.transition.getDistributionType().equals(DistributionType.NORMAL)){
			NormalDistribution nDist = (NormalDistribution) step.transition.getDistribution();
			double zValue = (step.duration-nDist.getMean())/nDist.getStandardDeviation();
			handleOutlierStats(simpleZStats, Math.abs(zValue) > 3, errorType);
		}
	}

	private void handleOutlierStats(RetrievalStats stats, boolean isOutlier, ErrorType errorType) {
		handleNeighborStats(stats, isOutlier, errorType, false);
	}

	private boolean isNeighborAnError(Map<ReplayStep, Boolean> isOutlierError, ReplayStep step) {
		boolean neighborIsError = false;
		for (ReplayStep rs : step.parents){
			neighborIsError = neighborIsError || isOutlierError.get(rs);
		}
		for (ReplayStep rs : step.children){
			neighborIsError = neighborIsError || isOutlierError.get(rs);
		}
		return neighborIsError;
	}
	
	class RetrievalStats{
		public static final String SEPARATOR = ";";
		public int errorRate = 1;
		
		/** true positives */
		public int tp = 0; 
		/** false positives */
		public int fp = 0;
		/** true negatives */
		public int tn = 0;
		/** false negatives */
		public int fn = 0;
		
		public int getCount(){
			return tp+fp+tn+fn;
		}
		
		public double getPrecision(){
			return tp / (double)(tp + fp);
		}
		public double getRecall(){
			return tp / (double)(tp + fn);
		}
		public double getAccuracy(){
			return (tp+tn)/(double)(getCount());
		}
		
		public String toString(){
			return errorRate+SEPARATOR+getPrecision()+SEPARATOR+getRecall()+SEPARATOR+getAccuracy()+SEPARATOR+tp+SEPARATOR+tn+SEPARATOR+fp+SEPARATOR+fn;	
		}
		public String getHeader(){
			return "error-rate"+SEPARATOR+"precision"+SEPARATOR+"recall"+SEPARATOR+"accuracy"+SEPARATOR+"tp"+SEPARATOR+"tn"+SEPARATOR+"fp"+SEPARATOR+"fn";
		}
	}
}
