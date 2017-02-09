package org.processmining.plugins.stochasticpetrinet.prediction.experiment;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.classification.*;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;
import org.processmining.models.graphbased.directed.transitionsystem.payload.event.EventPayloadTransitionSystem;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherPlugin;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.transitionsystem.miner.TSMiner;
import org.processmining.plugins.transitionsystem.miner.TSMinerInput;
import org.processmining.plugins.transitionsystem.miner.TSMinerOutput;
import org.processmining.plugins.transitionsystem.miner.modir.TSMinerModirInput;
import org.processmining.plugins.transitionsystem.miner.util.TSAbstractions;
import org.processmining.plugins.transitionsystem.miner.util.TSDirections;
import org.processmining.plugins.tsanalyzer.TSAnalyzerPlugin;
import org.processmining.plugins.tsanalyzer.annotation.time.TimeStateAnnotation;
import org.processmining.plugins.tsanalyzer.annotation.time.TimeTransitionSystemAnnotation;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Andreas Rogge-Solti
 */
public class PredictionExperimentPlugin {

    /**
     * number of iterations to split the log in: 10 is a usual tradeoff between
     * performance and accuracy.
     */
    private static final int FOLDS_IN_EXPERIMENT = 10;

    private static final int TRANSITION_SYSTEM_TYPES = 3;
    private static int predictionTypes = TRANSITION_SYSTEM_TYPES + 3;

    String SEPARATOR = ";";

    @Plugin(name = "Predict duration by Simulation (Simulated Experiment)", parameterLabels = {"StochasticNet"}, returnLabels = {"Prediction Results for each trace at multiple points in time"}, returnTypes = {PredictionExperimentResult.class}, userAccessible = true, help = "Predicts the remaining duration for a given trace and a model and time.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public PredictionExperimentResult predict(final UIPluginContext context, StochasticNet model) {
        PredictionExperimentConfig config = new PredictionExperimentConfig();
        config.letUserChooseValues(context);

        // generate some traces
        PNSimulator simulator = new PNSimulator();
        PNSimulatorConfig simConfig = new PNSimulatorConfig(config.getSimulatedTraces(), config.getTimeUnitFactor());

        Marking initialMarking = StochasticNetUtils.getInitialMarking(context, model);
        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(model);

        XLog simulatedLog = simulator.simulate(context, model, semantics, simConfig, initialMarking);

        return predictWithConfig(context, model, simulatedLog, config);
    }

    @Plugin(name = "Predict durations (Real Experiment)", parameterLabels = {"PetriNet", "Log"}, returnLabels = {"Prediction / Real Value Pairs for each trace at multiple points in time"}, returnTypes = {PredictionExperimentResult.class}, userAccessible = true, help = "Predicts the remainding duration for a given trace and a model and time.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public PredictionExperimentResult predict(final UIPluginContext context, Petrinet model, XLog log) {

        PredictionExperimentConfig config = new PredictionExperimentConfig();
        boolean performExperiment = config.letUserChooseValues(context);
        if (performExperiment) {
            return predictWithConfig(context, model, log, config);
        } else {
            context.getFutureResult(0).cancel(true);
            return null;
        }

    }

