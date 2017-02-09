package org.processmining.plugins.alignment;

import com.timeseries.Bit;
import com.timeseries.TimeSeriesBit;
import com.timeseries.TimeSeriesPoint;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.plugins.alignment.model.CaseTimeSeries;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

public class TimeSeriesConverter {

    public static final int DEFAULT_RESOLUTION = 200;

    public static CaseTimeSeries convert(UIPluginContext context, XLog log) {
        return convert(context, log, DEFAULT_RESOLUTION);
    }

    private static CaseTimeSeries convert(UIPluginContext context, XLog log, int resolution) {
        CaseTimeSeries caseTimeSeries = new CaseTimeSeries();

        // collect events -> number of dimensions
        XLogInfo info = XLogInfoFactory.createLogInfo(log);

        XEventClasses eventClasses = info.getEventClasses();
        Collection<XEventClassifier> classifiers = info.getEventClassifiers();

        XLogInfo activityInfo = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier());

        List<String> activities = new ArrayList<>();
        for (XEventClass eClass : activityInfo.getEventClasses().getClasses()) {
            activities.add(eClass.getId());
        }

        BitSet currentState = new BitSet();


        for (XTrace trace : log) {
            TimeSeriesBit timeSeries = new TimeSeriesBit(activities.size());

            XEvent startEventInTrace = trace.get(0);
            long startEventTime = XTimeExtension.instance().extractTimestamp(startEventInTrace).getTime();
            long endEventTime = XTimeExtension.instance().extractTimestamp(trace.get(trace.size() - 1)).getTime();

            double traceDuration = endEventTime - startEventTime;
            int currentRelativeTime = 0;

            for (int i = 0; i < trace.size(); i++) {
                XEvent event = trace.get(i);
                long eventTime = XTimeExtension.instance().extractTimestamp(event).getTime();

                // normalize case durations between 0 and DEFAULT_RESOLUTION
                int relativeTimeOfEvent = (int) Math.round(resolution * ((eventTime - startEventTime) / traceDuration));
                String lifeCycleTransition = XLifecycleExtension.instance().extractTransition(event);
                String eventName = XConceptExtension.instance().extractName(event);
                if (relativeTimeOfEvent == currentRelativeTime - 1) {
                    // event belongs to last iteration
                    int bitIndex = activities.indexOf(eventName);
                    currentState = updateState(eventClasses.getClassOf(event), currentState, lifeCycleTransition, eventName, activities, true);
                    timeSeries.setMeasurement(relativeTimeOfEvent, bitIndex, new Bit(currentState.get(bitIndex)));
                    currentState = updateState(eventClasses.getClassOf(event), currentState, lifeCycleTransition, eventName, activities, false);
                }
                // TODO: find all active events in that time period!
                for (; currentRelativeTime < relativeTimeOfEvent; ) {
                    // add one time series point with the current state
                    timeSeries.addLast(currentRelativeTime++, getTimeSeriesPoint(currentState, activities.size()));
                }
                currentState = updateState(eventClasses.getClassOf(event), currentState, lifeCycleTransition, eventName, activities, true);
                if (currentRelativeTime <= relativeTimeOfEvent) {
                    // add one time series point with the current state
                    timeSeries.addLast(currentRelativeTime++, getTimeSeriesPoint(currentState, activities.size()));
                }
                currentState = updateState(eventClasses.getClassOf(event), currentState, lifeCycleTransition, eventName, activities, false);
            }
            caseTimeSeries.add(timeSeries);
        }


        return caseTimeSeries;
    }

    private static TimeSeriesPoint<Bit> getTimeSeriesPoint(BitSet currentState, int dimensions) {
        Bit[] values = new Bit[dimensions];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Bit(currentState.get(i));
        }
        return new TimeSeriesPoint<>(values);
    }

    private static BitSet updateState(XEventClass classOf, BitSet currentState, String lifeCycleTransition,
                                      String eventName, List<String> activities, boolean addComplete) {
        int idOfEventActivity = activities.indexOf(eventName);
        boolean enable = "start".equals(lifeCycleTransition.toLowerCase()) || addComplete;
        BitSet newState = (BitSet) currentState.clone();
        newState.set(idOfEventActivity, enable);
        return newState;
    }

}
