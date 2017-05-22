package org.processmining.tests.plugins.stochasticnet.forecast;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.StochasticMinerPlugin;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeseriesPredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorPlugin;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by andreas on 2/8/17.
 */
public class ForecastExperimentTest {

    public static final String TRAFFIC_LOG = "bpm2016/Road_Traffic_Fine_Management_Process.xes.gz";
    public static final String TIMESERIES = "Timeseries_";
    public static final String GSPN = "GSPN";

    public void main(String...args) throws Exception {
        XLog log = TestUtils.loadLog(TRAFFIC_LOG);
        System.out.println("loaded log:    \t"+ XConceptExtension.instance().extractName(log));
        System.out.println("traces loaded: \t"+log.size());

        // split in training and test log:
        Pair<XLog, XLog> trainingLogTestLog = StochasticNetUtils.splitTracesBasedOnRatio(log, 0.5);
        XLog trainingLog = trainingLogTestLog.getFirst();

        // use training log to enrich three models:
        // a stochastic petri net, a time series petri net, and a transition system with probabilities (Markov Chain)

        TimeUnit timeunit = TimeUnit.HOURS;

        DescriptiveStatistics statsBefore = StochasticNetUtils.getDurationsStats(trainingLog);

        // discover a "good" Petri net
        Object[] discoveredModel = StochasticMinerPlugin.discoverStochNetModel(StochasticNetUtils.getDummyUIContext(), trainingLog);
        StochasticNet net = (StochasticNet) discoveredModel[0];
        Marking initialMarking = (Marking) discoveredModel[1];

        PNSimulator simulator = new PNSimulator();
        PNSimulatorConfig config = new PNSimulatorConfig(statsBefore.getN(), net);
        XLog logSim = simulator.simulate(null, net, StochasticNetUtils.getSemantics(net), config, initialMarking);

        DescriptiveStatistics statsSimulated = StochasticNetUtils.getDurationsStats(logSim);
        System.out.println("stats before: "+statsBefore.toString());
        System.out.println("stats after enrichment: "+statsSimulated.toString());
        System.out.println("mean duration before in days: "+statsBefore.getMean() / TimeUnit.DAYS.toMillis(1));
        System.out.println("mean duration after in days: "+statsSimulated.getMean() / TimeUnit.DAYS.toMillis(1));
        System.out.println("bias in the mean in days: "+((statsSimulated.getMean() - statsBefore.getMean())/TimeUnit.DAYS.toMillis(1)));
        // StochasticNetUtils.exportAsDOTFile(net, "out", "out.dot");

        // simulate the same number of traces as given in the first half with an exponential arrival rate:

        Collection<TimeseriesPredictor> predictors = new ArrayList<>();

        TimeSeriesConfiguration configuration = new TimeSeriesConfiguration();
        configuration.setTimeSeriesType(TimeSeriesConfiguration.TimeSeriesType.NAIVE_METHOD);
        predictors.add(new TimeseriesPredictor(configuration));

        configuration = new TimeSeriesConfiguration();
        configuration.setTimeSeriesType(TimeSeriesConfiguration.TimeSeriesType.DRIFT_METHOD);
        predictors.add(new TimeseriesPredictor(configuration));

        configuration = new TimeSeriesConfiguration();
        configuration.setTimeSeriesType(TimeSeriesConfiguration.TimeSeriesType.AUTO_ARIMA);
        predictors.add(new TimeseriesPredictor(configuration));


        TimePredictor gspnPredictor = new TimePredictor(false);

        long starttime  = XTimeExtension.instance().extractTimestamp(trainingLog.get(0).get(0)).getTime(); // assume constant interarrival rate for now
        Pair<Long,Long> tracebounds = StochasticNetUtils.getBufferedTraceBounds(trainingLog.get(trainingLog.size()-1));
        long endTime = tracebounds.getSecond();
        double meanTimeBetweenArrivals = ((double)(endTime - starttime) / timeunit.toMillis(1)) / (double)trainingLog.size();
        ExponentialDistribution arrivalDist = new ExponentialDistribution(meanTimeBetweenArrivals);
        long simulationTime = endTime;
        XTrace observedEvents = XFactoryRegistry.instance().currentDefault().createTrace();
        Map<String, List<Pair<Double,Double>>> results = new HashMap<>();
        for (TimeseriesPredictor predictor : predictors) {
            results.put(TIMESERIES +predictor.getCode(), new ArrayList<Pair<Double, Double>>());
        }
        results.put(GSPN, new ArrayList<Pair<Double, Double>>());

        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(net);
        semantics.initialize(net.getTransitions(), initialMarking);
        Marking semanticInitialMarking = new Marking(semantics.getCurrentState());


        for (int i = 0; i < trainingLogTestLog.getSecond().size(); i++){
            simulationTime += (long)(arrivalDist.sample()*timeunit.toMillis(1));
            for (TimeseriesPredictor predictor : predictors) {
                semantics.setCurrentState(semanticInitialMarking);
                Pair<Double, Double> predictionAndConfidence = predictor.predict(net, observedEvents, new Date(simulationTime), false, semantics);
                results.get(TIMESERIES + predictor.getCode()).add(predictionAndConfidence);
            }

            semantics.setCurrentState(semanticInitialMarking);
            Pair<Double, Double> predictionAndConfidence = gspnPredictor.predict(net, observedEvents, new Date(simulationTime), false, semantics);
            results.get(GSPN).add(predictionAndConfidence);
        }

        Map<Integer, Double> realDurations = new HashMap<>();
        for (XTrace result : trainingLogTestLog.getSecond()){
            tracebounds = StochasticNetUtils.getBufferedTraceBounds(result,0);
            double durationInMillis = (tracebounds.getSecond()-tracebounds.getFirst());
            realDurations.put(realDurations.size(), durationInMillis);
        }


        FileUtils.write(new File("prediction_results.csv"), getOutput(results, realDurations, ","));
    }

    private String getOutput(Map<String, List<Pair<Double, Double>>> results, Map<Integer, Double> realDurations, String separator) {
        StringBuilder builder = new StringBuilder();

        builder.append("numCase").append(separator).append("method").append(separator).append("prediction").append(separator).append("confidence_interval").append(separator).append("real_duration").append("\n");

        for (String key : results.keySet()){
            int i = 0;
            for (Pair<Double,Double> prediction : results.get(key)){
                builder.append(i).append(separator).append(key).append(separator).append(prediction.getFirst()).append(separator).append(prediction.getSecond());
                builder.append(separator).append(realDurations.get(i)).append("\n");
                i++;
            }
        }
        return builder.toString();
    }


}