    /**
     * Perform 10-fold cross validation with the log!
     *
     * @param context
     * @param model
     * @param log
     * @param config
     * @return
     */
    public PredictionExperimentResult predictWithConfig(final PluginContext context, Petrinet model, XLog log,
                                                        PredictionExperimentConfig config) {
        PredictionExperimentResult result = new PredictionExperimentResult();
        result.setPredictionResults(new LinkedList<List<PredictionData>>());
        // 10-fold validation:
        for (int kFold = 0; kFold < FOLDS_IN_EXPERIMENT; kFold++) {
            XLog trainingLog = StochasticNetUtils.filterTracesBasedOnModulo(log, FOLDS_IN_EXPERIMENT, kFold, false);
            XLog predictionLog = StochasticNetUtils.filterTracesBasedOnModulo(log, FOLDS_IN_EXPERIMENT, kFold, true);

            // mine stochastic Petri Net from training Log (9/10 of original traces)
            StochasticNet stochasticNet = null;
            StochasticNet exponentialNet = null;
            Marking finalPlainMarking = StochasticNetUtils.getFinalMarking(context, model);
            Marking finalStochasticMarking = null;
            Marking initialStochasticMarking = null;
            if (!config.getLearnSPNFromData() && model instanceof StochasticNet) {
                stochasticNet = (StochasticNet) model;
                initialStochasticMarking = StochasticNetUtils.getInitialMarking(context, stochasticNet);
                finalStochasticMarking = finalPlainMarking;

                exponentialNet = (StochasticNet) ToStochasticNet.fromStochasticNet(context, stochasticNet,
                        initialStochasticMarking)[0];
                exponentialNet = StochasticNetUtils.convertToGSPN(exponentialNet);

            } else {
                if (!config.getLearnSPNFromData()) {
                    System.out
                            .println("Learning stochastic Petri net from data, as no stochastic Petri net was passed!");
                }
                Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, model, trainingLog, true, true);
                PerformanceEnricherConfig performanceEnricherConfig = new PerformanceEnricherConfig(
                        config.getLearnedDistributionType(), config.getTimeUnitFactor(), config.getExecutionPolicy(),
                        null);
                Object[] netAndMarking = PerformanceEnricherPlugin.transform(context, manifest,
                        performanceEnricherConfig);
                performanceEnricherConfig.setType(DistributionType.EXPONENTIAL);
                Object[] exponentialNetAndMarking = PerformanceEnricherPlugin.transform(context, manifest,
                        performanceEnricherConfig);
                stochasticNet = (StochasticNet) netAndMarking[0];
                exponentialNet = (StochasticNet) exponentialNetAndMarking[0];
                Marking initialExponentialNetMarking = (Marking) exponentialNetAndMarking[1];
                StochasticNetUtils.cacheInitialMarking(exponentialNet, initialExponentialNetMarking);
                stochasticNet.getAttributeMap().put(StochasticNetUtils.ITERATION_KEY, kFold);
                exponentialNet.getAttributeMap().put(StochasticNetUtils.ITERATION_KEY, kFold);

                initialStochasticMarking = (Marking) netAndMarking[1];
                StochasticNetUtils.cacheInitialMarking(stochasticNet, initialStochasticMarking);

                finalStochasticMarking = new Marking();
                Marking finalExponentialNetMarking = new Marking();
                for (Place p : stochasticNet.getPlaces()) {
                    for (Place markingPlace : finalPlainMarking) {
                        if (p.getLabel().equals(markingPlace.getLabel())) {
                            finalStochasticMarking.add(p);
                            finalExponentialNetMarking.add(p);
                        }
                    }
                }
                StochasticNetUtils.cacheFinalMarking(exponentialNet, finalExponentialNetMarking);
                StochasticNetUtils.cacheFinalMarking(stochasticNet, finalStochasticMarking);
                if (context != null) {
                    context.addConnection(new FinalMarkingConnection(stochasticNet, finalStochasticMarking));
                }
            }

            TimeTransitionSystemAnnotation[] transitionSystemAnnotations = getTimeTransitionSystemAnnotations(context,
                    trainingLog);

            double meanDurationMillies = StochasticNetUtils.getMeanDuration(trainingLog);

            PredictionExperimentResult predictions = predict(context, stochasticNet, exponentialNet,
                    transitionSystemAnnotations, predictionLog, config, meanDurationMillies);
            result.getPredictionResults().addAll(predictions.getPredictionResults());
            printResultsToCSV(config, result);
        }

        printResultsToCSV(config, result);

