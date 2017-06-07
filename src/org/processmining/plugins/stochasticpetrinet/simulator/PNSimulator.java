package org.processmining.plugins.stochasticpetrinet.simulator;

import com.google.common.collect.SortedMultiset;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.*;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.timeseries.StatefulTimeseriesDistribution;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.utils.datastructures.ComparablePair;
import org.utils.datastructures.LimitedTreeMap;
import org.utils.datastructures.Triple;

import java.math.BigDecimal;
import java.util.*;

/**
 * Very plain simulator only used for evaluation of the evaluation of the mining of stochastic Petri nets
 * and the repair-log plug-in. Works with simple stochastic Petri nets.
 * <p>
 * ProM is no simulator!! By all means use CPNTools (http://cpntools.org) if you
 * want to have a feature-rich stable simulation and analysis tool for all kinds of Petri nets.
 *
 * @author Andreas Rogge-Solti
 */
public class PNSimulator {
    public static final String TRANSITION_COMPLETE = "complete";
    public static final String LIFECYCLE_TRANSITION = "lifecycle:transition";
    public static final String TIME_TIMESTAMP = "time:timestamp";
    public static final String CONCEPT_SIMULATED = "concept:simulated";
    public static final String LOCATION_ROOM = "location:room";
    public static final String CONCEPT_INSTANCE = "concept:instance";
    public static final String CONCEPT_NAME = "concept:name";
    public static final String SIMULATED_LOG_PROBABILITY = "simulated:logProbability";

    protected static Random random = new MersenneTwisterRNG();

    protected RealDistribution arrivalDistribution;

    /**
     * Each transition has it's remaining time stored, once it becomes active in a marking.
     * These values are all lowered, by the time elapsed in a marking.
     */
    protected Map<Transition, Long> transitionRemainingTimes;
    /**
     * Stores the time of the last step in the simulation.
     */
    protected long lastFiringTime;

    protected Marking oneMarking;

    /**
     * Needs to be explicitly set by the caller.
     * Switching this on, creates a significant overhead in filtering the training data to only being from the relative past.
     * It is currently the only implemented option, and only used for a rolling cross-validation.
     * In this case, we have one training pass to collect all the data and we filter later based on
     * availability of information at prediction time. The reason for this procedure is that events can be scattered throughout the log,
     * For example, the last observation at transition A could be in a trace that started later than the current one!
     */
    protected boolean useOnlyPastTrainingData = false;

    /**
     * maps from the prediction time point (long since start of epoch) to a cache storing the predictions for each transition
     */
    protected LimitedTreeMap<Integer, Map<Transition, RealDistribution>> cachedDurations;
    protected double logProbabilityOfCurrentTrace;

    public PNSimulator() {
        transitionRemainingTimes = new HashMap<Transition, Long>();
        cachedDurations = new LimitedTreeMap<>(1000);
    }

    /**
     * Asks the user to specify configuration parameters for the simulation.
     * {@link PNSimulatorConfig}
     *
     * @param context
     * @param petriNet
     * @param semantics
     * @return
     */
    public XLog simulate(UIPluginContext context, PetrinetGraph petriNet, Semantics<Marking, Transition> semantics) {
        PNSimulatorConfigUI ui = new PNSimulatorConfigUI(petriNet);
        return simulate(context, petriNet, semantics, ui.getConfig(context), StochasticNetUtils.getInitialMarking(context, petriNet));
    }

    /**
     * Performs a simulation according to a given configuration {@link PNSimulatorConfig}
     *
     * @param context   {@link UIPluginContext} ProM plugin context
     * @param petriNet  a petri net (preferable with stochastic timing information
     * @param semantics a {@link Semantics} according to which simulation should be performed
     * @param config    {@link PNSimulatorConfig}
     * @return
     */
    public XLog simulate(UIPluginContext context, PetrinetGraph petriNet,
                         Semantics<Marking, Transition> semantics, PNSimulatorConfig config, Marking initialMarking) {
        Marking finalMarking = null;
        if (context != null) {
            finalMarking = StochasticNetUtils.getFinalMarking(context, petriNet);
        }
        return simulate(context, petriNet, semantics, config, initialMarking, finalMarking);
    }

