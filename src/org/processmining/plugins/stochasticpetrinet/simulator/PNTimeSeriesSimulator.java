package org.processmining.plugins.stochasticpetrinet.simulator;

import com.google.common.collect.SortedMultiset;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.DiracDeltaDistribution;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration.MissingDataHandling;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.LastObservationTimeSeries;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.Observation;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.Prediction;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.TimeSeries;
import org.utils.datastructures.Aggregate;
import org.utils.datastructures.ComparablePair;
import org.utils.datastructures.LimitedTreeMap;

import java.util.*;


/**
 * A stochastic Petri net simulator that replaces each timed transition's distribution with a
 * time series predictor.
 *
 * @author Andreas Rogge-Solti
 */
public class PNTimeSeriesSimulator extends PNSimulator {

    protected TimeSeriesConfiguration config;

    public static final int CACHE_SIZES = 1000;

    /**
     * maps from the prediction time point (long since start of epoch) to a cache storing the predictions for each transition
     */
    private LimitedTreeMap<Long, Map<Transition, RealDistribution>> cachedPredictedDurations;

    /**
     * maps from the prediction time point (long since start of epoch) to a cache storing the predictions for each transition
     */
    private LimitedTreeMap<Long, Map<String, Map<Transition, Double>>> cachedConflictingProbabilities;

    private Map<Transition, TimeSeries<Double>> cachedTransitionTimeSeries;
    private Map<Transition, TimeSeries<Double>> cachedTransitionDecisionTimeSeries;


    public PNTimeSeriesSimulator(TimeSeriesConfiguration config) {
        super();
        cachedTransitionTimeSeries = new HashMap<>();
        cachedTransitionDecisionTimeSeries = new HashMap<>();
        cachedPredictedDurations = new LimitedTreeMap<>(CACHE_SIZES);
        cachedConflictingProbabilities = new LimitedTreeMap<>(CACHE_SIZES);
        this.config = config;
    }


    /**
     * @param currentTime            the time of prediction
     * @param systemLoad             the current system load
     * @param conflictingTransitions
     * @param semantics
     * @return
     */
    public Map<Transition, Double> getTransitionProbabilities(Date currentTime, int systemLoad,
                                                              Collection<Transition> conflictingTransitions, EfficientStochasticNetSemanticsImpl semantics) {
        Map<Transition, Double> probabilities = new HashMap<>();

        if (conflictingTransitions.size() == 1) {
            probabilities.put(conflictingTransitions.iterator().next(), 1.);
        } else {
            long index = config.getIndexForTime(currentTime.getTime());
            String key = getConflictKey(conflictingTransitions, semantics);

            if (cachedConflictingProbabilities.containsKey(index)
                    && cachedConflictingProbabilities.get(index).containsKey(key)) {
                // we have already a prediction
                return cachedConflictingProbabilities.get(index).get(key);
            } else {
                // for each transition we
                // collect training data and sort them according to the date
                for (Transition t : conflictingTransitions) {
                    if (!(t instanceof TimedTransition)) {
                        throw new IllegalArgumentException(
                                "Time series simulation only works with stochastic Petri net models!");
                    }
                    TimedTransition tt = (TimedTransition) t;
                    TimeSeries<Double> timeSeries = cachedTransitionDecisionTimeSeries.get(t);
                    if (timeSeries == null) {
                        timeSeries = config.createNewTimeSeries(tt);
                        timeSeries.setKey(tt.getLabel());
                        cachedTransitionDecisionTimeSeries.put(t, timeSeries);
                    }

                    // TODO: avoid reiterating all the training data somehow.
                    SortedMultiset<ComparablePair<Long, List<Object>>> trainingDataSoFar = tt.getTrainingDataUpTo(currentTime.getTime());
                    if (timeSeries instanceof LastObservationTimeSeries) {
                        probabilities.put(tt, (double) trainingDataSoFar.size());
                    } else {

                        List<List<Object>> sortedDecisions = new LinkedList<>();
                        for (ComparablePair<Long, List<Object>> pair : trainingDataSoFar) {
                            List<Object> list = Arrays.<Object>asList(config.getIndexForTime(pair.getFirst()), semantics.getTransitionId(t), pair.getSecond().get(0), pair.getSecond().get(1));
                            sortedDecisions.add(list);
                        }

                        Aggregate.Function<List<Object>, Long> groupBy = new Aggregate.Function<List<Object>, Long>() {
                            @Override
                            public Long apply(List<Object> item) {
                                // group by the time index
                                return (Long) (item.get(0));
                            }
                        };

                        Map<Long, Integer> count = Aggregate.sum(sortedDecisions, groupBy,
                                new Aggregate.Function<List<Object>, Integer>() {
                                    @Override
                                    public Integer apply(List<Object> item) {
                                        return 1;
                                    }
                                });

                        if (count.size() == 1) {
                            probabilities.put(t, Double.valueOf(count.values().iterator().next()));
                        } else {
                            // go through all elements of the map
                            List<Observation<Double>> observations = new ArrayList<>();

                            Iterator<Long> iter = count.keySet().iterator();
                            Long current = iter.next();
                            Long next = iter.hasNext() ? iter.next() : null;

                            double mean = StochasticNetUtils.getMean(count.values());

                            do {
                                Observation<Double> observedCount = new Observation<>();
                                observedCount.timestamp = current; // no real time stamp but rather the time index in the time series
                                if (count.containsKey(current)) {
                                    observedCount.observation = Double.valueOf(count.get(current));
                                } else {
                                    // missing value!
                                    // replace with last observation
                                    observedCount.observation = handleMissingValue(observations, config.getMissingDataHandling(), mean);
                                }
                                observations.add(observedCount);

                                if (next != null && current + 1 < next) {
                                    current++;
                                } else {
                                    current = next;
                                    next = iter.hasNext() ? iter.next() : null;
                                }
                            } while (current != null && current < index);
                            timeSeries.resetTo(observations);

                            // forecast horizon:
                            int h = (int) (index - observations.get(observations.size() - 1).timestamp);
                            Prediction<Double> prediction = timeSeries.predict(h);
                            probabilities.put(t, prediction.prediction);
                        }
                    }
                }
                if (!cachedConflictingProbabilities.containsKey(index)) {
                    cachedConflictingProbabilities.put(index, new HashMap<String, Map<Transition, Double>>());
                }
                cachedConflictingProbabilities.get(index).put(key, probabilities);
            }
        }
        return probabilities;
    }