        return result;
    }

    private void printResultsToCSV(PredictionExperimentConfig config, PredictionExperimentResult result) {
        List<List<PredictionData>> results = result.getPredictionResults();
        Iterator<List<PredictionData>> caseIterator = results.iterator();
        int caseId = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(getHeader(getMaxIterations(results)).replaceAll(" ", "_"));
        //		String predictionsCSV = getHeader(getMaxIterations(results));;

        int[] predictionfailsOverall = new int[predictionTypes];
        Arrays.fill(predictionfailsOverall, 0);

        while (caseIterator.hasNext()) {
            List<PredictionData> predictionsForCase = caseIterator.next();
            Iterator<PredictionData> predictionIterator = predictionsForCase.iterator();
            int predictionNo = 0;
            PredictionData data = predictionsForCase.get(0);

            //			String caseString = caseId+SEPARATOR+(data.getCaseEndDate().getTime()-data.getCaseStartDate().getTime());
            sb.append(caseId).append(SEPARATOR)
                    .append(data.getCaseEndDate().getTime() - data.getCaseStartDate().getTime());

            int[] predictionfails = new int[predictionTypes];
            Arrays.fill(predictionfails, 0);
            int predictionErrors = 0;
            int predictionsMade = 0;

            while (predictionIterator.hasNext()) {
                PredictionData prediction = predictionIterator.next();
                predictionsMade++;
                //				Iteration"+SEPARATOR+"Time passed"+SEPARATOR+"Real remaining"+SEPARATOR+
                //				"Pred (process avg)"+SEPARATOR+"Pred (state single)"+SEPARATOR+"Pred (state list)"+SEPARATOR+"Pred (state set)"+SEPARATOR+
                //				"Pred (state bag)"+SEPARATOR+"Pred (Pnet simple)"+SEPARATOR+"Pred (Pnet constrained)"+
                //				"Error (process avg)"+SEPARATOR+"Error (state single)"+SEPARATOR+"Error (state list)"+SEPARATOR+"Error (state set)"+SEPARATOR+
                //				"Error (state bag)"+SEPARATOR+"Error (Pnet simple)"+SEPARATOR+"Error (Pnet constrained)";

                long timePassed = prediction.getPredictionTimeDate().getTime()
                        - prediction.getCaseStartDate().getTime();
                long realRemaining = prediction.getCaseEndDate().getTime()
                        - prediction.getPredictionTimeDate().getTime();
                sb.append(SEPARATOR).append(predictionNo).append(SEPARATOR).append(timePassed).append(SEPARATOR)
                        .append(realRemaining);
                //caseString += SEPARATOR+predictionNo+SEPARATOR+timePassed+SEPARATOR+realRemaining;
                //String predictions = "";
                //String errors = "";

                StringBuilder predictions = new StringBuilder();
                StringBuilder errors = new StringBuilder();

                for (int i = 0; i < prediction.getPredictionDates().length; i++) {
                    Date predictionDate = prediction.getPredictionDates()[i];
                    if (predictionDate == null) {
                        predictionErrors++;
                        predictionfails[i]++;
                        predictionfailsOverall[i]++;
                        predictionDate = prediction.getPredictionTimeDate();
                    }
                    long predictedDuration = predictionDate.getTime() - prediction.getPredictionTimeDate().getTime();
                    long predictionError = predictionDate.getTime() - prediction.getCaseEndDate().getTime();
                    predictions.append(SEPARATOR).append(predictedDuration);
                    errors.append(SEPARATOR).append(predictionError);
                    //					predictiond += SEPARATOR+predictedDuration;
                    //					errors += SEPARATOR+predictionError;
                }
                sb.append(predictions).append(errors);
                //				caseString+=predictions+errors;
                predictionNo++;
            }
            if (predictionErrors > 0) {
                System.out.println("Case " + caseId + " failed in: " + Arrays.toString(predictionfails) + " of "
                        + predictionsMade + " cases.");
            }
            sb.append("\n");
            //			caseString += "\n";
            //			predictionsCSV += caseString;

            caseId++;
        }
        System.out.println("Overall prediction failures: " + Arrays.toString(predictionfailsOverall) + ")");
        StochasticNetUtils.writeStringToFile(sb.toString(), config.getResultFileName());
    }

    private int getMaxIterations(List<List<PredictionData>> results) {
        int maxIterations = 0;
        for (List<PredictionData> data : results) {
            maxIterations = Math.max(maxIterations, data.size());
        }
        return maxIterations;
    }

    //	private void printStats(List<List<Pair<Double, Double>>> predictions) {
    //		try {
    //			File file = new File("outputPredictions.csv");
    //			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    //
    //			String header = "TraceNo."+SEPARATOR+"Iteration"+SEPARATOR+"Prediction"+SEPARATOR+"Real Value"+SEPARATOR+"\n";
    //			writer.write(header);
    //			int traceId = 0;
    //			for (List<Pair<Double,Double>> tracePredictions : predictions){
    //				traceId++;
    //				int monitoringId = 0;
    //				for (Pair<Double,Double> predictionAndValue : tracePredictions){
    //					monitoringId++;
    //					writer.write(traceId+SEPARATOR+monitoringId+SEPARATOR+predictionAndValue.getFirst()+SEPARATOR+predictionAndValue.getSecond()+SEPARATOR+"\n");
    //				}
    //			}
    //			writer.flush();
    //			writer.close();
    //		} catch (IOException e) {
    //			// TODO Auto-generated catch block
    //			e.printStackTrace();
    //		}
    //	}

    private String getHeader(int maxIters) {
        String header = "";
        header += "TraceNo." + SEPARATOR + "Real duration (ms)";
        String headerForIteration = SEPARATOR + "Iteration" + SEPARATOR + "Time passed" + SEPARATOR + "Real remaining"
                + SEPARATOR + "Pred (process avg)" + SEPARATOR
                + "Pred (state single)"
                + SEPARATOR
                + "Pred (state list)"
                + SEPARATOR
                + "Pred (state set)"
                + SEPARATOR
                +
                //				"Pred (state bag)"+SEPARATOR+
                //				"Pred (state list 2)"+SEPARATOR+"Pred (state set 2)"+SEPARATOR+
                //				"Pred (state bag 2)"+SEPARATOR+
                "Pred (Pnet simple)" + SEPARATOR + "Pred (Pnet constrained)" + SEPARATOR + "Error (process avg)"
                + SEPARATOR + "Error (state single)" + SEPARATOR + "Error (state list)" + SEPARATOR
                + "Error (state set)" + SEPARATOR +
                //				"Error (state bag)"+SEPARATOR+
                //				"Error (state list 2)"+SEPARATOR+"Error (state set 2)"+SEPARATOR+
                //				"Error (state bag 2)"+SEPARATOR+
                "Error (Pnet simple)" + SEPARATOR + "Error (Pnet constrained)";
        for (int i = 0; i < maxIters; i++) {
            header += headerForIteration;
        }
        return header + "\n";
    }

    /**
     * Mines some different transition systems from a log and annotates them
     * with time from the same log. states are determined by 1. last activity 2.
     * list of last activities 3. set of last activities 4. multiset of last
     * activities
     *
     * @param context
     * @param trainingLog
     * @return
     */
    private TimeTransitionSystemAnnotation[] getTimeTransitionSystemAnnotations(PluginContext context, XLog trainingLog) {
        TimeTransitionSystemAnnotation[] transitionSystemAnnotations = new TimeTransitionSystemAnnotation[TRANSITION_SYSTEM_TYPES];
        XEventClassifier[] classifiers = new XEventClassifier[]{new XEventNameClassifier(),
                new XEventResourceClassifier(), new XEventLifeTransClassifier()};
        TSMinerInput input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers),
                new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier()));
        TSMiner miner = new TSMiner(context);

        // single event states
        changeSettings(input, classifiers, TSAbstractions.SET, 1);
        transitionSystemAnnotations[0] = getAnnotatedTransitionSystem(context, miner, input, trainingLog);

        // list of last activities
        input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(
                new XEventNameClassifier(), new XEventLifeTransClassifier()));
        changeSettings(input, classifiers, TSAbstractions.SEQUENCE, 1000);
        transitionSystemAnnotations[1] = getAnnotatedTransitionSystem(context, miner, input, trainingLog);

        // set of last activities
        input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(
                new XEventNameClassifier(), new XEventLifeTransClassifier()));
        changeSettings(input, classifiers, TSAbstractions.SET, 1000);
        transitionSystemAnnotations[2] = getAnnotatedTransitionSystem(context, miner, input, trainingLog);

        //		// multibag of last activities
        //		input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(new XEventNameClassifier(),new XEventLifeTransClassifier()));
        //		changeSettings(input, classifiers, TSAbstractions.BAG, 1000);
        //		transitionSystemAnnotations[3] = getAnnotatedTransitionSystem(context, miner, input, analyzer, trainingLog);
        //
        //		// list of last activities
        //		input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(new XEventNameClassifier(),new XEventLifeTransClassifier()));
        //		changeSettings(input, classifiers, TSAbstractions.SEQUENCE, 2);
        //		transitionSystemAnnotations[4] = getAnnotatedTransitionSystem(context, miner, input, analyzer, trainingLog);
        //
        //		// set of last activities
        //		input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(new XEventNameClassifier(),new XEventLifeTransClassifier()));
        //		changeSettings(input, classifiers, TSAbstractions.SET, 2);
        //		transitionSystemAnnotations[5] = getAnnotatedTransitionSystem(context, miner, input, analyzer, trainingLog);
        //
        //		// multibag of last activities
        //		input = new TSMinerInput(context, trainingLog, Arrays.asList(classifiers), new XEventAndClassifier(new XEventNameClassifier(),new XEventLifeTransClassifier()));
        //		changeSettings(input, classifiers, TSAbstractions.BAG, 2);
        //		transitionSystemAnnotations[6] = getAnnotatedTransitionSystem(context, miner, input, analyzer, trainingLog);

        return transitionSystemAnnotations;
    }

    private void changeSettings(TSMinerInput input, XEventClassifier[] classifiers, TSAbstractions abstractions,
                                int horizon) {
        for (XEventClassifier classifier : classifiers) {
            TSMinerModirInput modirInput = input.getModirSettings(TSDirections.BACKWARD, classifier);
            modirInput.setAbstraction(abstractions);
            modirInput.setUse(true);
            modirInput.setFilteredHorizon(horizon);
        }
    }

    private TimeTransitionSystemAnnotation getAnnotatedTransitionSystem(PluginContext context, TSMiner miner,
                                                                        TSMinerInput input, XLog log) {
        TSMinerOutput result = miner.mine(input);
        EventPayloadTransitionSystem ts = result.getTransitionSystem();
        return TSAnalyzerPlugin.simple(context, ts, log).getTimeAnnotation();
    }

    /**
     * Returns the mean absolute percentage error
     *
     * @param predictionPairs a list of pairs containing the prediction in the first field
     *                        and the real value in the second field.
     * @return the MAPE (which has it's problems, if the real value is very
     * small, i.e., near 0)
     */
    private Double getMAPE(List<List<Pair<Double, Double>>> predictionPairs) {
        DescriptiveStatistics errorStats = new DescriptiveStatistics();
        for (List<Pair<Double, Double>> tracePredictions : predictionPairs) {
            for (Pair<Double, Double> predictionAndValue : tracePredictions) {
                double error = predictionAndValue.getSecond() - predictionAndValue.getFirst(); // real value - forecast value
                double percentageError = 100 * error / predictionAndValue.getSecond();
                errorStats.addValue(Math.abs(percentageError));
            }
        }
        return errorStats.getMean();
    }

    /**
     * @param model  SPN model learned from historical data (or real parametric
     *               model)
     * @param log    test log to test predictions made with the SPN model and
     *               transition systems
     * @param config {@link PredictionExperimentConfig} stores parameters used to
     *               make predictions
     * @return Pairs of predicted durations and real durations for each trace in
     * the log and each monitoring iteration defined in
     * {@link PredictionExperimentConfig#getMonitoringIterations()}
     */
    public PredictionExperimentResult predict(PluginContext context, StochasticNet model, StochasticNet gspnModel,
                                              TimeTransitionSystemAnnotation[] transitionSystemAnnotations, XLog log, PredictionExperimentConfig config,
                                              double meanDuration) {
        PredictionExperimentResult result = new PredictionExperimentResult();
        List<List<PredictionData>> predictionsAndRealValues = new ArrayList<List<PredictionData>>();

        if (context != null) {
            context.getProgress().setIndeterminate(true);
            context.getProgress().setMinimum(0);
            context.getProgress().setValue(0);
            context.getProgress().setMaximum(log.size());
            context.getProgress().setIndeterminate(false);
        }

        EventPayloadTransitionSystem[] transitionSystems = new EventPayloadTransitionSystem[transitionSystemAnnotations.length];
        for (int i = 0; i < transitionSystemAnnotations.length; i++) {
            TimeTransitionSystemAnnotation timeTransitionAnnotation = transitionSystemAnnotations[i];
            // get transition graph:
            TimeStateAnnotation firstAnnotation = timeTransitionAnnotation.getAllStateAnnotations().iterator().next();
            TransitionSystem transitionSystem = firstAnnotation.getState().getGraph();
            if (transitionSystem instanceof EventPayloadTransitionSystem) {
                EventPayloadTransitionSystem epTransitionSystem = (EventPayloadTransitionSystem) transitionSystem;
                transitionSystems[i] = epTransitionSystem;
            } else {
                throw new IllegalArgumentException(
                        "TimeTransitionSystemAnnotation is not annotating an event payload transition system. Please annotate mined models only (for now).");
            }
        }
        // prediction period is such that we do equally distributed monitoring iterations half before and half after the process mean duration
        Double predictionPeriod = (meanDuration / config.getMonitoringIterations()) * 2;

        PredictionExperimentWorker[] workers = new PredictionExperimentWorker[config.getWorkerCount()];

        ExecutorService executor = Executors.newFixedThreadPool(workers.length);
        int traceId = 0;
        for (int i = 0; i < workers.length; i++) {
            XLog testLog = StochasticNetUtils.filterTracesBasedOnModulo(log, workers.length, i, true);
            workers[i] = new PredictionExperimentWorker(context, testLog, config, predictionPeriod, model, gspnModel,
                    meanDuration, transitionSystems, transitionSystemAnnotations, traceId, log.size());
            traceId += testLog.size();

            executor.execute(workers[i]);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted!");
            }
        }
        for (int i = 0; i < workers.length; i++) {
            predictionsAndRealValues.addAll(workers[i].predictionsAndRealValues);
        }
        result.setPredictionResults(predictionsAndRealValues);

        return result;
    }

    public long getProcessMeanDuration(StochasticNet model, XLog log, Marking initialMarking,
                                       PredictionExperimentConfig config) {
        TimePredictor predictor = new TimePredictor(false);
        XTrace initialTrace = new XTraceImpl(log.get(0).getAttributes());
        initialTrace.add(log.get(0).get(0));
        XEvent startEvent = initialTrace.get(0);
        Long realStartTime = XTimeExtension.instance().extractTimestamp(startEvent).getTime();
        Pair<Double, Double> meanAndConfidenceInterval = predictor.predict(model, initialTrace,
                new Date(realStartTime), initialMarking);
        long meanDuration = meanAndConfidenceInterval.getFirst().longValue();
        meanDuration = meanDuration - realStartTime;
        return meanDuration;
    }

    private class PredictionExperimentWorker implements Runnable {

        private XLog log;
        private PluginContext context;
        private PredictionExperimentConfig config;
        private Double predictionPeriod;
        private Marking initialMarking;
        private Marking initialGspnMarking;
        private TimePredictor predictor;
        private StochasticNet model;
        private StochasticNet gspnModel;
        private Double meanDuration;
        private EventPayloadTransitionSystem[] transitionSystems;
        private TimeTransitionSystemAnnotation[] transitionSystemAnnotations;
        private int traceId;
        private int traceCount;

        public List<List<PredictionData>> predictionsAndRealValues = new ArrayList<List<PredictionData>>();

        public PredictionExperimentWorker(PluginContext context, XLog testLog, PredictionExperimentConfig config,
                                          Double predictionPeriod, StochasticNet model, StochasticNet gspnModel, Double meanDuration,
                                          EventPayloadTransitionSystem[] transitionSystems,
                                          TimeTransitionSystemAnnotation[] transitionSystemAnnotations, int traceId, int traceCount) {
            this.context = context;
            this.log = testLog;
            this.config = config;
            this.predictionPeriod = predictionPeriod;
            this.initialMarking = StochasticNetUtils.getInitialMarking(context, model);
            this.initialGspnMarking = StochasticNetUtils.getInitialMarking(context, gspnModel);
            this.predictor = new TimePredictor(true);
            this.model = model;
            this.gspnModel = gspnModel;
            this.meanDuration = meanDuration;
            this.transitionSystems = transitionSystems;
            this.transitionSystemAnnotations = transitionSystemAnnotations;
            this.traceId = traceId;
            this.traceCount = traceCount;
        }

        public void run() {
            double pos = 0;
            long start = System.currentTimeMillis();
            NumberFormat format = NumberFormat.getPercentInstance();
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
            String estimate = "(unknown)";
            for (XTrace trace : log) {
                traceId++;
                if (pos > 0) {
                    long now = System.currentTimeMillis();
                    long passed = now - start;
                    long passedMilliesPerIteration = passed / (int) pos;
                    long estimated = passedMilliesPerIteration * (log.size() - (int) pos);
                    estimate = DurationFormatUtils.formatDuration(estimated, "HH:mm:ss:SS");
                }
                String percent = format.format(pos++ / log.size());
                String s = "Finished prediction of trace " + (traceId) + " of " + traceCount + " (" + percent
                        + ") rem. time:" + estimate;
                if (context != null) {
                    context.getProgress().inc();
                    //					context.log("Starting prediction of trace "+traceId+"...");
                    context.log("Starting prediction of trace " + (traceId) + " of " + traceCount + " (" + percent
                            + ")... rem. time:" + estimate);
                }
                if (trace.size() >= 2) {
                    List<PredictionData> predictionsAndRealValuesForThisTrace = new ArrayList<PredictionData>();
                    XEvent event = trace.get(trace.size() - 1);

                    Long realTerminationTime = XTimeExtension.instance().extractTimestamp(event).getTime();

                    event = trace.get(0);
                    Long realStartTime = XTimeExtension.instance().extractTimestamp(event).getTime();

                    for (int i = 0; i < config.getMonitoringIterations(); i++) {
                        Date currentTime = new Date(realStartTime + i * predictionPeriod.longValue());
                        Double realRemainingDuration = (double) (realTerminationTime - currentTime.getTime());
                        if (realRemainingDuration >= 0) {
                            s += ".";
                            XTrace subTrace = StochasticNetUtils.getSubTrace(trace, currentTime.getTime());

                            event = subTrace.get(subTrace.size() - 1);
                            Long realLastEventTime = ((XAttributeTimestamp) event.getAttributes().get(
                                    PNSimulator.TIME_TIMESTAMP)).getValueMillis();

                            Pair<Double, Double> simplePredictionAndConfidence = predictor.predict(gspnModel, subTrace,
                                    currentTime, initialGspnMarking);
                            Pair<Double, Double> constrainedPredictionAndConfidence = predictor.predict(model,
                                    subTrace, currentTime, initialMarking);

                            Double predictedValueSimple = (double) Math.max(simplePredictionAndConfidence.getFirst()
                                    .longValue(), currentTime.getTime());
                            Double predictedValueConstrained = (double) Math.max(constrainedPredictionAndConfidence
                                    .getFirst().longValue(), currentTime.getTime());

                            PredictionData predictions = new PredictionData(traceId, new Date(realStartTime), new Date(
                                    realTerminationTime), new Date(currentTime.getTime()), predictionTypes);
                            predictions.getPredictionDates()[0] = new Date(Math.max(
                                    (long) (realStartTime + meanDuration), currentTime.getTime()));

                            for (int j = 0; j < transitionSystemAnnotations.length; j++) {
                                EventPayloadTransitionSystem transitionSystem = transitionSystems[j];
                                TimeTransitionSystemAnnotation transitionSystemAnnotation = transitionSystemAnnotations[j];
                                org.processmining.models.graphbased.directed.transitionsystem.Transition transition = transitionSystem
                                        .getTransition(trace, subTrace.size() - 1);
                                if (transition != null) {
                                    TimeStateAnnotation timeAnnotation = transitionSystemAnnotation
                                            .getStateAnnotation(transition.getTarget());
                                    predictions.getPredictionDates()[j + 1] = new Date(Math.max(
                                            new Double(timeAnnotation.getRemaining().getAverage()).longValue()
                                                    + realLastEventTime, currentTime.getTime()));
                                }
                            }
                            predictions.getPredictionDates()[transitionSystemAnnotations.length + 1] = new Date(
                                    predictedValueSimple.longValue());
                            predictions.getPredictionDates()[transitionSystemAnnotations.length + 2] = new Date(
                                    predictedValueConstrained.longValue());
                            predictionsAndRealValuesForThisTrace.add(predictions);
                        }
                    }
                    System.out.println(s);
                    predictionsAndRealValues.add(predictionsAndRealValuesForThisTrace);
                } else {
                    System.out.println("Trace " + trace + " has less than 2 events!");
                }
            }
        }

    }
}