    /**
     * Performs a simulation according to a given configuration {@link PNSimulatorConfig}
     *
     * @param context        {@link UIPluginContext} ProM plugin context
     * @param petriNet       a petri net (preferable with stochastic timing information
     * @param semantics      a {@link Semantics} according to which simulation should be performed
     * @param config         {@link PNSimulatorConfig}
     * @param initialMarking
     * @param finalMarking
     * @return
     */
    public XLog simulate(UIPluginContext context, PetrinetGraph petriNet,
                         Semantics<Marking, Transition> semantics, PNSimulatorConfig config, Marking initialMarking, Marking finalMarking) {
        XLog log = null;
        if (initialMarking == null || initialMarking.isEmpty()) {
            if (context != null) {
                context.log("No initial marking found! Trying to use a default one...");
            }
            StochasticNetUtils.getDefaultInitialMarking(petriNet);
        }
        if (oneMarking == null) {
            oneMarking = new Marking(petriNet.getPlaces());
        }
        if (config != null) {
            arrivalDistribution = new ExponentialDistribution(config.arrivalRate);
            transitionRemainingTimes = new HashMap<Transition, Long>();
            random.setSeed(config.seed);
            log = XFactoryRegistry.instance().currentDefault().createLog();
            XConceptExtension.instance().assignName(log, config.logName + " (from " + petriNet.getLabel() + ")");

            Date traceStart = new Date();

            // show progress to the user:
            if (context != null) {
                context.log("Generating " + config.numberOfTraces + " traces...");
                context.getProgress().setMinimum(0);
                context.getProgress().setMaximum(1000);
            }

            if (!config.deterministicBoundedStateSpaceExploration) {
                // do a trace by trace simulation (assumed independence from each other..)
                for (long i = 0; i < config.numberOfTraces; i++) {
                    if (context != null) {
                        context.getProgress().setValue((int) ((i/(double)config.numberOfTraces) * 1000));
                    }

                    traceStart = getNextArrivalDate(traceStart, config.unitFactor);
                    //Map<Place, List<Long>> placeTimes = new HashMap<Place, List<Long>>();
                    //updatePlaceTimes(initialMarking, traceStart, placeTimes);
                    semantics.initialize(petriNet.getTransitions(), initialMarking);

                    XTrace trace = (XTrace) simulateOneTrace(petriNet, semantics, config, initialMarking, traceStart.getTime(), traceStart.getTime(), i, false, finalMarking);
                    trace.getAttributes().put(SIMULATED_LOG_PROBABILITY, new XAttributeContinuousImpl(SIMULATED_LOG_PROBABILITY, this.logProbabilityOfCurrentTrace));
                    log.add(trace);
                }
            } else {
                // explore the state space of the structural model with a breadth first search,
                // but produce only valid traces that correctly finish within a certain threshold.
                long time = System.currentTimeMillis();

                XTrace trace = createTrace(1, config);
                LinkedList<VisitState> statesToVisit = new LinkedList<>();
                statesToVisit.add(new VisitState(trace, initialMarking, time, new BigDecimal(1.0)));
                semantics.initialize(petriNet.getTransitions(), initialMarking);
                Marking endPlaces = getEndPlaces(petriNet);
                if (petriNet.getLabel().equals("s00000633##s00004419")) {
                    throw new IllegalArgumentException("Too many states!");
                }
                addAllDifferentTracesToLog(log, petriNet, statesToVisit, semantics, new HashMap<String, Set<Integer>>(), config, endPlaces);
            }

            if (context != null) {
                context.log(config.numberOfTraces + " traces generated successfully.");
            }
        }
        return log;


    }

    protected Marking getEndPlaces(PetrinetGraph petriNet) {
        Marking endPlaces = new Marking();
        for (Place p : petriNet.getPlaces()) {
            if (petriNet.getOutEdges(p).size() == 0) {
                endPlaces.add(p);
            }
        }
        return endPlaces;
    }

    protected void addAllDifferentTracesToLog(XLog log, PetrinetGraph petriNet, LinkedList<VisitState> statesToVisit,
                                              Semantics<Marking, Transition> semantics, Map<String, Set<Integer>> numberOfDecisionTransitions, PNSimulatorConfig config, Marking endPlaces) {

        BigDecimal cumulativeProb = new BigDecimal(0.0);
        while (!statesToVisit.isEmpty() && cumulativeProb.doubleValue() < config.getQuantile()) {
            VisitState currentStateWithTime = statesToVisit.removeFirst();
            XTrace prefix = currentStateWithTime.getTrace();
            Marking currentMarking = currentStateWithTime.getMarking();
            long time = currentStateWithTime.getTime();
            BigDecimal probability = currentStateWithTime.getProbability();

//			if (prefix.size() > config.maxEventsInOneTrace*10){
//				throw new IllegalArgumentException("Petri net contains a potential lifelock!");
//			}
            if (!config.allowUnbounded && !isOneBounded(currentMarking)) {
                throw new IllegalArgumentException("Petri net is not 1-bounded!");
            }
//			if (statesToVisit.size() > config.maxEventsInOneTraceEventsInOneTrace*10){
//				throw new IllegalArgumentException("Too many states!");
//			}
            if (log.size() > config.numberOfTraces) {
                return;
            }

            if (isFinal(currentMarking, endPlaces)) {
                // ensure proper naming:
                String instance = String.valueOf(log.size());
                XConceptExtension.instance().assignName(prefix, "tr_" + instance);
                for (XEvent e : prefix) {
                    XConceptExtension.instance().assignInstance(e, instance);
                }
                log.add(prefix);
                cumulativeProb = cumulativeProb.add(probability);
            } else {
                // explore all executable transitions:
                semantics.setCurrentState(currentMarking);
                Collection<Transition> executableTransitions = semantics.getExecutableTransitions();
                if (executableTransitions.size() == 0) {
                    throw new IllegalArgumentException("Petri net contains a deadlock!");
                }
                Map<Transition, Double> transitionProbabilities = getTransitionProbabilities(executableTransitions, semantics);

                for (Transition t : executableTransitions) {
//					String markingTransitionCombination = currentMarking.toString()+"_"+t.getLabel()+t.getId();
//					if (!numberOfDecisionTransitions.containsKey(markingTransitionCombination)){
//						numberOfDecisionTransitions.put(markingTransitionCombination, new HashSet<Integer>());
//					}

                    BigDecimal probOfTransition = new BigDecimal(1.0/executableTransitions.size());

                    int numberOfTimesAlreadyInTrace = 0;

                    for (XEvent event : prefix) {
                        if (XConceptExtension.instance().extractName(event).equals(t.getLabel())) {
                            numberOfTimesAlreadyInTrace++;
                        }
                    }
                    // TODO: deal with very large state spaces!!
                    // sampling needs some correction mechanism (maybe use LoLA to estimate state space)
                    // should be able to use that information in a smart way.
                    if (numberOfTimesAlreadyInTrace >= 20) {
                        // old version: numberOfDecisionTransitions.get(markingTransitionCombination).size() > 5

                        // do not explore this transition further...
                    } else {
//						numberOfDecisionTransitions.get(markingTransitionCombination).add(prefix.size());
                        semantics.setCurrentState(currentMarking);
                        XTrace clone = (XTrace) prefix.clone();
                        StochasticNetUtils.updateLogProbability(clone, Math.log(transitionProbabilities.get(t)));
                        if (!t.isInvisible()) {
                            if (!prefix.isEmpty()) {
                                long lastEventTime = XTimeExtension.instance().extractTimestamp(prefix.get(prefix.size() - 1)).getTime();
                                time = Math.max(time, lastEventTime);
                            }
                            if (t instanceof TimedTransition) {
                                TimedTransition tt = (TimedTransition) t;
                                if (tt.getDistribution() instanceof StatefulTimeseriesDistribution) {
                                    ((StatefulTimeseriesDistribution) tt.getDistribution()).setCurrentTime(time);
                                }
                                time += (long) (config.unitFactor.getUnitFactorToMillis() * StochasticNetUtils.sampleWithConstraint(tt, 0.1));
                            } else {
                                time += random.nextDouble() * config.unitFactor.getUnitFactorToMillis();
                            }
                            XEvent e = createSimulatedEvent(t, petriNet, time, XConceptExtension.instance().extractName(clone));
                            clone.add(e);
                        }
                        try {
                            semantics.executeExecutableTransition(t);
                        } catch (IllegalTransitionException e1) {
                            e1.printStackTrace();
                        }
                        statesToVisit.addLast(new VisitState(clone, semantics.getCurrentState(), time, probability.multiply(probOfTransition)));
                    }
                }
            }
        }
    }

