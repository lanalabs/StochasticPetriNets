package org.processmining.plugins.stochasticpetrinet.prediction;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.classification.XEventAndClassifier;
import org.deckfour.xes.classification.XEventLifeTransClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.*;

public abstract class AbstractTimePredictor {

    /**
     * the confidence interval to be used for estimating bounds on the predicted remaining duration
     */
    public static final double CONFIDENCE_INTERVAL = 0.99;

    /**
     * Simulation is allowed to stop, when relative error is below this value
     */
    public static final double ERROR_BOUND_PERCENT = 3;

    public static final double ABS_ERROR_THRESHOLD = 10000;

    /**
     * If we wanted to restrict the number of simulated runs, we could do it here
     */
    public static final int MAX_RUNS = Integer.MAX_VALUE;

    private static final Map<Integer, Map<Double, Double>> confidenceCache = new HashMap<>();

    public Pair<Double, Double> predict(StochasticNet model, XTrace observedEvents, Date currentTime, Marking initialMarking) {
        return predict(model, observedEvents, currentTime, initialMarking, false);
    }


    public Pair<Double, Double> predict(StochasticNet model, XTrace observedEvents, Date currentTime, boolean useOnlyPastTrainingData, Semantics<Marking, Transition> semantics) {
        DescriptiveStatistics stats = getPredictionStats(model, observedEvents, currentTime, useOnlyPastTrainingData, semantics);
        return new Pair<Double, Double>(stats.getMean(), getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL));
    }

    /**
     * Does not care about final markings -> simulates net until no transitions are enabled any more...
     * Time
     *
     * @param {@link                  StochasticNet} model the model capturing the stochastic behavior of the net
     * @param observedEvents          the monitored partial trace (complete, i.e., no visible transition missing)
     * @param currentTime             the time of prediction (can be later than the last event's time stamp)
     * @param initialMarking          initial marking of the net
     * @param useOnlyPastTrainingData indicator, whether the training data needs to be filtered with the current time as upper bound
     * @return {@link Pair} of doubles (the point predictor, and the associated 99 percent confidence interval)
     */
    public Pair<Double, Double> predict(StochasticNet model, XTrace observedEvents, Date currentTime, Marking initialMarking, boolean useOnlyPastTrainingData) {
        Semantics<Marking, Transition> semantics = getSemantics(model, observedEvents, initialMarking);
        return predict(model, observedEvents, currentTime, useOnlyPastTrainingData, semantics);
    }

    public final Semantics<Marking, Transition> getSemantics(StochasticNet model, XTrace observedEvents, Marking initialMarking){
        Semantics<Marking, Transition> semantics = null;
        if (observedEvents.isEmpty()){
            semantics = StochasticNetUtils.getSemantics(model);
            semantics.initialize(model.getTransitions(), initialMarking);
        } else {
            semantics = getCurrentStateWithAlignment(model, initialMarking, observedEvents);
        }
        return semantics;
    }

    /**
     * Maximum likelihood estimate for the risk of missing a deadline until the end of the process.
     * (Currently, we did not implement the time until we reach a certain state)
     *
     * @param model                   StochasticNet capturing the stochastic behavior of the net
     * @param observedEvents          the monitored partial trace (complete, i.e., no visible transition missing)
     * @param currentTime             the time of prediction (can be later than the last event's time stamp)
     * @param targetTime              the deadline with respect to which the risk is calculated
     * @param initialMarking          initial marking of the net
     * @param useOnlyPastTrainingData indicator, whether the training data needs to be filtered with the current time as upper bound
     * @return
     */
    public Double computeRiskToMissTargetTime(StochasticNet model, XTrace observedEvents, Date currentTime, Date targetTime, Marking initialMarking, boolean useOnlyPastTrainingData) {
        Semantics<Marking, Transition> semantics = getSemantics(model, observedEvents, initialMarking);
        DescriptiveStatistics stats = getPredictionStats(model, observedEvents, currentTime, useOnlyPastTrainingData, semantics);
        double[] sortedEstimates = stats.getSortedValues();
        long[] longArray = new long[sortedEstimates.length];
        for (int i = 0; i < sortedEstimates.length; i++) {
            longArray[i] = (long) sortedEstimates[i];
        }
        // use discounting to avoid extreme probabilities
        return 1 - (StochasticNetUtils.getIndexBinarySearch(longArray, targetTime.getTime()) + 0.5) / (sortedEstimates.length + 1);
    }

    /**
     * Computes some stats by running a Monte Carlo simulation of the process.
     *
     * @param model                   the model that is enriched by some training data
     * @param observedEvents          the current history of the trace (observed events so far)
     * @param currentTime             the current time at prediction
     * @param useOnlyPastTrainingData indicator that tells us whether to only rely on training data that was observed in the past (relative to the currentTime)
     * @param semantics               the semantics with the current marking of the model that shows the starting point
     * @return {@link DescriptiveStatistics} gathered from a set of simulated continuations of the current process
     */
    protected abstract DescriptiveStatistics getPredictionStats(StochasticNet model, XTrace observedEvents, Date currentTime, boolean useOnlyPastTrainingData, Semantics<Marking, Transition> semantics);

    protected double getConfidenceIntervalWidth(DescriptiveStatistics summaryStatistics, double confidence) {
        int n = (int) summaryStatistics.getN() - 1;
        if (!confidenceCache.containsKey(n)) {
            confidenceCache.put(n, new HashMap<Double, Double>());
        }
        double a;
        if (!confidenceCache.get(n).containsKey(confidence)) {
            TDistribution tDist = new TDistribution(summaryStatistics.getN() - 1);
            a = tDist.inverseCumulativeProbability(1 - ((1 - confidence) / 2.));
            confidenceCache.get(n).put(confidence, a);
        } else {
            a = confidenceCache.get(n).get(confidence);
        }
        //tDist.inverseCumulativeProbability(1-((1-confidence) / 2.));
        return 2 * a * Math.sqrt(summaryStatistics.getVariance() / summaryStatistics.getN());
    }

    protected double getErrorPercent(DescriptiveStatistics stats) {
        double mean = stats.getMean();
        double confidenceIntervalWidth = getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL);
        return (mean / (mean - confidenceIntervalWidth / 2.) - 1) * 100;
    }

    protected double getError(DescriptiveStatistics stats) {
        return getConfidenceIntervalWidth(stats, CONFIDENCE_INTERVAL) / 2;
    }


    /**
     * TODO: Maybe switch to alignment approach
     *
     * @param model
     * @param initialMarking
     * @param observedEvents
     * @return
     */
    public static Semantics<Marking, Transition> getCurrentState(StochasticNet model, Marking initialMarking, XTrace observedEvents) {
        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(model);
        semantics.initialize(model.getTransitions(), initialMarking);
        for (XEvent event : observedEvents) {
            Set<Marking> visitedMarkings = new HashSet<Marking>();
            String transitionName = XConceptExtension.instance().extractName(event);
            Long time = XTimeExtension.instance().extractTimestamp(event).getTime();
            boolean foundTransition = false;
            // breadth-width search for the event transition in the graph from the current marking
//			
//			foundTransition = findAndExecuteTransition(semantics, transitionName, time);
//			if (!foundTransition){
            ArrayList<Pair<Marking, Transition>> transitionQueue = new ArrayList<Pair<Marking, Transition>>();
            addAllEnabledTransitions(semantics, transitionQueue);
            while (!foundTransition && transitionQueue.size() > 0) {
                Pair<Marking, Transition> currentState = transitionQueue.remove(0);
                semantics.setCurrentState(currentState.getFirst());
                Transition enabledTransition = currentState.getSecond();
                try {
                    if (enabledTransition.getLabel().equals(transitionName)) {
                        foundTransition = true;
                        executeTransition(semantics, enabledTransition, time);
                    } else {
                        semantics.executeExecutableTransition(enabledTransition);
                        if (!visitedMarkings.contains(semantics.getCurrentState())) {
                            addAllEnabledTransitions(semantics, transitionQueue);
                            visitedMarkings.add(semantics.getCurrentState());
                        }
                    }
                } catch (IllegalTransitionException e) {
                    e.printStackTrace();
                }
            }
            if (!foundTransition) {
                throw new IllegalArgumentException("Did not find transition for \"" + transitionName + "\".");
            }
//			}
        }
        return semantics;
    }

    public static Semantics<Marking, Transition> getCurrentStateWithAlignment(StochasticNet model, Marking initialMarking, XTrace observedEvents) {
        Semantics<Marking, Transition> stochasticSemantics = StochasticNetUtils.getSemantics(model);
        // when replaying, don't worry about temporal semantics, just use plain Petri net ones.
        Semantics<Marking, Transition> plainSemantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
        stochasticSemantics.initialize(model.getTransitions(), initialMarking);
        plainSemantics.initialize(model.getTransitions(), initialMarking);

        XLog log = XFactoryRegistry.instance().currentDefault().createLog();
//		log.getClassifiers().add(new XEventNameClassifier());
        log.add(observedEvents);
        TransEvClassMapping mapping = StochasticNetUtils.getEvClassMapping(model, log);

        try {

            Pair<SyncReplayResult, PNManifestFlattener> result = StochasticNetUtils.replayTrace(log, mapping, model, initialMarking, StochasticNetUtils.getFinalMarking(null, model), new XEventAndClassifier(new XEventNameClassifier(), new XEventLifeTransClassifier()));
            //SyncReplayResult result = StochasticNetUtils.replayTrace(log, mapping, model, initialMarking, StochasticNetUtils.getFinalMarking(null, model), new XEventNameClassifier());
            List<StepTypes> stepTypes = result.getFirst().getStepTypes();
            List<Object> nodeInstances = result.getFirst().getNodeInstance();

            // advance the model to the last synchronous move

            // find the last synchronous move:
            int lastSynchronousMove = -1;
            for (int i = 0; i < stepTypes.size(); i++) {
                StepTypes stepType = stepTypes.get(i);
                if (stepType.equals(StepTypes.LMGOOD)) {
                    lastSynchronousMove = i;
                }
            }


            for (int i = 0; i < stepTypes.size(); i++) {
                StepTypes stepType = stepTypes.get(i);
                if (stepType.equals(StepTypes.L)) {
                    // ignore log only moves when unrolling
                } else {
                    // move on model (or on both) advance model until we reached the last synchronous move
                    if (i <= lastSynchronousMove) {
                        Transition nodeInstance = (Transition) nodeInstances.get(i);
                        Collection<Transition> transitions = plainSemantics.getExecutableTransitions();
                        Transition selectedTrans = result.getSecond().getOrigTransFor(nodeInstance);
                        try {
                            if (selectedTrans != null) {
                                plainSemantics.executeExecutableTransition(selectedTrans);
                            } else {
                                System.err.println("Debug me!");
                            }
                        } catch (IllegalTransitionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stochasticSemantics.setCurrentState(plainSemantics.getCurrentState());
        return stochasticSemantics;
    }


    private static Transition getTransition(Collection<Transition> transitions, Transition nodeInstance) {
        for (Transition t : transitions) {
            if (t.getLabel().equals(nodeInstance.getLabel())) {
                return t;
            }
        }
        return null;
    }

    protected static void addAllEnabledTransitions(
            Semantics<Marking, Transition> semantics, ArrayList<Pair<Marking, Transition>> searchState) {
        for (Transition t : semantics.getExecutableTransitions()) {
            if (t.isInvisible()) {
                // visit invisible states first and add them before the visible ones
                boolean added = false;
                for (int i = 0; i < searchState.size() && !added; i++) {
                    if (!searchState.get(i).getSecond().isInvisible() || i == searchState.size() - 1) {
                        searchState.add(i, new Pair<Marking, Transition>(semantics.getCurrentState(), t));
                        added = true;
                    }
                }
            } else {
                searchState.add(new Pair<Marking, Transition>(semantics.getCurrentState(), t));
            }
        }
    }

//	private boolean findAndExecuteTransition(Semantics<Marking, Transition> semantics, String transitionName, Long time) {
//		for (Transition t : semantics.getExecutableTransitions()){
//			if (t.getLabel().equals(transitionName)){
//				try {
//					executeTransition(semantics, t, time);
//					return true;
//				} catch (IllegalTransitionException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//		return false;
//	}

    protected static void executeTransition(Semantics<Marking, Transition> semantics, Transition transition, Long time) throws IllegalTransitionException {
        Marking before = semantics.getCurrentState();
        semantics.executeExecutableTransition(transition);
        Marking after = semantics.getCurrentState();
        Marking oldInvalidPlaces = new Marking(before);
        oldInvalidPlaces.removeAll(after);
        Marking newPlaces = new Marking(after);
        newPlaces.removeAll(before);
//		for (Place p : oldInvalidPlaces){
//			if (placeTimes.containsKey(p) && placeTimes.get(p).size() > 0){
//				placeTimes.get(p).remove(0);
//			}
//		}
//		for (Place p : newPlaces){
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(time);
//		}
    }
}
