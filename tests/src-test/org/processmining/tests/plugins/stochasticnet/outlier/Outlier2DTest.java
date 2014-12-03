package org.processmining.tests.plugins.stochasticnet.outlier;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import junit.framework.Assert;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.util.Pair;
import org.deckfour.xes.model.XLog;
import org.junit.Ignore;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsAnalyzer;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;
import org.processmining.plugins.stochasticpetrinet.analyzer.LikelihoodAnalyzer;
import org.processmining.plugins.stochasticpetrinet.analyzer.ReplayStep;
import org.processmining.plugins.stochasticpetrinet.distribution.numeric.ConvolutionHelper;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherPlugin;
import org.processmining.plugins.stochasticpetrinet.generator.Generator;
import org.processmining.plugins.stochasticpetrinet.generator.GeneratorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.tests.plugins.stochasticnet.TestUtils;
import org.processmining.tests.plugins.stochasticnet.outlier.OutlierDetectionTest.ErrorType;

public class Outlier2DTest {

	private Random rand = new Random();
	
	private static final int K_FOLDS = 10;
	
	
	@Test
	public void testTwoActivities() throws Exception {
		String experimentName = "2_joint_normals"; 
		Generator generator = new Generator(1);
		
		GeneratorConfig config = new GeneratorConfig();
		config.setContainsLoops(false);
		config.setDegreeOfLoops(0);
		config.setDegreeOfExclusiveChoices(0); // 10 %
		config.setDegreeOfSequences(1);  // 60%
		config.setDegreeOfParallelism(0);  // 30%
		config.setDistributionType(DistributionType.NORMAL);
		config.setTimeUnit(TimeUnit.MINUTES);
		config.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		config.setTransitionSize(2);
		config.setCreateDedicatedImmediateStartTransition(true);
		
		Object[] netAndMarking = generator.generateStochasticNet(config);
		StochasticNet stochasticNet = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		List<Pair<Double, Double>> parameters = new LinkedList<Pair<Double,Double>>();
		parameters.add(new Pair(new Double(10),new Double(2)));
		parameters.add(new Pair(new Double(5),new Double(1)));
		Iterator<Transition> iter = stochasticNet.getTransitions().iterator();
		while (iter.hasNext()){
			TimedTransition tt = (TimedTransition) iter.next(); 
			if (!tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
				Pair<Double,Double> parameter = parameters.remove(0);
				tt.setDistributionParameters(new double[]{parameter.getKey(),parameter.getValue()});
				tt.setDistribution(null);
				tt.setDistribution(tt.initDistribution(0));
			}
		}
		
		PnmlExportStochasticNet exporter = new PnmlExportStochasticNet();
		try {
			exporter.exportPetriNetToPNMLFile(null, stochasticNet, new File("/home/andi/WorkspaceTex/TimeAnomalyBPM/experiment/model_"+experimentName+".pnml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		PNSimulator sim = new PNSimulator();
		XLog log = sim.simulate(null,stochasticNet, StochasticNetUtils.getSemantics(stochasticNet), new PNSimulatorConfig(50000,stochasticNet), initialMarking);
		
		Map<ReplayStep, ErrorType> errorLabel = new HashMap<ReplayStep, ErrorType>();
		
		CaseStatisticsList csl = LikelihoodAnalyzer.getLogLikelihoods(null, log, stochasticNet);
		CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(stochasticNet, initialMarking, csl);
	
		int errorPercentage = 10;
		
		double errorStandardDeviation = 4;
		
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
					double error = (rand.nextGaussian())*errorStandardDeviation;
					boolean affectingOrder = OutlierDetectionTest.shiftStep(rs, error);
					if (affectingOrder){
						errorsAffectingOrder++;
					}
					errorLabel.put(rs, (affectingOrder?ErrorType.STRUCTURAL_ERROR:ErrorType.TIME_ERROR));
					errorCount++;
				}
			}
		}
		// update analyzer:		
		analyzer.updateStatistics(analyzer.getOutlierRate());
		
		File results = new File("/home/andi/WorkspaceTex/TimeAnomalyBPM/experiment/2D_"+experimentName+"1000.dat");
		FileWriter writer = new FileWriter(results);
		String SEP = ";";
		writer.write("id"+SEP+"A_dur"+SEP+"B_dur"+SEP+"error"+SEP+"A_P-value"+SEP+"B_P-value"+SEP+"A-B-Marginal_P-value"+SEP+"prediction_onlyA"+SEP+"prediction_A_and_AB\n");
		int count = 0;
		for (CaseStatistics cs: csl){
			ReplayStep step = cs.getReplaySteps().get(1);
			if (errorLabel.get(step).equals(ErrorType.STRUCTURAL_ERROR)){
				continue;
			}
			Assert.assertEquals(1,step.children.size()); // has one child
			Assert.assertEquals(0,step.children.iterator().next().children.size()); // child is last step
			ReplayStep childStep = step.children.iterator().next(); 
			boolean A_prediction = analyzer.getIndividualOutlierSteps(cs).contains(step);
			boolean AB_prediction = analyzer.getIndividualOutlierSteps(cs).contains(step) && analyzer.isOutlierLikelyToBeAnError(step);
			
			double A_p_value = analyzer.getPValueOfStepIntegral(step);
			double B_p_value = analyzer.getPValueOfStepIntegral(childStep);
			RealDistribution convolvedDist = ConvolutionHelper.getConvolvedDistribution(step.transition.getDistribution(), childStep.transition.getDistribution());
			double AB_p_value = analyzer.computePValueByApproximateIntegration(convolvedDist, step.duration+childStep.duration);
			
			writer.write((count++) + SEP+step.duration+SEP+childStep.duration+SEP+(errorLabel.get(step).equals(ErrorType.TIME_ERROR)?"T":"F")+SEP+
					A_p_value+SEP+B_p_value+SEP+AB_p_value+SEP+A_prediction+SEP+AB_prediction+"\n");
		}
		writer.flush();
		writer.close();
	}
	
