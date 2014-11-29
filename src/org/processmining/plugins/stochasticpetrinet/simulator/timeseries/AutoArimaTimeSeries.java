package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
//import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
//import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;
//import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.LimitedTreeMap;
//import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration.AvailableScripts;
//import org.rosuda.REngine.REXPDouble;
//import org.rosuda.REngine.REXPInteger;
//import org.rosuda.REngine.REngineException;
//import org.rosuda.REngine.RList;
//import org.utils.datastructures.LimitedQueue;
//
public class AutoArimaTimeSeries extends RTimeSeries<Double>{

	public AutoArimaTimeSeries(String key) {
		super(key);
	}

	protected void fit(LimitedQueue<Observation<Double>> currentObservations) {
		// TODO Auto-generated method stub
	}

	protected Prediction<Double> getPrediction(int h, Object... payload) {
		// TODO Auto-generated method stub
		return null;
	}
//
//	public AutoArimaTimeSeries(String key) {
//		super(key);
//		loadScriptJRI(AvailableScripts.CATEGORICAL_SCRIPT);
//		loadScriptJRI(AvailableScripts.METRIC_SCRIPT);
//	}
//
//	protected void fit(LimitedQueue<Observation<H>> currentObservations) {
//		// collect training data and sort them according to the date
//		Map<Long, String> sortedDecisions = new LimitedTreeMap<>(MAX_SIZE);
//		
//		String header[] = new String[]{"timestamp","decision","duration","systemLoad"};
//		
//		for (Transition t : conflictingTransitions){ // TODO: add training data to immediate transitions!
//			assert t instanceof TimedTransition;
//			
//			TimedTransition tt = (TimedTransition) t;
//			String trainingData = tt.getTrainingData();
//			
//			String[] entries = trainingData.split("\n");
//			// ignore header!
//			for (int i = 1; i < entries.length; i++){
//				String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
//				sortedDecisions.put(Long.valueOf(entryParts[2]), semantics.getTransitionId(t)+StochasticManifestCollector.DELIMITER+entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
//			}
//			probabilities.put(t, (double)(entries.length-1)); // prefill with observation counts
//		}
//		// create the matrix
//		double[] timeStamps = new double[sortedDecisions.size()];
//		int[] decisions = new int[sortedDecisions.size()];
//		double[] durations =  new double[sortedDecisions.size()];
//		int[] systemLoads =  new int[sortedDecisions.size()];
//		int i = 0;
//		for (Map.Entry<Long, String> entry : sortedDecisions.entrySet()){
//			timeStamps[i] = entry.getKey();
//			String[] parts = entry.getValue().split(StochasticManifestCollector.DELIMITER);
//			decisions[i] = Integer.valueOf(parts[0]);
//			durations[i] = Double.valueOf(parts[1]);
//			systemLoads[i] = Integer.valueOf(parts[2]);
//			i++;
//		}
//		RList list = new RList();
//		list.put(header[0], new REXPDouble(timeStamps));
//		list.put(header[1], new REXPInteger(decisions));
//		list.put(header[2], new REXPDouble(durations));
//		list.put(header[3], new REXPInteger(systemLoads));
//		
//		try {
//			org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//			rEngine.assign("data", exp);
//			
//			rEngine.parseAndEval("df <- runCategoricalPredictionJRI(data=data, timeStamp="+currentTime.getTime()+")");
//			org.rosuda.REngine.REXP size = rEngine.parseAndEval("nrow(df)");
//			if (size.asInteger() <= 1){
//				// only one time point! Can't use time series, just return the ratio
//				return probabilities;
//			}
//			
//			rFitString = "fit"+key.replaceAll("[\\s,\\[\\]]", "_");
//			rEngine.parseAndEval(rFitString + " <- metricFit(df=df, predictDuration=F)");
//			this.conflictFits.put(key, rFitString);
//		} catch (REngineException e){
//			e.printStackTrace();
//		}
//		
//	}
//
//	protected Prediction<H> getPrediction(int h, Object... payload) {
//		List<String> headers = new ArrayList<>();
//		List<org.rosuda.REngine.REXP> entries = new ArrayList<>();
//		headers.add("timestamp");
//		entries.add(new REXPDouble(currentTime.getTime()));
//		for (Transition t : conflictingTransitions){
//			headers.add("decision."+semantics.getTransitionId(t));
//			entries.add(new REXPDouble(Double.NaN));
//		}
//		headers.add("duration");
//		entries.add(new REXPDouble(Double.NaN));
//		headers.add("systemLoad");
//		entries.add(new REXPDouble(1));
//		
//		RList list = new RList(entries, headers.toArray(new String[headers.size()]));
//		org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//		rEngine.assign("new", exp);
//		
//		exp = rEngine.parseAndEval("prediction <- doForecast(fit="+rFitString+", new=new, predictDuration=F)");
//		double[][] forecastArray = exp.asDoubleMatrix(); //
//		// get the point estimates:
//		int i = 0;
//		double totalSum = 0;
//		for (Transition t : conflictingTransitions){
//			double estimate = forecastArray[i++][0];
//			totalSum += estimate;
//			probabilities.put(t, estimate);
//		}
//		for (Map.Entry<Transition, Double> entry : probabilities.entrySet()){
//			entry.setValue(entry.getValue()/totalSum);
//		}
//	}
//
//
//	
//	
	

	
//	String header[] = new String[]{"timestamp","decision","duration","systemLoad"};
//	
//	for (Transition t : conflictingTransitions){ // TODO: add training data to immediate transitions!
//		assert t instanceof TimedTransition;
//		
//		TimedTransition tt = (TimedTransition) t;
//		String trainingData = tt.getTrainingData();
//		
//		String[] entries = trainingData.split("\n");
//		// ignore header!
//		for (int i = 1; i < entries.length; i++){
//			String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
//			sortedDecisions.put(Long.valueOf(entryParts[2]), semantics.getTransitionId(t)+StochasticManifestCollector.DELIMITER+entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
//		}
//		probabilities.put(t, (double)(entries.length-1)); // prefill with observation counts
//	}
//	// create the matrix
//	double[] timeStamps = new double[sortedDecisions.size()];
//	int[] decisions = new int[sortedDecisions.size()];
//	double[] durations =  new double[sortedDecisions.size()];
//	int[] systemLoads =  new int[sortedDecisions.size()];
//	int i = 0;
//	for (Map.Entry<Long, String> entry : sortedDecisions.entrySet()){
//		timeStamps[i] = entry.getKey();
//		String[] parts = entry.getValue().split(StochasticManifestCollector.DELIMITER);
//		decisions[i] = Integer.valueOf(parts[0]);
//		durations[i] = Double.valueOf(parts[1]);
//		systemLoads[i] = Integer.valueOf(parts[2]);
//		i++;
//	}
//	RList list = new RList();
//	list.put(header[0], new REXPDouble(timeStamps));
//	list.put(header[1], new REXPInteger(decisions));
//	list.put(header[2], new REXPDouble(durations));
//	list.put(header[3], new REXPInteger(systemLoads));
//	
//	try {
//		org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//		rEngine.assign("data", exp);
//		
//		rEngine.parseAndEval("df <- runCategoricalPredictionJRI(data=data, timeStamp="+currentTime.getTime()+")");
//		org.rosuda.REngine.REXP size = rEngine.parseAndEval("nrow(df)");
//		if (size.asInteger() <= 1){
//			// only one time point! Can't use time series, just return the ratio
//			return probabilities;
//		}
//		
//		rFitString = "fit"+key.replaceAll("[\\s,\\[\\]]", "_");
//		rEngine.parseAndEval(rFitString + " <- metricFit(df=df, predictDuration=F)");
//		this.conflictFits.put(key, rFitString);
//	} catch (REngineException e){
//		e.printStackTrace();
//	}
	
}