    /**
     * Retrieves the transition probabilities (normalized (sums to one) -> one must be chosen).
     * Only considers these cases:
     * <ol>
     * <li> no timed transitions: uniform choice </li>
     * <li> mixed transitions: unrealistic case uniform choice </li>
     * <li> only timed transitions:
     * <ul>
     * <li><b>only immediate transitions:</b> weight ratio </li>
     * <li><b>only timed transitions:</b> wiring rate ratio </li>
     * <li><b>immediate and timed transitions:</b> only immediate are allowed to fire!<br>
     * immediate transitions -> weight ratio <br>
     * timed transitions -> 0</li>
     * </ul>
     * </li>
     * </ol>
     *
     * @param transitions collection of executable transitions
     * @param semantics   {@link Semantics}
     * @return
     */
    private Map<Transition, Double> getTransitionProbabilities(Collection<Transition> transitions, Semantics<Marking, Transition> semantics) {
        Map<Transition, Double> transitionProbabilities = new HashMap<>();
        // default: equal probabilities
        for (Transition transition : transitions) {
            transitionProbabilities.put(transition, 1. / transitions.size());
        }

        if (transitionsContainTimingInfo(transitions)) {
            // sanity check of the semantics, to make sure that only immediate transitions, or timed transitions are competing for the right to fire next!
            boolean allImmediate = getOnlyImmediateTransitions(transitions, true);
            boolean allTimed = getOnlyImmediateTransitions(transitions, false);

            Pair<Double, Double> cumulativeWeightAndRate = getCumulativeWeightAndRate(transitions);

            if (allImmediate) {
                for (Transition t : transitions) {
                    transitionProbabilities.put(t, StochasticNetUtils.getWeight(t) / cumulativeWeightAndRate.getFirst());
                }
            } else if (allTimed) {
                for (Transition t : transitions) {
                    transitionProbabilities.put(t, StochasticNetUtils.getFiringRate(t) / cumulativeWeightAndRate.getFirst());
                }
            } else { // mixed -> should not be the case, as semantics should take care of this case and not return both as executable!
                System.out.println("Debug me: why are mixed (immediate/timed) transitions here?");
            }
        }
        return transitionProbabilities;
    }

    private Pair<Double, Double> getCumulativeWeightAndRate(Collection<Transition> transitions) {
        double cumulativeWeight = 0;
        double cumulativeRate = 0;

        for (Transition t : transitions) {
            if (t instanceof TimedTransition) {
                cumulativeWeight += ((TimedTransition) t).getWeight();
                cumulativeRate += 1. / ((TimedTransition) t).getDistribution().getNumericalMean();
            }
        }
        return new Pair<>(cumulativeWeight, cumulativeRate);
    }

    protected boolean isOneBounded(Marking currentMarking) {
        return currentMarking.isLessOrEqual(oneMarking);
    }

    protected boolean isFinal(Marking currentMarking, Marking endPlaces) {
        return currentMarking.isLessOrEqual(endPlaces);
    }