	@Ignore
	@Test
	public void testSurgeryOutliers() throws Exception{
		// load model and perform analysis
		Object[] surgeryNetAndMarking = TestUtils.loadModel("surgery_kernel_correct", true);
		XLog surgeryLog = TestUtils.loadLog(new File("/home/andreas/WorkspaceTex/BayesianMonitoring/models/real-data/correct_traces.xes"));
		
		// use 9 folds to learn stochastic properties of the model and 1 fold to find errors in.
		
		// report analysis result for pairs of activities
		Map<String,StringBuffer> fileBuffers = new HashMap<String, StringBuffer>();
		String SEP = ";";
		
		String header = "id"+SEP+"A_dur"+SEP+"B_dur"+SEP+"error"+SEP+"A_P-value"+SEP+"B_P-value"+SEP+"A-B-Marginal_P-value"+SEP+"prediction_onlyA"+SEP+"prediction_A_and_AB"+
						SEP+"likelihoodRatio"+SEP+"score"+
						SEP+"densityAB"+SEP+"densityErrorAB"+SEP+"errorScore"+
						SEP+"densityA"+SEP+"densityErrorA"+SEP+"errorScoreA"+
						SEP+"densityB"+SEP+"densityErrorB"+SEP+"errorScoreB"+
						SEP+"finalScore\n";
		StringBuffer allActivities = new StringBuffer(header);
		fileBuffers.put("all_activities", allActivities);
		StringBuffer allActivitiesAlsoStructural = new StringBuffer(header);
		fileBuffers.put("all_activities_also_structural", allActivitiesAlsoStructural);
		
		for(int kFold = 0; kFold < K_FOLDS; kFold++){
			System.out.println("running "+(kFold+1)+". fold / "+K_FOLDS+"...");
			
			StochasticNet surgeryStochasticNet = (StochasticNet)surgeryNetAndMarking[0];
			
			XLog trainingLog = StochasticNetUtils.filterTracesBasedOnModulo(surgeryLog, K_FOLDS, kFold, false);
			XLog testLog = StochasticNetUtils.filterTracesBasedOnModulo(surgeryLog, K_FOLDS, kFold, true);
			
			Manifest manifest = (Manifest)StochasticNetUtils.replayLog(null, surgeryStochasticNet, trainingLog, true, true);
			PerformanceEnricherConfig performanceEnricherConfig = new PerformanceEnricherConfig(
					DistributionType.GAUSSIAN_KERNEL,TimeUnit.MINUTES, ExecutionPolicy.RACE_ENABLING_MEMORY, null);
			Object[] netAndMarking = PerformanceEnricherPlugin.transform(null, manifest, performanceEnricherConfig);
			StochasticNet surgeryNet = (StochasticNet) netAndMarking[0];
			Marking initialMarking = (Marking) netAndMarking[1];
					
			
			CaseStatisticsList csl = LikelihoodAnalyzer.getLogLikelihoods(null, testLog, surgeryNet);
			CaseStatisticsAnalyzer analyzer = new CaseStatisticsAnalyzer(surgeryNet, initialMarking, csl);
			
		
		
			Map<ReplayStep, ErrorType> errorLabel = new HashMap<ReplayStep, ErrorType>();
			Assert.assertEquals(TimeUnit.MINUTES, surgeryNet.getTimeUnit());
			
			int errorPercentage = 10;
			double errorRate = 0.1;
			// insert errors
			double errorStandardDeviation = 60; // 1 hour (average case duration is 3.6 hours)
			int errorCount = 0;
			int errorsAffectingOrder = 0;
			for (CaseStatistics cs : csl){
				for (ReplayStep rs : cs.getReplaySteps()){
					errorLabel.put(rs, ErrorType.NONE);
					if (rs.transition.getDistributionType().equals(DistributionType.IMMEDIATE)){
						// ignore immediate transition replay steps
					} else { 
						if (rs.parents.size() > 0 && errorLabel.get(rs.parents.iterator().next()).equals(ErrorType.STRUCTURAL_ERROR)){
							// parent is a structural error (this event is also affected)
							errorLabel.put(rs, ErrorType.ERROR_NEIGHBOR);
							// do create two errors in a row.
						} else if (rand.nextDouble() <= errorPercentage/100.){
							// insert error:
							//double error = (rand.nextDouble()-0.5)*caseDuration/6.;
							double error = (rand.nextGaussian())*errorStandardDeviation;
							boolean affectingOrder = OutlierDetectionTest.shiftStep(rs, error);
							if (affectingOrder){
								errorsAffectingOrder++;
							}
							errorLabel.put(rs, (affectingOrder?ErrorType.STRUCTURAL_ERROR:ErrorType.TIME_ERROR));
							errorCount++;
						}
					}
				}
			}
			// update analyzer:		
			analyzer.updateStatistics(analyzer.getOutlierRate());
			
		
			int count = 0;
			int c = 0;
			
			Map<String, RealDistribution> convolvedErrorDists = new HashMap<String, RealDistribution>();
			
			// assumed error:
			RealDistribution errorDist = new NormalDistribution(0, errorStandardDeviation);
			
			
			for (CaseStatistics cs: csl){
				count++;
				System.out.println(count+"/"+csl.size());
				for (ReplayStep step : cs.getReplaySteps()){
					String stepString = step.transition.getLabel();
					if (!step.children.isEmpty()){
						stepString+="-"+step.children.iterator().next().transition.getLabel(); 
					}
					StringBuffer buffer;  
					if (!fileBuffers.containsKey(stepString)){
						buffer = new StringBuffer(header);
						fileBuffers.put(stepString, buffer);
					} else {
						buffer = fileBuffers.get(stepString);
					}
					if (errorLabel.get(step).equals(ErrorType.STRUCTURAL_ERROR)){
						// assume we are able to find structural errors correctly:
						String line = count+"_"+(c++)+SEP+step.duration+SEP+"-1"+SEP+"T"+SEP+analyzer.getPValueOfStepIntegral(step)+SEP+-1+SEP+-1+
						SEP+"true"+SEP+"true"+ // prediction_onlyA, prediction_A_and_AB
						SEP+2+SEP+2+ // likelihoodRatio, score
						SEP+0+SEP+2+SEP+1+// densityAB, densityErrorAB, errorScoreAB, 
						SEP+0+SEP+2+SEP+1+// densityA, densityErrorA, errorscoreA,
						SEP+0+SEP+2+SEP+1+// densityB, densityErrorB, errorscoreB,
						SEP+1+"\n";// final Score
						buffer.append(line);
						allActivitiesAlsoStructural.append(line);
					} else {
						ReplayStep childStep;
						if (step.children.size()>0){
							childStep = step.children.iterator().next();
							
	//						if (step.parents.size()>0){
	//							ReplayStep parentStep = step.parents.iterator().next();
	//							if (errorLabel.get(parentStep).equals(ErrorType.STRUCTURAL_ERROR)){
	//								// ignore this. We assume that structural errors can be detected correctly.
	//								continue;
	//							}
	//						}
							// compute likelihood ratio:
							// P(A,B | no error) = P(A | no error)*P(B | no error)
							double density_AB = step.transition.getDistribution().density(step.duration) * childStep.transition.getDistribution().density(childStep.duration);
							
							
							RealDistribution convolvedDistributionA = getErrorConvolution(step, errorDist, convolvedErrorDists);
							RealDistribution convolvedDistributionB = getErrorConvolution(childStep, errorDist, convolvedErrorDists);
							double density_A_Error = convolvedDistributionA.density(step.duration);
							double density_B_Error = convolvedDistributionB.density(childStep.duration);
							double likelihood_AB_Ratio = 0.5;
							likelihood_AB_Ratio = (density_A_Error*density_B_Error) / (density_AB + density_A_Error*density_B_Error);
							
							boolean A_prediction = analyzer.getIndividualOutlierSteps(cs).contains(step);
							boolean AB_prediction = analyzer.getIndividualOutlierSteps(cs).contains(step) && analyzer.isOutlierLikelyToBeAnError(step);
							
							double A_p_value = analyzer.getPValueOfStepIntegral(step);
							double B_p_value = analyzer.getPValueOfStepIntegral(childStep);
							RealDistribution convolvedDist = null;
							String convolvedId = step.transition.getLabel()+"_"+childStep.transition.getLabel();
							if (convolvedErrorDists.containsKey(convolvedId)){
								convolvedDist = convolvedErrorDists.get(convolvedId);
							} else {
								convolvedDist = ConvolutionHelper.getConvolvedDistribution(step.transition.getDistribution(), childStep.transition.getDistribution());
								convolvedErrorDists.put(convolvedId,convolvedDist);
							}
							 
							double AB_p_value = analyzer.computePValueByApproximateIntegration(convolvedDist, step.duration+childStep.duration);
							
							double score = (A_prediction?likelihood_AB_Ratio:0.01*(1-A_p_value));
							double[] modelDensities = analyzer.getModelDensities(step, errorDist, errorRate);
							
							ErrorType errorType = errorLabel.get(step);
							
							String line = count+"_"+(c++)+SEP+step.duration+SEP+childStep.duration+SEP+(errorType.equals(ErrorType.TIME_ERROR)?"T":"F")+SEP+
							A_p_value+SEP+B_p_value+SEP+AB_p_value+SEP+A_prediction+SEP+AB_prediction+SEP+likelihood_AB_Ratio+SEP+score+SEP+
							modelDensities[0]+SEP+modelDensities[1]+SEP+modelDensities[2]+SEP+ // densityAB, densityErrorAB, errorScoreAB, 
							modelDensities[3]+SEP+modelDensities[4]+SEP+modelDensities[5]+SEP+ // densityA, densityErrorA, errorscoreA,
							modelDensities[6]+SEP+modelDensities[7]+SEP+modelDensities[8]+SEP+ // densityB, densityErrorB, errorscoreB,
							modelDensities[1]/(modelDensities[1]+modelDensities[0]+modelDensities[4]+modelDensities[7])+"\n";
							buffer.append(line);
							allActivities.append(line);
							allActivitiesAlsoStructural.append(line);
						} else {
							// last activity (ignore?)
						}		
					}
				}
			}
		}
		for (Entry<String, StringBuffer> entry : fileBuffers.entrySet()){
			File results = new File("/home/andreas/WorkspaceTex/TimeAnomalyBPM/experiment/surgery2/"+entry.getKey()+".dat");
			FileWriter writer = new FileWriter(results);
			writer.write(entry.getValue().toString());
			writer.flush();
			writer.close();
		}
	}

	private RealDistribution getErrorConvolution(ReplayStep step, RealDistribution errorDist, Map<String, RealDistribution> convolvedErrorDists) {
		if (convolvedErrorDists.containsKey(step.transition.getLabel())){
			return convolvedErrorDists.get(step.transition.getLabel());
		}
		RealDistribution convolvedDistribution = ConvolutionHelper.getConvolvedDistribution(step.transition.getDistribution(), errorDist);
		convolvedErrorDists.put(step.transition.getLabel(), convolvedDistribution);
		return convolvedDistribution;
	}
}
