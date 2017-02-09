package org.processmining.plugins.temporal.miner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import gnu.trove.TLongCollection;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.linked.TLongLinkedList;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;


public class TemporalProfile {

    private BiMap<String, Integer> eventToId;

    private Table<Integer, Integer, TemporalTraces> temporalTraceTable;

    public TemporalProfile(XLog log) {
        this.eventToId = HashBiMap.create();
        this.temporalTraceTable = HashBasedTable.create();
        init(log);
    }

    private void init(XLog log) {
        // collect all events first and assign an id in the eventToId map.
        for (XTrace trace : log) {
            for (XEvent e : trace) {
                getId(e);
            }
        }
        // try to identify triggering events, that is, for each combination of events store all temporal distances
        for (XTrace trace : log) {
            int currentEventPos = 0;
            for (XEvent event : trace) {
                long currentEventTime = XTimeExtension.instance().extractTimestamp(event).getTime();
                int eventId = getId(event);
                // go through all potential enabling events (basically all other events and store the distances in the table)

                Iterator<XEvent> traceIter = trace.iterator();
                int otherEventPos = 0;

                // traverse backward from current event
                int pointer = currentEventPos;
                BitSet eventsSeenBetween = new BitSet();
                while (pointer > 0) {
                    XEvent previousEvent = trace.get(pointer - 1);
                    int otherEventId = getId(previousEvent);
                    if (otherEventId >= eventId) {
                        long otherEventTime = XTimeExtension.instance().extractTimestamp(previousEvent).getTime();
                        addTimeInformation(eventId, otherEventId, currentEventTime - otherEventTime, eventsSeenBetween);
                    }
                    pointer--;
                    eventsSeenBetween.set(otherEventId);
                }

                // traverse forward from current event
                eventsSeenBetween = new BitSet();
                pointer = currentEventPos;
                while (pointer < trace.size() - 1) {
                    XEvent nextEvent = trace.get(pointer + 1);
                    int otherEventId = getId(nextEvent);
                    if (otherEventId >= eventId) {
                        long otherEventTime = XTimeExtension.instance().extractTimestamp(nextEvent).getTime();
                        addTimeInformation(eventId, otherEventId, currentEventTime - otherEventTime, eventsSeenBetween);
                    }
                    pointer++;
                    eventsSeenBetween.set(otherEventId);
                }
                currentEventPos++;
            }
        }
    }

    private void addTimeInformation(int eventId, int otherEventId, long tempDistance, BitSet inBetweeners) {
        if (!temporalTraceTable.contains(eventId, otherEventId)) {
            temporalTraceTable.put(eventId, otherEventId, new TemporalTraces());
        }
        temporalTraceTable.get(eventId, otherEventId).addOneEntry((BitSet) inBetweeners.clone(), tempDistance);
    }

    private int getId(XEvent event) {
        String label = XConceptExtension.instance().extractName(event);
        return getId(label);
    }

    private int getId(String label) {
        if (!eventToId.containsKey(label)) {
            eventToId.put(label, eventToId.size());
        }
        return eventToId.get(label);
    }

    /**
     * @param eventId
     * @param sequenceSet
     * @return
     */
    public TLongCollection getDurationsForEvent(int eventId, BitSet sequenceSet) {
        TLongCollection values = new TLongLinkedList();
        // collect all values from profile:
        for (int i = 0; i < eventId; i++) {
            values.addAll(collectValues(temporalTraceTable.get(i, eventId), sequenceSet, true));
        }
        for (int j = eventId + 1; j < eventToId.size(); j++) {
            values.addAll(collectValues(temporalTraceTable.get(eventId, j), sequenceSet, false));
        }
        return values;
    }

    private TLongCollection collectValues(TemporalTraces temporalTraces, BitSet sequenceSet, boolean invertResults) {
        TLongCollection values = temporalTraces.getDistance(sequenceSet);
        if (invertResults) {
            TLongCollection invertedValues = new TLongLinkedList();
            TLongIterator iter = values.iterator();
            while (iter.hasNext()) {
                invertedValues.add(-iter.next());
            }
            values = invertedValues;
        }
        return values;
    }

    public int getEventCount() {
        return eventToId.size();
    }

    public static BitSet getBitSet(List<TemporalRelation> relations, TemporalRelation ofInterest) {
        BitSet set = new BitSet();
        for (int i = 0; i < relations.size(); i++) {
            TemporalRelation relation = relations.get(i);
            if (relation.equals(ofInterest)) {
                set.set(i);
            }
        }
        return set;
    }
}
