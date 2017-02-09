package org.processmining.plugins.temporal.miner;

import gnu.trove.TLongCollection;
import gnu.trove.list.linked.TLongLinkedList;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;

import java.util.BitSet;

public class TemporalTraces {

    private TMap<BitSet, TLongCollection> traces;

    public TemporalTraces() {
        this.traces = new THashMap<BitSet, TLongCollection>();
    }

    public void addOneEntry(BitSet set, long distance) {
        if (!traces.containsKey(set)) {
            traces.put(set, new TLongLinkedList());
        }
        traces.get(set).add(distance);
    }

    /**
     * @param sequentialProfile the profile for the current event (marks the events with which the current event is in sequence)
     * @return
     */
    public TLongCollection getDistance(BitSet sequentialProfile) {
        TLongCollection resultingTimes = new TLongLinkedList();
        for (BitSet bitSet : traces.keySet()) {
            if (!sequentialProfile.intersects(bitSet)) {
                // if they don't intersect, we assume that the intermediate events are in parallel and we return the times
                resultingTimes.addAll(traces.get(bitSet));
            }
        }
        return resultingTimes;
    }
}