    /**
     * Performs a simple simulation of the Petri net (mostly used for {@link StochasticNet}s, but can also simulate a PN without stochastic annotations)
     *
     * @param petriNet          {@link PetrinetGraph} the model
     * @param semantics         {@link Semantics} the semantics
     * @param config            {@link PNSimulatorConfig} the configuration {@link PNSimulatorConfig}
     * @param initialMarking    {@link Marking} the initial Marking
     * @param traceStart        long the date time to start the trace
     * @param constraint        long the date time that all simulated events should be greater than
     * @param i                 int trace id
     * @param useTimeConstraint boolean stores whether created events are constrained to be later than traceStart
     * @param finalMarking      Marking a final marking can be set to terminate the simulation, when it is reached... ignored, if null
     * @return
     */
    public Object simulateOneTrace(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
                                   PNSimulatorConfig config, Marking initialMarking, long traceStart, long constraint, long i, boolean useTimeConstraint, Marking finalMarking) {
        XTrace trace = createTrace(i, config);

        transitionRemainingTimes = new HashMap<Transition, Long>();
        lastFiringTime = traceStart;

        Collection<Transition> transitions = semantics.getExecutableTransitions();
        int eventsProduced = 0;

        this.logProbabilityOfCurrentTrace = 0;

        Marking currentMarking = semantics.getCurrentState();
        while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace && !currentMarking.equals(finalMarking)) {
//			System.out.println("events produced: "+eventsProduced);
            try {
                Triple<Transition, Long, Double> transitionAndDuration = pickTransition(semantics, transitions, transitionRemainingTimes, cachedDurations, petriNet, config, lastFiringTime, constraint, useTimeConstraint, useOnlyPastTrainingData);
                long firingTime = lastFiringTime + transitionRemainingTimes.get(transitionAndDuration.getFirst());

                // fire first transition the list:
                semantics.executeExecutableTransition(transitionAndDuration.getFirst());

                Collection<Transition> afterwardsEnabledTransitions = semantics.getExecutableTransitions();


                updateTransitionMemoriesAfterFiring(config, transitions, transitionAndDuration, firingTime - lastFiringTime, afterwardsEnabledTransitions, semantics);

                // Now, create an event according to the marking and duration of the transition:
                lastFiringTime = firingTime;

                if (useTimeConstraint && !transitionAndDuration.getFirst().isInvisible() && firingTime < constraint) {
                    if (transitionAndDuration.getFirst() instanceof TimedTransition &&
                            ((TimedTransition) transitionAndDuration.getFirst()).getDistributionType().equals(DistributionType.IMMEDIATE)) {
                        // ignore immediate transitions firing before passed time.
                    } else {
                        System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
                    }
                }
                insertEvent(String.valueOf(i), trace, petriNet, transitionAndDuration, firingTime, config);
                this.logProbabilityOfCurrentTrace += Math.log(transitionAndDuration.getThird());
                // before proceeding with the next transition, we update the enabled transitions:
                transitions = afterwardsEnabledTransitions;
                currentMarking = semantics.getCurrentState();
            } catch (IllegalTransitionException e) {
                e.printStackTrace();
                break;
            }
        }
        return getReturnObject(trace, lastFiringTime, config);
    }

    public double getLogProbabilityOfLastTrace() {
        return this.logProbabilityOfCurrentTrace;
    }

    protected Object getReturnObject(XTrace trace, long lastFiringTime, PNSimulatorConfig config) {
        if (config.simulateTraceless) {
            return lastFiringTime;
        } else {
            return trace;
        }
    }

    protected XTrace createTrace(long i, PNSimulatorConfig config) {
        if (config.simulateTraceless) {
            return null;
        } else {
            XAttributeMap traceAttributes = new XAttributeMapImpl();
            traceAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, String.valueOf(i)));
            XTrace trace = new XTraceImpl(traceAttributes);
            return trace;
        }
    }

    protected void insertEvent(String instanceId, XTrace trace, PetrinetGraph net, Triple<Transition, Long, Double> transitionAndDuration, long firingTime, PNSimulatorConfig config) {
        if (config.simulateTraceless) {
            // do nothing
        } else if (!transitionAndDuration.getFirst().isInvisible()) {
            XEvent e = createSimulatedEvent(transitionAndDuration.getFirst(), net, firingTime, instanceId);
            trace.insertOrdered(e);
        }
    }

    public void updateTransitionMemoriesAfterFiring(PNSimulatorConfig config, Collection<Transition> transitionsEnabledInMarking,
                                                    Triple<Transition, Long, Double> transitionAndDuration, long elapsedTimeInCurrentMarking, Collection<Transition> afterwardsEnabledTransitions, Semantics<Marking, Transition> semantics) {
        transitionRemainingTimes.remove(transitionAndDuration.getFirst());
        switch (config.executionPolicy) {
            case GLOBAL_PRESELECTION:
                // only one transition is allowed. (no transition count-downs should be used at all...)
                assert transitionRemainingTimes.isEmpty();
                break;
            case RACE_RESAMPLING:
                // reset all clocks after firing.
                transitionRemainingTimes.clear();
                break;
            case RACE_ENABLING_MEMORY:
                Collection<Transition> dormantTransitions = afterwardsEnabledTransitions;
                if (semantics instanceof StochasticNetSemanticsImpl) {
                    dormantTransitions = ((StochasticNetSemanticsImpl) semantics).getEnabledTransitions();
                }
                // reset timers of all disabled transitions (not in the set of enabled (possibly dormant) transitions
                Object[] transitions = transitionRemainingTimes.keySet().toArray();
                for (Object t : transitions) {
                    // entering a state where timed transitions are competing...
                    if (!dormantTransitions.contains(t)) {
                        // transition got disabled in current marking: needs to be resampled next time, it becomes enabled
                        transitionRemainingTimes.remove(t);
                    }
                    // if current marking is not vanishing, time has passed, and we reduce the clocks of the transitions still enabled
                    if (transitionRemainingTimes.containsKey(t)) {
                        transitionRemainingTimes.put((Transition) t, transitionRemainingTimes.get(t) - elapsedTimeInCurrentMarking);
                    }
                }
                break;
            case RACE_AGE_MEMORY:
                if (elapsedTimeInCurrentMarking > 0) {
                    // reduce clocks of all enabled transitions
                    for (Transition t : transitionsEnabledInMarking) {
                        if (!t.equals(transitionAndDuration.getFirst())) {
                            transitionRemainingTimes.put(t, transitionRemainingTimes.get(t) - elapsedTimeInCurrentMarking);
                        }
                    }
                }
                break;
        }
    }