    private Double handleMissingValue(List<Observation<Double>> observations, MissingDataHandling missingDataHandling, double mean) {
        switch (missingDataHandling) {
            case KEEP_AS_NAN:
                return Double.NaN;
            case REPLACE_WITH_LAST_OBSERVATION:
                return observations.get(observations.size() - 1).observation;
            case REPLACE_WITH_MEAN:
            default:
                return mean;
        }
    }


//
//					Map<Long, String> sortedDecisions = new LimitedTreeMap<>(CACHE_SIZES);
//					
//					
//				}
//				List<String> headers = new ArrayList<>();
//				List<org.rosuda.REngine.REXP> entries = new ArrayList<>();
//				headers.add("timestamp");
//				entries.add(new REXPDouble(currentTime.getTime()));
//				for (Transition t : conflictingTransitions){
//					headers.add("decision."+semantics.getTransitionId(t));
//					entries.add(new REXPDouble(Double.NaN));
//				}
//				headers.add("duration");
//				entries.add(new REXPDouble(Double.NaN));
//				headers.add("systemLoad");
//				entries.add(new REXPDouble(1));
//				
//				RList list = new RList(entries, headers.toArray(new String[headers.size()]));
//				org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//				rEngine.assign("new", exp);
//				
//				exp = rEngine.parseAndEval("prediction <- doForecast(fit="+rFitString+", new=new, predictDuration=F)");
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
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			} catch (REngineException e) {
//				e.printStackTrace();
//			} catch (REXPMismatchException e) {
//				e.printStackTrace();
//			} 
//		}
//		return probabilities;
//	}


    private String getConflictKey(Collection<Transition> conflictingTransitions, EfficientStochasticNetSemanticsImpl semantics) {
        TreeSet<Short> keys = new TreeSet<>();
        for (Transition t : conflictingTransitions) {
            keys.add(semantics.getTransitionId(t));
        }
        return Arrays.toString(keys.toArray());
    }

//
//	/**
//	 * TODO: make this work from a jar:
//	 * @param metricScript
//	 */
//	private void loadScript(AvailableScripts metricScript) {
//		if (!loadedScripts.contains(metricScript)){
//			String sourceString = "source('"+new File(metricScript.getPath()).getAbsolutePath()+"')";
//			System.out.println(sourceString);
//			REXP exp = engine.eval(sourceString);
//			if (exp != null){
//				loadedScripts.add(metricScript);
//			}
//		}
//	}
//
//	/**
//	 * TODO: make this work from a jar:
//	 * @param metricScript
//	 */
//	private void loadScriptJRI(AvailableScripts metricScript) {
//		if (!loadedScriptsJRI.contains(metricScript)){
//			try {
//				String sourceString = "source('"+new File(metricScript.getPath()).getAbsolutePath()+"')";
//				System.out.println(sourceString);
//				org.rosuda.REngine.REXP exp = rEngine.parseAndEval(sourceString);
//				if (exp != null && !exp.isNull()){
//					loadedScriptsJRI.add(metricScript);
//				}
//			} catch (REngineException e) {
//				e.printStackTrace();
//			} catch (REXPMismatchException e) {
//				e.printStackTrace();
//			}
//		}
//	}

    /**
     *
     */
    protected double sampleDurationForTransition(double positiveConstraint, long startOfTransition, TimedTransition timedT, TimeUnit unitFactor, LimitedTreeMap<Integer, Map<Transition, RealDistribution>> cachedDurations, boolean useOnlyPastTrainingData) {
        if (timedT.getTrainingData() != null) {
//			try{
//			String fitName = null;

            long index = config.getIndexForTime(startOfTransition);
            //Prediction<Double> prediction;
            RealDistribution predictionDist;
            if (cachedPredictedDurations.containsKey(index) && cachedPredictedDurations.get(index).containsKey(timedT)) {
                predictionDist = cachedPredictedDurations.get(index).get(timedT);
            } else {
                TimeSeries<Double> transitionSeries = getTimeSeriesForTransition(timedT);


                // aggregate by average:
                // TODO: avoid reiterating all the training data somehow.
                SortedMultiset<ComparablePair<Long, List<Object>>> trainingDataSoFar = timedT.getTrainingDataUpTo(startOfTransition);
                LinkedList<List<Object>> sortedDurations = new LinkedList<>();
                for (ComparablePair<Long, List<Object>> pair : trainingDataSoFar) {
                    List<Object> list = Arrays.<Object>asList(config.getIndexForTime(pair.getFirst() - (long) ((Double) pair.getSecond().get(0) * unitFactor.getUnitFactorToMillis())),
                            pair.getSecond().get(0),
                            pair.getSecond().get(1));
                    sortedDurations.add(list);
                }

                if (transitionSeries instanceof LastObservationTimeSeries) {
                    // special case: do not aggregate into hourly(or other) intervals, but only return the very last observation as predictor.
                    ((LastObservationTimeSeries) transitionSeries).setLastObservation((Double) sortedDurations.getLast().get(1));
                    return transitionSeries.predict(0).prediction;
                }


//				// aggregate by average:
//				String trainingData = timedT.getTrainingData();
//				Map<Long, List<Object>> sortedDurations = new TreeMap<>();
//				String[] entries = trainingData.split("\n");
//				// ignore header!
//				for (int i = 1; i < entries.length; i++) {
//					String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
//					long time = Long.valueOf(entryParts[2]);
//					sortedDurations.put(time, Arrays.<Object>asList(config.getIndexForTime(time),
//							Double.valueOf(entryParts[0]), Double.valueOf(entryParts[1])));
//				}
                Aggregate.Function<List<Object>, Long> groupBy = new Aggregate.Function<List<Object>, Long>() {
                    @Override
                    public Long apply(List<Object> item) {
                        // group by the time index
                        return (Long) (item.get(0));
                    }
                };

                Map<Long, Double> avgs = Aggregate.avg(sortedDurations, groupBy,
                        new Aggregate.Function<List<Object>, Double>() {
                            @Override
                            public Double apply(List<Object> item) {
                                return (Double) item.get(1);
                            }
                        });

                if (avgs.size() == 1) {
                    DescriptiveStatistics stats = new DescriptiveStatistics();
                    for (ComparablePair<Long, List<Object>> pair : trainingDataSoFar) {
                        stats.addValue(Double.valueOf(pair.getSecond().get(0).toString())); // duration
                    }
                    Prediction<Double> prediction = new Prediction<>();
                    prediction.prediction = avgs.values().iterator().next();
                    if (!(Math.abs(prediction.prediction - stats.getMean()) < 0.001)) {
                        System.err.println("average computation failed! Should be: " + stats.getMean() + ", but is: " + prediction.prediction);
                    }
                    prediction.lower5Percentile = stats.getPercentile(2.5);
                    prediction.upper95Percentile = stats.getPercentile(97.5);
                    predictionDist = getDistributionForPrediction(prediction);

                } else {
                    // go through all elements of the map
                    List<Observation<Double>> observations = new ArrayList<>();

                    double mean = StochasticNetUtils.getMean(avgs.values());

                    Iterator<Long> iter = avgs.keySet().iterator();
                    if (avgs.size() == 0) {
                        System.out.println("debug me!");
                    }
                    Long current = iter.next();
                    Long next = iter.hasNext() ? iter.next() : null;
                    do {
                        Observation<Double> observedAverage = new Observation<>();
                        observedAverage.timestamp = current; // no real time stamp but rather the time index in the time series
                        if (avgs.containsKey(current)) {
                            observedAverage.observation = Double.valueOf(avgs.get(current));
                        } else {
                            // replace with last observation
                            observedAverage.observation = handleMissingValue(observations, config.getMissingDataHandling(), mean);
                        }
                        observations.add(observedAverage);

                        if (next != null && current + 1 < next) {
                            current++;
                        } else {
                            current = next;
                            next = iter.hasNext() ? iter.next() : null;
                        }
                    } while (current != null && current < index);
                    transitionSeries.resetTo(observations);


                    Observation<Double> lastObservation = transitionSeries.getLastObservation();
                    //				long lastIndex = config.getIndexForTime(lastObservation.timestamp);
                    long lastIndex = lastObservation.timestamp;
                    long thisIndex = config.getIndexForTime(startOfTransition);
                    int horizon = (int) (thisIndex - lastIndex);
                    Prediction<Double> prediction = transitionSeries.predict(horizon);
                    predictionDist = getDistributionForPrediction(prediction);
                }
                if (!cachedPredictedDurations.containsKey(index)) {
                    cachedPredictedDurations.put(index, new HashMap<Transition, RealDistribution>());
                }
                cachedPredictedDurations.get(index).put(timedT, predictionDist);
            }
            StochasticNetUtils.setCacheEnabled(false);
            return StochasticNetUtils.sampleWithConstraint(predictionDist, "", positiveConstraint);


//				if (transitionDurationFits.containsKey(timedT)){
//					fitName = transitionDurationFits.get(timedT);
//				} else {
////					loadScript(AvailableScripts.CATEGORICAL_SCRIPT);
////					loadScript(AvailableScripts.METRIC_SCRIPT);
//					
//					loadScriptJRI(AvailableScripts.CATEGORICAL_SCRIPT);
//					loadScriptJRI(AvailableScripts.METRIC_SCRIPT);

//					// create the training data frame:
//					String[] lines = timedT.getTrainingData().split("\n");
//					// duration; systemload; timestamp
//					String[] names = lines[0].split(";");
//					RList list = new RList();
//					
//					// collect training data and sort them according to the date
//					Map<Long, String> sortedDurations = new LimitedTreeMap<>(MAX_SIZE);
//					// ignore header!
//					for (int i = 1; i < lines.length; i++){
//						String[] entryParts = lines[i].split(StochasticManifestCollector.DELIMITER);
//						sortedDurations.put(Long.valueOf(entryParts[2]), entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
//					}
//					// create the matrix
//					double[] timeStamps = new double[sortedDurations.size()];
//					double[] durations =  new double[sortedDurations.size()];
//					int[] systemLoads =  new int[sortedDurations.size()];
//					int i = 0;
//					for (Map.Entry<Long, String> entry : sortedDurations.entrySet()){
//						timeStamps[i] = entry.getKey();
//						String[] parts = entry.getValue().split(StochasticManifestCollector.DELIMITER);
//						durations[i] = Double.valueOf(parts[0]);
//						systemLoads[i] = Integer.valueOf(parts[1]);
//						i++;
//					}
//					// add the data column-wise
//					list.put(names[0],  new REXPDouble(durations));
//					list.put(names[1],  new REXPInteger(systemLoads));
//					list.put(names[2],  new REXPDouble(timeStamps));
//					
////					for (int i = 1; i < lines.length; i++){
////						RList lineList = new RList(Arrays.asList(lines[i].split(";")), names);
////						list.add(lineList);
////					}
//					try {
//						org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//						rEngine.assign("data", exp);
//						
//						rEngine.parseAndEval("df <- runCategoricalPredictionJRI(data=data, timeStamp="+startOfTransition+")");
//						org.rosuda.REngine.REXP size = rEngine.parseAndEval("nrow(df)");
//						if (size.asInteger() <= 1){
//							// only one time point! Can't use time series, just return the ratio
//							return rEngine.parseAndEval("as.numeric(df$duration)").asDouble();
//						}
//						
//						fitName = "fit"+timedT.getLabel().replaceAll("\\s", "_");
//						String fitCommand = fitName + " <- metricFit(df=df, predictDuration=T)";
//						System.out.println(fitCommand);
//						rEngine.parseAndEval(fitCommand);
//					} catch (REXPMismatchException e) {
//						e.printStackTrace();
//					} catch (REngineException e) {
//						e.printStackTrace();
//					}

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

//					this.transitionDurationFits.put(timedT, fitName);
//				}

//				try {
////					File tempFile = java.io.File.createTempFile("training", ".csv");
////					tempFile.deleteOnExit();
////					FileUtils.write(tempFile, timedT.getTrainingData());
////					System.out.println(tempFile.getAbsolutePath());
//					
//					RList list = new RList(Arrays.asList(new REXPDouble(startOfTransition),new REXPDouble(Double.NaN),new REXPDouble(1)), new String[]{"timestamp", "duration", "systemLoad"});
//					org.rosuda.REngine.REXP exp = org.rosuda.REngine.REXP.createDataFrame(list);
//					rEngine.assign("new", exp);
//					
//					
//					String predictionCommand = "prediction <- doForecast(fit = "+fitName+", new=new, predictDuration=T)";
//					org.rosuda.REngine.REXP predictedDuration = rEngine.parseAndEval(predictionCommand);
//					double[] predictionArray = predictedDuration.asDoubles();
//					NormalDistribution dist = new NormalDistribution(predictionArray[0], (predictionArray[0]-predictionArray[2]) / 2 ); // 95% equals to roughly 2 sigma in an assumed normal distribution
//					StochasticNetUtils.setCacheEnabled(false);
//					return StochasticNetUtils.sampleWithConstraint(dist, "", positiveConstraint);
//				} catch (REXPMismatchException e) {
//					e.printStackTrace();
//				} catch (REngineException e) {
//					e.printStackTrace();
//				}
//				catch (IOException e) {
//					e.printStackTrace();
//				}


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
        return super.sampleDurationForTransition(positiveConstraint, startOfTransition, timedT, unitFactor, cachedDurations, useOnlyPastTrainingData);

    }


    private RealDistribution getDistributionForPrediction(Prediction<Double> prediction) {
        if (prediction.upper95Percentile - prediction.lower5Percentile < 0.00001) {
            return new DiracDeltaDistribution(prediction.prediction);
        }
        return new NormalDistribution(prediction.prediction, (prediction.upper95Percentile - prediction.lower5Percentile) / 2);
    }


    private TimeSeries<Double> getTimeSeriesForTransition(TimedTransition timedT) {
        if (cachedTransitionTimeSeries.containsKey(timedT)) {
            return cachedTransitionTimeSeries.get(timedT);
        }
        TimeSeries<Double> timeseries = config.createNewTimeSeries(timedT);
        cachedTransitionTimeSeries.put(timedT, timeseries);
        return timeseries;
    }

    public Pair<Integer, Double> pickTransitionAccordingToWeights(Collection<Transition> transitions, Date currentTime, Semantics<Marking, Transition> semantics) {
        if (semantics instanceof EfficientStochasticNetSemanticsImpl) {
            EfficientStochasticNetSemanticsImpl effiSemantics = (EfficientStochasticNetSemanticsImpl) semantics;
            Map<Transition, Double> transitionProbabilities = getTransitionProbabilities(currentTime, 1, transitions, effiSemantics);
            double[] probabilities = new double[transitionProbabilities.size()];
            double cumulativeProbabilities = 0;
            int i = 0;
            for (Transition t : transitions) {
                probabilities[i] = transitionProbabilities.get(t);
                cumulativeProbabilities += probabilities[i];
                i++;
            }
            int index = StochasticNetUtils.getRandomIndex(probabilities, random);
            return new Pair<Integer, Double>(index, probabilities[index] / cumulativeProbabilities);
        } else {
            throw new RuntimeException("Please make sure to use the EfficientStochasticNetSemanticsImpl for time series prediction!");
        }
    }


}
