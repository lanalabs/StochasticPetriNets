package org.processmining.plugins.filter.context;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.util.*;

/**
 * Created by andreas on 2/15/17.
 */
public class PreprocessLogFilter {
    public static XLog renameUnderscores(XLog input) {
        String fromPattern = "_";
        String toPattern = "#";

        XLog log = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) input.getAttributes().clone());

        for (XTrace trace : input) {
            XTrace newTrace = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
            for (XEvent e : trace) {
                XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
                String newName = XConceptExtension.instance().extractName(e).replaceAll(fromPattern,toPattern);
                XConceptExtension.instance().assignName(newEvent, newName);
                newTrace.add(newEvent);
            }
            log.add(newTrace);
        }
        return log;
    }

    public static XLog reDuplicateEvents(XLog input){
        String splitPattern = "_";

        XLog log = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) input.getAttributes().clone());
        XLogInfo info = XLogInfoFactory.createLogInfo(input, new XEventNameClassifier());
        XEventClasses classes = info.getEventClasses();
        Set<String> eventNames = new HashSet<>();
        for (XEventClass eventClass : classes.getClasses()){
            eventNames.add(eventClass.getId());
        }
        Map<String, String> renameMap = new HashMap<>();
        for (XEventClass eventClass : classes.getClasses()){
            String[] parts = eventClass.getId().split(splitPattern);
            try {
                Integer num = Integer.valueOf(parts[parts.length - 1]);
                String neighborClass = eventClass.getId().substring(0, eventClass.getId().lastIndexOf(splitPattern));
                if (isContainingAllIterations(eventNames, num, neighborClass+splitPattern)){
                    renameMap.put(eventClass.getId(), neighborClass);
                }
            } catch (NumberFormatException e){
            }
        }

        for (XTrace trace : input) {
            XTrace newTrace = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
            for (XEvent e : trace) {
                XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
                String oldName = XConceptExtension.instance().extractName(e);
                String newName = renameMap.containsKey(oldName) ? renameMap.get(oldName) : oldName;
                XConceptExtension.instance().assignName(newEvent, newName);
                newTrace.add(newEvent);
            }
            log.add(newTrace);
        }
        return log;
    }

    private static boolean isContainingAllIterations(Set<String> eventNames, Integer num, String neighborClass) {
        if (num == -1){
            return false;
        } else if (num == 0 || num == 1){
            return eventNames.contains(neighborClass+(num+1)) || eventNames.contains(neighborClass+(num - 1));
        } else {
            return eventNames.contains(neighborClass+(num-1)) && isContainingAllIterations(eventNames, num-1, neighborClass);
        }
    }

    public static XLog reorderByTime(XLog input) {
        XLog log = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) input.getAttributes().clone());

        for (XTrace trace : input) {
            XTrace newTrace = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
            for (XEvent e : trace) {
                XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
                newTrace.add(newEvent);
            }
            Collections.sort(newTrace, new Comparator<XEvent>() {
                @Override
                public int compare(XEvent o1, XEvent o2) {
                    return XTimeExtension.instance().extractTimestamp(o1).compareTo(XTimeExtension.instance().extractTimestamp(o2));
                }
            });
            log.add(newTrace);
        }
        return log;
    }
}