//	/**
// 	 * Same as {@link #simulateOneTrace(PetrinetGraph, Semantics, PNSimulatorConfig, Marking, long, long, int, boolean)}, but without time-consuming XID generation for Events. 
//	 * @param petriNet
//	 * @param semantics
//	 * @param config
//	 * @param initialMarking
//	 * @param traceStart
//	 * @param constraint
//	 * @param i
//	 * @param useTimeConstraint
//	 * @return
//	 */
//	public Long simulateTraceEnd(PetrinetGraph petriNet, Semantics<Marking, Transition> semantics,
//			PNSimulatorConfig config, Marking initialMarking, long traceStart, long constraint, int i, boolean useTimeConstraint) {
//		Collection<Transition> transitions = semantics.getExecutableTransitions();
//		int eventsProduced = 0;
//		SortedSet<Long> transitionTimes = new TreeSet<Long>();
//		lastFiringTime = traceStart;
//
//		while (transitions.size() > 0 && eventsProduced++ < config.maxEventsInOneTrace) {
//			try {
//				Set<Transition> transitionsEnabledBefore = null;
//				if (useTimeConstraint){
//					transitionsEnabledBefore = new HashSet<Transition>(transitionRemainingTimes.keySet());
//				}
//				Pair<Transition, Long> transitionAndDuration = pickTransition(transitions, petriNet, config, lastFiringTime, constraint, useTimeConstraint);
//				long firingTime = lastFiringTime+transitionRemainingTimes.get(transitionAndDuration.getFirst());
//				
//				// fire first transition the list:
//				semantics.executeExecutableTransition(transitionAndDuration.getFirst());
//				
//				Collection<Transition> afterwardsEnabledTransitions = semantics.getExecutableTransitions();
//
//				updateTransitionMemoriesAfterFiring(config, transitions, transitionAndDuration, firingTime-lastFiringTime, afterwardsEnabledTransitions, semantics);
//				
//				// Now, create an event according to the marking and duration of the transition:
//				lastFiringTime = firingTime;
//								
//				if (useTimeConstraint && firingTime < constraint && !transitionsEnabledBefore.contains(transitionAndDuration.getFirst()) && !transitionAndDuration.getFirst().isInvisible()){
//					System.out.println("Debug me! This should not happen (if timed transitions were picked!!)");
//				}
//				if (!transitionAndDuration.getFirst().isInvisible()){
//					transitionTimes.add(firingTime);
//				}
//				transitions = afterwardsEnabledTransitions;
//			} catch (IllegalTransitionException e) {
//				e.printStackTrace();
//				break;
//			}
//		}
//		return transitionTimes.last();
//	}


    protected XEvent createSimulatedEvent(Transition transition, PetrinetGraph net, long firingTime, String instance) {
        String name = getLabel(transition);

        XAttributeMap eventAttributes = new XAttributeMapImpl();
        eventAttributes.put(LIFECYCLE_TRANSITION, new XAttributeLiteralImpl(LIFECYCLE_TRANSITION,
                TRANSITION_COMPLETE));
        eventAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, name));
        eventAttributes.put(CONCEPT_INSTANCE, new XAttributeLiteralImpl(CONCEPT_INSTANCE, instance));
        eventAttributes.put(CONCEPT_SIMULATED, new XAttributeBooleanImpl(CONCEPT_SIMULATED, true));
        eventAttributes.put(TIME_TIMESTAMP, new XAttributeTimestampImpl(TIME_TIMESTAMP, firingTime));
        XEvent e = XFactoryRegistry.instance().currentDefault().createEvent(eventAttributes);
        return e;
    }

    private String getLabel(Transition transition) {
        if (transition.getLabel() != null && !transition.getLabel().trim().isEmpty()){
            return transition.getLabel();
        }
        return "NA";
    }

//	/**
//	 * 
//	 * @param t
//	 * @return
//	 */
//	private Date getFiringTime(Transition t, long duration, Map<Place, List<Long>> placeTimes) {
//		// use max time of incoming places as base and add transition's duration:
//		long startTime = getTransitionStartTime(t, placeTimes, true);
//		long firingTime = startTime + duration;
//		Date firingDate = new Date(firingTime);
//
//		// add new timed tokens
//		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = t.getGraph().getOutEdges(t);
//		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdge : outEdges) {
//			Place p = (Place) outEdge.getTarget();
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(firingTime);
//		}
//		return firingDate;
//	}

