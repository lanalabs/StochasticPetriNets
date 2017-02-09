package org.processmining.plugins.filter.context;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

import java.util.*;

/**
 * This is very basic. It adds the number of currently started, but not finished cases (i.e., the system load) to each event
 * as a meta information.
 * More sophisticated context annotation functionality can be found in the PredictionWithContext plug-in by Can Eren.
 * <p>
 * only works, when time stamps are available
 *
 * @author Andreas Rogge-Solti
 */
public class LoadAnnotationPlugin {

    public static final String CONTEXT_LOAD = "context:load";

    /**
     * Count the number of active cases in the process and add the count to each instance
     */
    @Plugin(name = "Load Annotation Filter", parameterLabels = {"log"}, returnLabels = {"log"}, returnTypes = {XLog.class}, userAccessible = true,
            help = "Annotates each event with the context:load field (to capture the concurrently running traces).")
    @UITopiaVariant(affiliation = "WU Vienna", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at")
    public XLog addNumberOfInstancesInSystemToLog(UIPluginContext context, final XLog log) {
        return addNumberOfInstancesInSystemToLogHeadless(context, log);
    }

    public XLog addNumberOfInstancesInSystemToLogHeadless(PluginContext context, XLog log) {

        if (log.isEmpty() || log.get(0).isEmpty()) {
            debug(context, "Log is Empty, nothing to do!");
            return log;
        }
        Date timeStamp = XTimeExtension.instance().extractTimestamp(log.get(0).get(0));
        if (timeStamp == null) {
            debug(context, "Log does not contain any temporal data to exploit for counting concurrently active traces.");
            return log;
        }

        Map<Long, List<XEvent>> eventsAtTimePoints = new TreeMap<>();
        Map<XEvent, XTrace> eventToTraceMapping = new HashMap<>();

        if (context != null) {
            context.getProgress().setMaximum(0);
            context.getProgress().setMaximum(log.size() * 3);
        }

        // sort events according to their time
        for (XTrace trace : log) {
            if (context != null) context.getProgress().inc();
            for (XEvent event : trace) {
                eventToTraceMapping.put(event, trace);
                Date eventDate = XTimeExtension.instance().extractTimestamp(event);
                if (eventDate != null) {
                    long time = eventDate.getTime();
                    if (!eventsAtTimePoints.containsKey(time)) {
                        eventsAtTimePoints.put(Long.valueOf(time), new ArrayList<XEvent>());
                    }
                    eventsAtTimePoints.get(Long.valueOf(time)).add(event);
                }
            }
        }
        Map<XEvent, Integer> loadInEvent = new HashMap<>();
        // go through events and cache load annotations:
        Set<XTrace> activeTraces = new HashSet<>();
        for (List<XEvent> events : eventsAtTimePoints.values()) {
            if (context != null) context.getProgress().inc();
            for (XEvent event : events) {
                XTrace eventTrace = eventToTraceMapping.get(event);
                activeTraces.add(eventTrace);
                loadInEvent.put(event, activeTraces.size());
                if (eventTrace.get(eventTrace.size() - 1).equals(event)) {
                    // last event in trace! Remove trace from the set of active traces
                    activeTraces.remove(eventTrace);
                }
            }
        }

        // construct new log with additional information:
        XLog enrichedLog = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) log.getAttributes().clone());
        for (XTrace trace : log) {
            if (context != null) context.getProgress().inc();
            XTrace traceCopy = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
            for (XEvent event : trace) {
                XEvent eventCopy = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) event.getAttributes().clone());
                eventCopy.getAttributes().put(CONTEXT_LOAD, new XAttributeDiscreteImpl(CONTEXT_LOAD, loadInEvent.get(event)));
                traceCopy.add(eventCopy);
            }
            enrichedLog.add(traceCopy);
        }
        return enrichedLog;
    }


    private void debug(PluginContext context, String string) {
        if (context != null) {
            context.log(string);
        } else {
            System.out.println(string);
        }
    }

}
