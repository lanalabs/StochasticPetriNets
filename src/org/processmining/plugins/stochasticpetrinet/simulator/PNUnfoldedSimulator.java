package org.processmining.plugins.stochasticpetrinet.simulator;

import com.google.common.base.Joiner;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.external.AllocationBasedNetGenerator;
import org.utils.datastructures.Triple;

import java.util.*;

/**
 * Idea: Use one unfolded model for multiple traces.
 * Same semantics, but different log (we need to treat each "lane" in the model as an instance).
 * The lanes are dependent by resource connections.
 * <p>
 * There is only one big run of the model emitting all the traces.
 *
 * @author Andreas Rogge-Solti
 */
public class PNUnfoldedSimulator extends PNSimulator {

    public static final String RESOURCE_SEPARATOR = ",";

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
            transitionRemainingTimes = new HashMap<Transition, Long>();
            random.setSeed(config.seed);

            XAttributeMap attributeMap = new XAttributeMapImpl();
            attributeMap.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, config.logName + " (simulated from "
                    + petriNet.getLabel() + ")"));
            log = XFactoryRegistry.instance().currentDefault().createLog(attributeMap);

            Date traceStart = new Date(0l);


            //Map<Place, List<Long>> placeTimes = new HashMap<Place, List<Long>>();
            //updatePlaceTimes(initialMarking, traceStart, placeTimes);
            semantics.initialize(petriNet.getTransitions(), initialMarking);

            Map<Integer, XTrace> caseTraces = new HashMap<Integer, XTrace>();

            // traverse initial marking and look for cases:
            for (Place p : initialMarking.baseSet()) {
                if (p.getLabel().startsWith(AllocationBasedNetGenerator.PLACE_CASE_PREFIX)) {
                    int caseId = Integer.valueOf(p.getLabel().substring(AllocationBasedNetGenerator.PLACE_CASE_PREFIX.length()));
                    XTrace caseTrace = createTrace(caseId, config);
                    caseTraces.put(caseId, caseTrace);
                }
            }


            transitionRemainingTimes = new HashMap<Transition, Long>();
            lastFiringTime = traceStart.getTime();

            Collection<Transition> transitions = semantics.getExecutableTransitions();

            this.logProbabilityOfCurrentTrace = 0;

            while (transitions.size() > 0) {
//					System.out.println("events produced: "+eventsProduced);
                try {
                    Triple<Transition, Long, Double> transitionAndDuration = pickTransition(semantics, transitions, transitionRemainingTimes, cachedDurations, petriNet, config, lastFiringTime, 0, false, useOnlyPastTrainingData);
                    long firingTime = lastFiringTime + transitionRemainingTimes.get(transitionAndDuration.getFirst());

                    Transition firingTransition = transitionAndDuration.getFirst();
                    XTrace traceToInsertEvent = null;
                    for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : petriNet.getInEdges(firingTransition)) {
                        if (edge.getSource().getLabel().startsWith(AllocationBasedNetGenerator.PLACE_CASE_PREFIX)) {
                            int caseId = Integer.valueOf(edge.getSource().getLabel().split("_")[2]);
                            traceToInsertEvent = caseTraces.get(caseId);
                        }
                    }

                    // fire first transition the list:
                    semantics.executeExecutableTransition(transitionAndDuration.getFirst());

                    Collection<Transition> afterwardsEnabledTransitions = semantics.getExecutableTransitions();


                    updateTransitionMemoriesAfterFiring(config, transitions, transitionAndDuration, firingTime - lastFiringTime, afterwardsEnabledTransitions, semantics);

                    // Now, create an event according to the marking and duration of the transition:
                    lastFiringTime = firingTime;

                    insertEvent("", traceToInsertEvent, petriNet, transitionAndDuration, firingTime, config);
                    // before proceeding with the next transition, we update the enabled transitions:
                    transitions = afterwardsEnabledTransitions;
                } catch (IllegalTransitionException e) {
                    e.printStackTrace();
                    break;
                }
            }
            for (XTrace trace : caseTraces.values()) {
                log.add(trace);
            }
        }
        return log;
    }

    protected XEvent createSimulatedEvent(Transition transition, PetrinetGraph petriNet, long firingTime, String instance) {
        // get resources:
        Set<String> resources = new HashSet<String>();
        Set<String> locations = new HashSet<String>();
        String caseId = "";
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : petriNet.getInEdges(transition)) {
            if (!edge.getSource().getLabel().startsWith(AllocationBasedNetGenerator.PLACE_CASE_PREFIX)) {
                // this is either a resource place, or a location!
                String placeLabel = edge.getSource().getLabel();
                if (placeLabel.contains("_room_")) {
                    locations.add(placeLabel.substring(2));
                } else {
                    resources.add(placeLabel.substring(2));
                }
            } else {
                caseId = edge.getSource().getLabel().substring(AllocationBasedNetGenerator.PLACE_CASE_PREFIX.length());
                if (caseId.indexOf("_") > -1) {
                    caseId = caseId.substring(0, caseId.indexOf("_"));
                }
            }
        }
        if (locations.isEmpty()) {
            // check for complete transitions:
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : petriNet.getOutEdges(transition)) {
                if (!edge.getTarget().getLabel().startsWith(AllocationBasedNetGenerator.PLACE_CASE_PREFIX)) {
                    // this is either a resource place, or a location!
                    String placeLabel = edge.getTarget().getLabel();
                    if (placeLabel.contains("_room_")) {
                        locations.add(placeLabel.substring(2));
                    } else {
                        resources.add(placeLabel.substring(2));
                    }
                }
            }
        }

        String name = transition.getLabel();
        XAttributeMap eventAttributes = new XAttributeMapImpl();
        if (name.contains("+")) {
            String activityName = name.split("\\+")[0];
            String lifecycleTransition = name.split("\\+")[1];
            switch (lifecycleTransition) {
                case "START":
                    eventAttributes.put(LIFECYCLE_TRANSITION, new XAttributeLiteralImpl(LIFECYCLE_TRANSITION,
                            "start"));
                    break;
                case "COMPLETE":
                default:
                    eventAttributes.put(LIFECYCLE_TRANSITION, new XAttributeLiteralImpl(LIFECYCLE_TRANSITION,
                            XLifecycleExtension.StandardModel.COMPLETE.toString()));
                    break;
            }
            name = activityName;
        } else {
            eventAttributes.put(LIFECYCLE_TRANSITION, new XAttributeLiteralImpl(LIFECYCLE_TRANSITION,
                    TRANSITION_COMPLETE));
        }
        eventAttributes.put(CONCEPT_NAME, new XAttributeLiteralImpl(CONCEPT_NAME, name));
        eventAttributes.put(CONCEPT_INSTANCE, new XAttributeLiteralImpl(CONCEPT_INSTANCE, caseId));
        eventAttributes.put(CONCEPT_SIMULATED, new XAttributeBooleanImpl(CONCEPT_SIMULATED, true));
        if (!locations.isEmpty()) {
            eventAttributes.put(LOCATION_ROOM, new XAttributeLiteralImpl(LOCATION_ROOM, getResourceString(locations)));
        }

        eventAttributes.put(TIME_TIMESTAMP, new XAttributeTimestampImpl(TIME_TIMESTAMP, firingTime));
        XEvent event = XFactoryRegistry.instance().currentDefault().createEvent(eventAttributes);

        if (!resources.isEmpty()) {
            XOrganizationalExtension.instance().assignResource(event, getResourceString(resources));
        }


        return event;
    }

    public static String getResourceString(Set<String> resources) {
        if (resources.size() == 1) {
            return resources.iterator().next();
        } else {
            return Joiner.on(RESOURCE_SEPARATOR).join(resources);
        }
    }

    public static String[] getResources(String resourceString) {
        return resourceString.split(RESOURCE_SEPARATOR);
    }

    /**
     * Performs a simple simulation of the Petri net (mostly used for {@link StochasticNet}s, but can also simulate a PN without stochastic annotations)
     * See {@link #simulateTraceEnd(PetrinetGraph, Semantics, PNSimulatorConfig, Marking, Date, int, Map, boolean)} for an implementation that does not
     * generate costly XIDs required for XES log files.
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
                                   PNSimulatorConfig config, Marking initialMarking, long traceStart, long constraint, int i, boolean useTimeConstraint, Marking finalMarking) {
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
                insertEvent("", trace, petriNet, transitionAndDuration, firingTime, config);
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
}