//	/**
//	 * @param t Transition to fire
//	 * @param placeTimes times of the tokens on the places
//	 * @return
//	 */
//	public long getTransitionStartTime(Transition t, Map<Place, List<Long>> placeTimes, boolean removeFromPlaceTimes) {
//		Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = t.getGraph().getInEdges(t);
//		long maxTime = 0;
//		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : inEdges) {
//			Place p = (Place) inEdge.getSource();
//			long placeTime = 0;
//			if (placeTimes.containsKey(p)){
//				placeTime = placeTimes.get(p).get(0);
//			}
//			maxTime = Math.max(maxTime, placeTime);
//			if (removeFromPlaceTimes && placeTimes.containsKey(p)){
//				placeTimes.get(p).remove(0);
//			}
//		}
//		return maxTime;
//	}

    /**
     * Checks if the transition has already a running task (depending on the memory policy, this can be true) and returns that value, or
     * samples a new value freshly from the distribution.
     *
     * @param t                        the transition for which the remaining duration (in ms) will be determined
     * @param unitFactor               the scaling factor to get from the distribution parameters to milliseconds
     * @param transitionRemainingTimes
     * @param startOfTransition        long the entry time of the current marking (i.e., usually the last observed event's timestamp)
     * @param positiveConstraint       a constraint that might restrict sample values (left-truncates the distribution)
     * @param cachedDurations          {@link LimitedTreeMap} that caches distributions to avoid costly recomputation from the time series.
     * @param useOnlyPastTrainingData  a flag that tells us whether only data from the past is allowed for the computation of the remaining time
     * @return long milliseconds that the transition has to wait until it will fire.
     */
    public long getTransitionRemainingTime(Transition t, TimeUnit unitFactor, Map<Transition, Long> transitionRemainingTimes,
                                                  long startOfTransition, double positiveConstraint, LimitedTreeMap<Integer,
            Map<Transition, RealDistribution>> cachedDurations, boolean useOnlyPastTrainingData) {
        // only sample for transitions, that have no memory of their previous enabled periods (stored in the transition clocks)
        if (transitionRemainingTimes.containsKey(t)) {
            return transitionRemainingTimes.get(t);
        } else {
            long duration;
            if (t instanceof TimedTransition) {
                TimedTransition timedT = (TimedTransition) t;
                switch (timedT.getDistributionType()) {
                    case IMMEDIATE:
                        duration = 0;
                        break;
                    default:
                        double sample;
                        sample = sampleDurationForTransition(positiveConstraint, startOfTransition, timedT, unitFactor, cachedDurations, useOnlyPastTrainingData);
                        if (sample < positiveConstraint) {
                            System.out.println("debug me!");
                        }
                        duration = (long) (sample * unitFactor.getUnitFactorToMillis());
                }
            } else {
                // untimed net, just progress one unit in time.
                duration = (long) unitFactor.getUnitFactorToMillis();
            }
            transitionRemainingTimes.put(t, duration);
            return duration;
        }
    }

    /**
     * @param positiveConstraint a possible constraint for sampling a value from the distribution of the transition
     * @param startOfTransition  long the current time (or better: the last observed event's time)
     * @param timedT             {@link TimedTransition} that captures information about the duration distribution from which the sample should be taken.
     * @return
     */
    protected double sampleDurationForTransition(double positiveConstraint, long startOfTransition, TimedTransition timedT, TimeUnit unitFactor, LimitedTreeMap<Integer, Map<Transition, RealDistribution>> cachedDurations, boolean useOnlyPastTrainingData) {
        RealDistribution dist;
        if (useOnlyPastTrainingData && !(timedT.getDistribution() instanceof StatefulTimeseriesDistribution)) {
            SortedMultiset<ComparablePair<Long, List<Object>>> sortedTrainingData = timedT.getTrainingDataUpTo(startOfTransition);
            int sizeOfTrainingData = sortedTrainingData.size();
            if (!cachedDurations.containsKey(sizeOfTrainingData)) {
                cachedDurations.put(sizeOfTrainingData, new HashMap<Transition, RealDistribution>());
            }
            if (cachedDurations.get(sizeOfTrainingData).containsKey(timedT)) {
                dist = cachedDurations.get(sizeOfTrainingData).get(timedT);
            } else {
                DescriptiveStatistics stats = new DescriptiveStatistics();
                for (ComparablePair<Long, List<Object>> pair : sortedTrainingData) {
                    stats.addValue((Double) pair.getSecond().get(0));
                }
                switch (timedT.getDistributionType()) {
                    case EXPONENTIAL:
                        dist = new ExponentialDistribution(stats.getMean());
                        break;
                    case NORMAL:
                        dist = new NormalDistribution(stats.getMean(), stats.getStandardDeviation());
                        break;
                    case GAUSSIAN_KERNEL:
                        dist = new GaussianKernelDistribution();
                        ((GaussianKernelDistribution) dist).addValues(stats.getValues());
                        break;
                    case DETERMINISTIC:
                        dist = timedT.getDistribution();
                        break;
                    default:
                        throw new IllegalArgumentException("Distribution type " + timedT.getDistributionType() + " not yet supported for rolling cross validation!");
                }
                cachedDurations.get(sizeOfTrainingData).put(timedT, dist);
            }
        } else {
            dist = timedT.getDistribution();
        }

        if (dist instanceof StatefulTimeseriesDistribution) {
            ((StatefulTimeseriesDistribution) dist).setCurrentTime(startOfTransition);
        }
        StochasticNetUtils.useCache(false);
        return StochasticNetUtils.sampleWithConstraint(dist, "", positiveConstraint);
    }

