package org.processmining.plugins.stochasticpetrinet.simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.LimitedTreeMap;
import org.rosuda.JRI.REXP;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;

/**
 * A stochastic Petri net simulator that replaces each timed transition's distribution with a 
 * time series predictor powered by R. 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PNTimeSeriesSimulator extends PNSimulator {

	/** reference to the R engine */
	protected Rengine engine;
	
	protected REngine rEngine;
	
	/**
	 * The maximum number of historical values to use to train a time series predictor...
	 */
	public static final int MAX_SIZE = 1000;
	
	public enum AvailableScripts{
		METRIC_SCRIPT, CATEGORICAL_SCRIPT;
		
		public String getPath(){
			switch(this){
				case METRIC_SCRIPT:
					return "scripts/metric_prediction.r";
				case CATEGORICAL_SCRIPT:
					return "scripts/categorical_prediction.r";
			}
			return "unspecified Path!!";
		}
	}
	
	Set<AvailableScripts> loadedScripts;
	Set<AvailableScripts> loadedScriptsJRI;
	
	private Map<Transition, String> transitionDurationFits;
	private Map<String, String> conflictFits;
		
	public PNTimeSeriesSimulator(){
		super();
		engine = RProvider.getEngine();
		rEngine = RProvider.getREngine();
		loadedScripts = new HashSet<AvailableScripts>();
		loadedScriptsJRI = new HashSet<AvailableScripts>();
		transitionDurationFits = new HashMap<>();
		conflictFits = new HashMap<>();
	}
	
	
	
	/**
	 * 
	 * @param currentTime the time of prediction
	 * @param systemLoad the current system load
	 * @param conflictingTransitions
	 * @param semantics
	 * @return
	 * 
	 * TODO: do not fit all the models over and over again, store a "fit" object in the R engine!!
	 */
	public Map<Transition, Double> getTransitionProbabilities(Date currentTime, int systemLoad, Collection<Transition> conflictingTransitions, EfficientStochasticNetSemanticsImpl semantics) {
		Map<Transition, Double> probabilities = new HashMap<>();
		
		if (conflictingTransitions.size()==1){
			probabilities.put(conflictingTransitions.iterator().next(), 1.);
		} else {
			try {
				loadScriptJRI(AvailableScripts.METRIC_SCRIPT);
				loadScriptJRI(AvailableScripts.CATEGORICAL_SCRIPT);
				
				String key = getConflictKey(conflictingTransitions, semantics);
				String rFitString = null;
				if (conflictFits.containsKey(key)){
					rFitString = conflictFits.get(key);
				} else {
					// collect training data and sort them according to the date
					Map<Long, String> sortedDecisions = new LimitedTreeMap<>(MAX_SIZE);
					
					String header[] = new String[]{"timestamp","decision","duration","systemLoad"};
					
					for (Transition t : conflictingTransitions){ // TODO: add training data to immediate transitions!
						assert t instanceof TimedTransition;
						
						TimedTransition tt = (TimedTransition) t;
						String trainingData = tt.getTrainingData();
						
						String[] entries = trainingData.split("\n");
						// ignore header!
						for (int i = 1; i < entries.length; i++){
							String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
							sortedDecisions.put(Long.valueOf(entryParts[2]), semantics.getTransitionId(t)+StochasticManifestCollector.DELIMITER+entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
						}
						probabilities.put(t, (double)(entries.length-1)); // prefill with observation counts
					}
					// create the matrix
					double[] timeStamps = new double[sortedDecisions.size()];
					int[] decisions = new int[sortedDecisions.size()];
					double[] durations =  new double[sortedDecisions.size()];
					int[] systemLoads =  new int[sortedDecisions.size()];
					int i = 0;
					for (Map.Entry<Long, String> entry : sortedDecisions.entrySet()){
						timeStamps[i] = entry.getKey();
						String[] parts = entry.getValue().split(";");
						decisions[i] = Integer.valueOf(parts[0]);
						durations[i] = Double.valueOf(parts[1]);
						systemLoads[i] = Integer.valueOf(parts[2]);
						i++;
					}
					RList list = new RList();
					list.put(header[0], new REXPDouble(timeStamps));
					list.put(header[1], new REXPInteger(decisions));
					list.put(header[2], new REXPDouble(durations));
					list.put(header[3], new REXPInteger(systemLoads));
					
					try {
						org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
						rEngine.assign("data", exp);
						
						rEngine.parseAndEval("df <- runCategoricalPredictionJRI(data=data, timeStamp="+currentTime.getTime()+")");
						org.rosuda.REngine.REXP size = rEngine.parseAndEval("nrow(df)");
						if (size.asInteger() <= 1){
							// only one time point! Can't use time series, just return the ratio
							return probabilities;
						}
						
						rFitString = "fit"+key.replaceAll("[\\s,\\[\\]]", "_");
						rEngine.parseAndEval(rFitString + " <- metricFit(df=df, predictDuration=F)");
						this.conflictFits.put(key, rFitString);
					} catch (REngineException e){
						e.printStackTrace();
					}
				}
				List<String> headers = new ArrayList<>();
				List<String> entries = new ArrayList<>();
				headers.add("timestamp");
				entries.add(String.valueOf(currentTime.getTime()));
				for (Transition t : conflictingTransitions){
					headers.add("decision."+semantics.getTransitionId(t));
					entries.add("NA");
				}
				headers.add("duration");
				entries.add("NA");
				headers.add("systemLoad");
				entries.add("1");
				
				RList list = new RList(entries, headers.toArray(new String[headers.size()]));
				org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
				rEngine.assign("new", exp);
				
				exp = rEngine.parseAndEval("prediction <- doForecast(fit="+rFitString+", new=new, predictDuration=F)");
				double[][] forecastArray = exp.asDoubleMatrix(); //
				// get the point estimates:
				int i = 0;
				double totalSum = 0;
				for (Transition t : conflictingTransitions){
					double estimate = forecastArray[i++][0];
					totalSum += estimate;
					probabilities.put(t, estimate);
				}
				for (Map.Entry<Transition, Double> entry : probabilities.entrySet()){
					entry.setValue(entry.getValue()/totalSum);
				}
			
			
//				// load R script
//				loadScript(AvailableScripts.METRIC_SCRIPT);
//				loadScript(AvailableScripts.CATEGORICAL_SCRIPT);
//				
//				String key = getConflictKey(conflictingTransitions, semantics);
//				String rFitString = null;
//				if (conflictFits.containsKey(key)){
//					rFitString = conflictFits.get(key);
//				} else {
//					// collect training data and sort them according to the date
//					Map<Long, String> sortedDecisions = new TreeMap<>();
//					
//					File tempFile = java.io.File.createTempFile("training", ".csv");
//					tempFile.deleteOnExit();
//					BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
//					
//					String header = "timestamp;decision;duration;systemLoad";
//					
//					for (Transition t : conflictingTransitions){ // TODO: add training data to immediate transitions!
//						assert t instanceof TimedTransition;
//						
//						TimedTransition tt = (TimedTransition) t;
//						String trainingData = tt.getTrainingData();
//						
//						String[] entries = trainingData.split("\n");
//						// ignore header!
//						for (int i = 1; i < entries.length; i++){
//							String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
//							sortedDecisions.put(Long.valueOf(entryParts[2]), semantics.getTransitionId(t)+StochasticManifestCollector.DELIMITER+entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
//						}
//						probabilities.put(t, (double)(entries.length-1)); // prefill with observation counts
//					}
//					writer.write(header+"\n");
//					for (Map.Entry<Long, String> entry : sortedDecisions.entrySet()){
//						writer.write(entry.getKey()+";"+entry.getValue()+"\n");
//					}
//					writer.flush();
//					writer.close();
//					
//					String evalString = "df <- runCategoricalPrediction(fileName='"+tempFile.getAbsolutePath()+"', timeStamp="+currentTime.getTime()+")";
//					System.out.println(evalString);
//					engine.eval(evalString);
//					REXP size = engine.eval("nrow(df)");
//					if (size.asInt() <= 1){
//						// only one time point! Can't use time series, just return the ratio
//						return probabilities;
//					}
////					tempFile.delete();
//					
//					rFitString = "fit"+key.replaceAll("[\\s,\\[\\]]", "_");
//					engine.eval(rFitString + " <- metricFit(df=df, predictDuration=F)");
//					this.conflictFits.put(key, rFitString);
//				}
//				
//				// we have the handle to the time series fit; now we want to make the prediction with it:
//				File inputFile = java.io.File.createTempFile("input", ".csv");
//				inputFile.deleteOnExit();
//				String headerInput = "timestamp;";
//				for (Transition t : conflictingTransitions){
//					headerInput += "decision."+semantics.getTransitionId(t)+";";
//				}
//				headerInput += "duration;systemLoad";
//				BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile));
//				writer.write(headerInput+"\n");
//				writer.write(currentTime.getTime()+";");
//				for (Transition t: conflictingTransitions){
//					writer.write("NA;");
//				}
//				writer.write("NA;"+systemLoad+"\n");
//				writer.flush();
//				writer.close();
//				
//				String readInput = "new <- read.csv('"+inputFile.getAbsolutePath()+"', sep = \";\")";
//				System.out.println(readInput);
//				engine.eval(readInput);
////				inputFile.delete();
//				
//				REXP exp = engine.eval("prediction <- doForecast(fit="+rFitString+", new=new, predictDuration=F)");
//				double[][] forecastArray = exp.asDoubleMatrix(); //
//				// get the point estimates:
//				int i = 0;
//				double totalSum = 0;
//				for (Transition t : conflictingTransitions){
//					double estimate = forecastArray[i++][0];
//					totalSum += estimate;
//					probabilities.put(t, estimate);
//				}
//				for (Map.Entry<Transition, Double> entry : probabilities.entrySet()){
//					entry.setValue(entry.getValue()/totalSum);
//				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (REngineException e) {
				e.printStackTrace();
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			} 
		}
		return probabilities;
	}
	

	
	private String getConflictKey(Collection<Transition> conflictingTransitions, EfficientStochasticNetSemanticsImpl semantics) {
		TreeSet<Short> keys = new TreeSet<>();
		for (Transition t : conflictingTransitions){
			keys.add(semantics.getTransitionId(t));
		}
		return Arrays.toString(keys.toArray());
	}


	/**
	 * TODO: make this work from a jar:
	 * @param metricScript
	 */
	private void loadScript(AvailableScripts metricScript) {
		if (!loadedScripts.contains(metricScript)){
			String sourceString = "source('"+new File(metricScript.getPath()).getAbsolutePath()+"')";
			System.out.println(sourceString);
			REXP exp = engine.eval(sourceString);
			if (exp != null){
				loadedScripts.add(metricScript);
			}
		}
	}

	/**
	 * TODO: make this work from a jar:
	 * @param metricScript
	 */
	private void loadScriptJRI(AvailableScripts metricScript) {
		if (!loadedScriptsJRI.contains(metricScript)){
			try {
				String sourceString = "source('"+new File(metricScript.getPath()).getAbsolutePath()+"')";
				System.out.println(sourceString);
				org.rosuda.REngine.REXP exp = rEngine.parseAndEval(sourceString);
				if (exp != null && !exp.isNull()){
					loadedScriptsJRI.add(metricScript);
				}
			} catch (REngineException e) {
				e.printStackTrace();
			} catch (REXPMismatchException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	protected double sampleDurationForTransition(double positiveConstraint, long startOfTransition, TimedTransition timedT) {
		if (timedT.getTrainingData() != null){
//			try{
				String fitName = null;
				if (transitionDurationFits.containsKey(timedT)){
					fitName = transitionDurationFits.get(timedT);
				} else {
//					loadScript(AvailableScripts.CATEGORICAL_SCRIPT);
//					loadScript(AvailableScripts.METRIC_SCRIPT);
					
					loadScriptJRI(AvailableScripts.CATEGORICAL_SCRIPT);
					loadScriptJRI(AvailableScripts.METRIC_SCRIPT);
					
					// create the training data frame:
					String[] lines = timedT.getTrainingData().split("\n");
					// duration; systemload; timestamp
					String[] names = lines[0].split(";");
					RList list = new RList();
					// add the data columnwise?
					for (int c = 0; c < names.length; c++){
						double[] column = new double[lines.length-1];
						for (int i = 1; i < lines.length; i++){
							String[] parts = lines[i].split(";");
							column[i-1] = Double.valueOf(parts[c]);
						}
						REXPDouble doubleColumn = new REXPDouble(column);  
						list.put(names[c], doubleColumn);
					}
//					for (int i = 1; i < lines.length; i++){
//						RList lineList = new RList(Arrays.asList(lines[i].split(";")), names);
//						list.add(lineList);
//					}
					try {
						org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
						rEngine.assign("data", exp);
						
						rEngine.parseAndEval("df <- runCategoricalPredictionJRI(data=data, timeStamp="+startOfTransition+")");
						org.rosuda.REngine.REXP size = rEngine.parseAndEval("nrow(df)");
						if (size.asInteger() <= 1){
							// only one time point! Can't use time series, just return the ratio
							return rEngine.parseAndEval("as.numeric(df$duration)").asDouble();
						}
						
						fitName = "fit"+timedT.getLabel().replaceAll("\\s", "_");
						String fitCommand = fitName + " <- metricFit(df=df, predictDuration=T)";
						System.out.println(fitCommand);
						rEngine.parseAndEval(fitCommand);
					} catch (REXPMismatchException e) {
						e.printStackTrace();
					} catch (REngineException e) {
						e.printStackTrace();
					}
				
//					// train a time series model (TODO: use cached fit)
//					File tempFile = java.io.File.createTempFile("training", ".csv");
//					tempFile.deleteOnExit();
//					FileUtils.write(tempFile, timedT.getTrainingData());
//					
//					String evalString = "df <- runCategoricalPrediction(fileName='"+tempFile.getAbsolutePath()+"', timeStamp="+startOfTransition+")";
//					System.out.println(evalString);
//					engine.eval(evalString);
//					tempFile.delete();
//					REXP size = engine.eval("nrow(df)");
//					if (size.asInt() <= 1){
//						// only one time point! Can't use time series, just return the ratio
//						return engine.eval("as.numeric(df$duration)").asDouble();
//					}
//					
//					
//					fitName = "fit"+timedT.getLabel().replaceAll("\\s", "_");
//					String fitCommand = fitName + " <- metricFit(df=df, predictDuration=T)";
//					System.out.println(fitCommand);
//					engine.eval(fitCommand);
					
					this.transitionDurationFits.put(timedT, fitName);
				}
				
				try {
					RList list = new RList(Arrays.asList(startOfTransition+"","NA","1"), new String[]{"timestamp", "duration", "systemLoad"});
					org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
					rEngine.assign("new", exp);
					String predictionCommand = "prediction <- doForecast(fit = "+fitName+", new=new, predictDuration=T)";
					org.rosuda.REngine.REXP predictedDuration = rEngine.parseAndEval(predictionCommand);
					double[] predictionArray = predictedDuration.asDoubles();
					NormalDistribution dist = new NormalDistribution(predictionArray[0], (predictionArray[0]-predictionArray[2]) / 2 ); // 95% equals to roughly 2 sigma in an assumed normal distribution
					return dist.sample();
				} catch (REXPMismatchException e) {
					e.printStackTrace();
				} catch (REngineException e) {
					e.printStackTrace();
				}
				
				
//				// we have a handle to the fit object; now we want to make the prediction with it:
//				String headerInput = "timestamp;duration;systemLoad";
//				
//				File inputFile = java.io.File.createTempFile("input", ".csv");
//				inputFile.deleteOnExit();
//				
//				FileUtils.write(inputFile, headerInput+"\n"
//						+startOfTransition+";NA;"+1+"\n"); // TODO: handle system load in simulation! (Do a joint simulation over multiple cases)
//				
//				String readInput = "new <- read.csv('"+inputFile.getAbsolutePath()+"', sep = \";\")";
//				System.out.println(readInput);
//				engine.eval(readInput);
//				inputFile.delete();
				
//				String predictionCommand = "prediction <- doForecast(fit = "+fitName+", new=new, predictDuration=T)";
//				System.out.println(predictionCommand);
//				REXP predictedDuration = engine.eval(predictionCommand);
//				double[] predictionArray = predictedDuration.asDoubleArray(); 
//				// predictedDuration has 5 fields: Point Forecast,  Lo 80, Lo 95, Hi 80, Hi 95
//				NormalDistribution dist = new NormalDistribution(predictionArray[0], (predictionArray[0]-predictionArray[2]) / 2 ); // 95% equals to roughly 2 sigma in an assumed normal distribution
//				
//				return dist.sample();
//			} catch (IOException e){
//				e.printStackTrace();
//			}
		}
		return super.sampleDurationForTransition(positiveConstraint, startOfTransition, timedT);
	}



	public int pickTransitionAccordingToWeights(Collection<Transition> transitions, Date currentTime, Semantics<Marking, Transition> semantics) {
		if (semantics instanceof EfficientStochasticNetSemanticsImpl){
			EfficientStochasticNetSemanticsImpl effiSemantics = (EfficientStochasticNetSemanticsImpl) semantics;
			Map<Transition, Double> transitionProbabilities = getTransitionProbabilities(currentTime, 1, transitions, effiSemantics);	
			double[] probabilities = new double[transitionProbabilities.size()];
			int i = 0;
			for (Transition t : transitions){
				probabilities[i] = transitionProbabilities.get(t);
				i++;
			}
			return StochasticNetUtils.getRandomIndex(probabilities, random);
		} else {
			throw new RuntimeException("Please make sure to use the EfficientStochasticNetSemanticsImpl for time series prediction!");
		}
	}
	
	
	
	
}
