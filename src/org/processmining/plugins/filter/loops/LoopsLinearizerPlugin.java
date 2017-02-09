package org.processmining.plugins.filter.loops;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Goes through all the traces in a log and numbers occurrences of repeated events,
 * such that repetitions get numbered and distinguished.
 * <hl/>
 * Example:<br />
 * trace &lt; a, b, b, c, b, d &gt; <br/>
 * becomes:<br />
 * trace &lt; a, b, b_2, c, b_3, d &gt; <br/>
 *
 * @author Andreas Rogge-Solti
 */
public class LoopsLinearizerPlugin {

    public static final String SEPARATOR = "_";

    @Plugin(name = "Unroll Loops Filter", parameterLabels = {"log"}, returnLabels = {"log"}, returnTypes = {XLog.class}, userAccessible = true,
            help = "Numbers repeated events of the same class in each trace.")
    @UITopiaVariant(affiliation = "WU Vienna", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at")
    public XLog unroll(UIPluginContext context, XLog log) {
        return unrollHeadless(context, log);

    }

    public XLog unrollHeadless(PluginContext context, XLog log) {
        XLog result = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) log.getAttributes().clone());

        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);

        for (XTrace trace : log) {
            XTrace traceCopy = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());

            Map<String, Integer> visitedClassesCount = new HashMap<String, Integer>();
            for (XEvent event : trace) {
                XEvent eventCopy = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) event.getAttributes().clone());

                String eventName = XConceptExtension.instance().extractName(event);
                int occurred = 0;
                if (visitedClassesCount.containsKey(eventName)) {
                    occurred = visitedClassesCount.get(eventName);
                }
                if (occurred == 0) {
                    // not occurred yet, insert for next time:
                    visitedClassesCount.put(eventName, 1);
                } else {
                    // already occurred, append occurrence counter to class label:
                    XConceptExtension.instance().assignName(eventCopy, eventName + SEPARATOR + (occurred + 1));
                    visitedClassesCount.put(eventName, occurred + 1);
                }
                traceCopy.add(eventCopy);
            }
            result.add(traceCopy);
        }
        return result;
    }

}
