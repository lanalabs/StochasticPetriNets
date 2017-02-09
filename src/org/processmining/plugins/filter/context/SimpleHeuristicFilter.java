package org.processmining.plugins.filter.context;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.HashMap;
import java.util.Map;

public class SimpleHeuristicFilter {

    public static XLog filterLogForMostFrequentTrace(XLog input) {
        Map<String, Long> traceVariantCounts = new HashMap<>();
        long maxTraceCount = 0;
        XTrace mostFrequentTrace = null;
        for (XTrace trace : input) {
            String traceString = StochasticNetUtils.debugTrace(trace, true);
            if (!traceVariantCounts.containsKey(traceString)) {
                traceVariantCounts.put(traceString, 0l);
            }
            Long traceCount = traceVariantCounts.get(traceString) + 1;
            traceVariantCounts.put(traceString, traceCount);

            if (maxTraceCount < traceCount) {
                maxTraceCount = traceCount;
                mostFrequentTrace = trace;
            }
        }
        XLog log = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) input.getAttributes().clone());
        XTrace newTrace = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) mostFrequentTrace.getAttributes().clone());
        for (XEvent e : mostFrequentTrace) {
            XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
            newTrace.add(newEvent);
        }
        log.add(newTrace);
        return log;

    }
}
