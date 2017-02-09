package org.processmining.plugins.overfit;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.HashMap;
import java.util.Map;

/**
 * Assumes the log to have dedicated start and end events embracing each trace.
 * <p>
 * Created by andreas on 9/15/16.
 */
public class OverfitChoiceMiner extends AbstractMiner {

    private final double threshold;
    private final int window;
    Map<int[], Place> statePlaces;
    Table<int[], int[], Integer> transitionTable;

    public OverfitChoiceMiner(double threshold) {
        this.statePlaces = new HashMap<>();
        this.threshold = threshold;
        this.window = 1;
    }

    public Object[] mine(UIPluginContext context, XLog log) {
        Petrinet net = PetrinetFactory.newPetrinet("overfitting choice net");
        Place startPlace = net.addPlace("start");
        Marking initialMarking = new Marking();
        initialMarking.add(startPlace);

        if (context != null && context.getProgress() != null) {
            context.getProgress().setMinimum(0);
            context.getProgress().setMaximum(log.size() * 2);
            context.getProgress().setValue(0);
        }
        transitionTable = HashBasedTable.create();

        int maxTransitionCount = 0;
        BoundaryEvents boundaryEvents = new BoundaryEvents();
        for (XTrace trace : log) {
            boundaryEvents.checkTrace(trace);
            for (int i = 0; i < trace.size(); i++) {
                if (context != null && context.getProgress() != null) {
                    context.getProgress().inc();
                }
                int[] state = new int[this.window];
                int[] previousState = null;
                for (int w = 0; w < this.window; w++) {
                    state[w] = getEventId(trace, i + w - this.window);
                    if (previousState != null) {
                        if (!transitionTable.contains(previousState, state)) {
                            transitionTable.put(previousState, state, 1);
                        } else {
                            int count = transitionTable.get(previousState, state) + 1;
                            maxTransitionCount = Math.max(count, maxTransitionCount);
                            transitionTable.put(previousState, state, count);
                        }
                    }
                }
            }
        }
        if (context != null && context.getProgress() != null) {
            int progress = transitionTable.rowKeySet().size();
            context.getProgress().setMaximum(progress * 2);
            context.getProgress().setValue(progress);
        }

        for (int[] rowKey : transitionTable.rowKeySet()) {

            Map<int[], Integer> targets = transitionTable.row(rowKey);
        }
        for (XTrace trace : log) {
            if (context != null && context.getProgress() != null) {
                context.getProgress().inc();
            }
        }
        return null;
    }

    private Integer getEventId(XTrace trace, int i) {
        if (i < 0) {
            return -1;
        }
        String eventName = XConceptExtension.instance().extractName(trace.get(i));
        return super.getEventId(eventName);
    }

    private class BoundaryEvents {
        private String startEvent;
        private String endEvent;

        public BoundaryEvents() {
            this.startEvent = null;
            this.endEvent = null;
        }

        public BoundaryEvents checkTrace(XTrace trace) {
            String thisStartEvent = XConceptExtension.instance().extractName(trace.get(0));
            String thisEndEvent = XConceptExtension.instance().extractName(trace.get(trace.size() - 1));
            if (startEvent == null) {
                startEvent = thisStartEvent;
                endEvent = thisEndEvent;
            }
            if (!startEvent.equals(thisStartEvent) || !endEvent.equals(thisEndEvent)) {
                throw new IllegalArgumentException("all traces are assumed to start/end with a dedicated start/end event!\n" +
                        "Please add artificial start/end events to the traces before using this miner.");
            }
            return this;
        }
    }
}