//	private void updatePlaceTimes(Collection<Place> places, Date time, Map<Place, List<Long>> placeTimes) {
//		for (Place p : places) {
//			if (!placeTimes.containsKey(p)){
//				placeTimes.put(p, new ArrayList<Long>());
//			}
//			placeTimes.get(p).add(time.getTime());
//		}
//	}

    /**
     * @param semantics                   the Semantics of the net
     * @param transitions                 all enabled transitions to pick from.
     * @param transitionRemainingTimes    state of the simulation (transitions have associated remaining times/clocks)
     * @param cachedDurationDistributions (if transitions are only allowed to use training data up to a certain point in time, then this cache prevents us to recompute the estimated distribution over and over)
     * @param petriNet                    the underlying Petri net of the simulation.
     * @param config                      the configuration of the simulation. See {@link PNSimulatorConfig}. Contains the selection policy!
     * @param startOfTransition           the absolute time of the current marking's last state.
     * @param constraint                  all sampled durations should be greater than the constraint
     * @param usePositiveTimeContraint    the simulation might start in the middle of one trace, after some time has passed.
     *                                    In this case, we don't want to generate samples that are in the past.  (the parameter traceStart sets the constraint's value)
     * @param useOnlyPastTrainingData     flag that indicates whether only past training data is allowed to be used (rolling forecasts), or all training data is allowed to be used in distribution estimation (cross-validation)
     * @return The transition that is picked as the next one to fire with its duration in the current marking
     */
    public Triple<Transition, Long, Double> pickTransition(
            Semantics<Marking, Transition> semantics,
            Collection<Transition> transitions,
            Map<Transition, Long> transitionRemainingTimes,
            LimitedTreeMap<Integer, Map<Transition, RealDistribution>> cachedDurationDistributions,
            PetrinetGraph petriNet,
            PNSimulatorConfig config,
            long startOfTransition,
            long constraint,
            boolean usePositiveTimeContraint,
            boolean useOnlyPastTrainingData) {
        if (petriNet instanceof StochasticNet && transitionsContainTimingInfo(transitions)) {
            // sanity check of the semantics, to make sure that only immediate transitions, or timed transitions are competing for the right to fire next!
            boolean allImmediate = getOnlyImmediateTransitions(transitions, true);
            boolean allTimed = getOnlyImmediateTransitions(transitions, false);


            // either transitions are all immediate -> pick one randomly according to their relative weights...
            if (allImmediate) {
                Pair<Integer, Double> indexAndProbability = pickTransitionAccordingToWeights(transitions, new Date(constraint), semantics, useOnlyPastTrainingData);
                Transition t = getTransitionWithIndex(transitions, indexAndProbability.getFirst());
                transitionRemainingTimes.put(t, 0l);
                return new Triple<Transition, Long, Double>(t, 0l, indexAndProbability.getSecond());
            } // or they are all timed -> pick according to firing semantics...
            else if (allTimed) {
                double probability = 1;
                // select according to selection policy:
                if (config.executionPolicy.equals(ExecutionPolicy.GLOBAL_PRESELECTION)) {
                    Pair<Integer, Double> indexAndProbability = pickTransitionAccordingToWeights(transitions, new Date(constraint), semantics, useOnlyPastTrainingData);
                    // restrict the set of enabled transitions to the randomly picked one:
                    Transition t = getTransitionWithIndex(transitions, indexAndProbability.getFirst());
                    transitions = new LinkedList<Transition>();
                    transitions.add(t);
                    probability = indexAndProbability.getSecond();
                }

                // compute mean durations (indirectly proportional to firing rates) and assume exponential distributions for computing the probability (we don't want to do costly integration now)
                double cumulativeRates = 0;

                // select according to race policy:
                // they are all timed: (truly concurrent or racing for shared tokens)
                SortedMap<Long, Transition> times = new TreeMap<>();
                Map<Transition, Double> firingRates = new HashMap<>();
                for (Transition transition : transitions) {
                    if (usePositiveTimeContraint) {
                        // calculate minimum transition time that is necessary for transition to be satisfying the constraint (resulting in time bigger than traceStart)
                        double samplingConstraint = Math.max(0, (constraint - startOfTransition) / config.unitFactor.getUnitFactorToMillis());
                        long now = System.currentTimeMillis();
                        long transitionRemainingTime = getTransitionRemainingTime(transition, config.unitFactor, transitionRemainingTimes, startOfTransition, samplingConstraint, cachedDurationDistributions, useOnlyPastTrainingData);
                        if (transitionRemainingTime + startOfTransition < constraint) {
                            transitionRemainingTime = constraint - startOfTransition;
                            transitionRemainingTimes.put(transition, transitionRemainingTime);
                        }
                        long millis = System.currentTimeMillis() - now;
                        if (millis > 100) {
                            System.out.println("sampling took: " + millis + "ms. constraint " + samplingConstraint + ", transition: " + transition.getLabel() + " type: " + ((TimedTransition) transition).getDistributionType());
                        }
                        // make sure transition duration is bigger than constraint (sometimes floating point arithmetic might sample values that are overflowing, or just about the constraint.
                        if (!transitionRemainingTimes.containsKey(transition) && transitionRemainingTime + startOfTransition < constraint) {
                            transitionRemainingTimes.put(transition, (long) (samplingConstraint * config.unitFactor.getUnitFactorToMillis()) + 1);
                            System.out.println("distribution (" + transition.getLabel() + ") with constraint: " + samplingConstraint + ", mean: " + ((TimedTransition) transition).getDistribution().getNumericalMean() + " (Rounding produced Infinity)!!");
                        }
                        times.put(transitionRemainingTime, transition);
                    } else {
                        // only allow positive durations:
                        times.put(getTransitionRemainingTime(transition, config.unitFactor, transitionRemainingTimes, startOfTransition, 0, cachedDurationDistributions, useOnlyPastTrainingData), transition);
                    }
                    double rate = getFiringrate(transition);
                    firingRates.put(transition, rate);
                    cumulativeRates += rate;

                }
                Transition nextTransition = times.get(times.firstKey());
                probability *= firingRates.get(nextTransition) / cumulativeRates;
                return new Triple<Transition, Long, Double>(nextTransition, transitionRemainingTimes.get(nextTransition), probability);
            } else {
                // semantics should make sure, that only the transitions of the highest priority are enabled in the current marking!
                throw new IllegalArgumentException("Stochastic semantics bug! There should either be only immediate or only timed activities enabled!");
            }
        } else {
            // pick randomly:
            int randomPick = random.nextInt(transitions.size());
            Transition t = getTransitionWithIndex(transitions, randomPick);
            return new Triple<Transition, Long, Double>(t, getTransitionRemainingTime(t, config.unitFactor, transitionRemainingTimes, startOfTransition, 0, cachedDurationDistributions, useOnlyPastTrainingData), 1.0 / transitions.size());
        }
    }

    private static double getFiringrate(Transition transition) {
        double rate = 1.;
        if (transition instanceof TimedTransition) {
            TimedTransition tt = (TimedTransition) transition;
            rate = 1.0 / tt.getDistribution().getNumericalMean();
        }
        return rate;
    }

    public static boolean transitionsContainTimingInfo(Collection<Transition> transitions) {
        boolean allTimed = true;
        for (Transition t : transitions) {
            allTimed &= t instanceof TimedTransition;
        }
        return allTimed;
    }

    /**
     * Checks whether all transitions in set are immediate (if flag onlyImmediate is true)
     * or whether all transitions in set are timed (if flag onlyImmediate is false).
     * Example: To check, whether all transitions in the set <b>trans<b> are immediate, call: getOnlyImmediateTransitions(trans,true);
     *
     * @param transitions set of transitions to check, whether they belong to the same type (immediate, or not immediate)
     * @param immediate   flag
     * @return
     */
    public static boolean getOnlyImmediateTransitions(Collection<Transition> transitions, boolean immediate) {
        boolean allSame = true;
        for (Transition transition : transitions) {
            if (transition instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) transition;
                allSame = allSame && (immediate ? tt.getDistributionType().equals(DistributionType.IMMEDIATE) : !tt.getDistributionType().equals(DistributionType.IMMEDIATE));
            } else {
                return !immediate;
            }
        }
        return allSame;
    }

    /**
     * @param transitions             collections of enabled transitions
     * @param currentTime             the current simulation time
     * @param semantics               semantics of the net
     * @param useOnlyPastTrainingData flag that tells us wether we can use all training data, or only the ones in the past (e.g. for rolling forecasts)
     * @return
     */
    public static Pair<Integer, Double> pickTransitionAccordingToWeights(Collection<Transition> transitions, Date currentTime, Semantics<Marking, Transition> semantics, boolean useOnlyPastTrainingData) {
        double[] weights = new double[transitions.size()];
        double cumulativeWeights = 0;
        int i = 0;
        for (Transition transition : transitions) {
            TimedTransition tt = (TimedTransition) transition;
            double weight;
            if (useOnlyPastTrainingData) {
                weight = tt.getTrainingDataUpTo(currentTime.getTime()).size();
            } else {
                weight = tt.getWeight();
            }
            weights[i++] = weight;
            cumulativeWeights += weight;
        }
        int index = StochasticNetUtils.getRandomIndex(weights, random);
        double probability = weights[index] / cumulativeWeights;
        return new Pair<Integer, Double>(index, probability);
    }

    public static Transition getTransitionWithIndex(Collection<Transition> transitions, int index) {
        Transition t = null;
        Iterator<Transition> iterator = transitions.iterator();
        for (int i = 0; i < index + 1; i++) {
            t = iterator.next();
        }
        return t;
    }

    protected Date getNextArrivalDate(Date lastTime, TimeUnit unitFactor) {
        return new Date(lastTime.getTime() + (long) (arrivalDistribution.sample() * unitFactor.getUnitFactorToMillis()));
    }

    public void setUseOnlyPastTrainingData(boolean useOnlyPastTrainingData) {
        this.useOnlyPastTrainingData = useOnlyPastTrainingData;
    }
}
