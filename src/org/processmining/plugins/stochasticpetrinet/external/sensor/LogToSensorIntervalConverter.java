package org.processmining.plugins.stochasticpetrinet.external.sensor;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.external.Person;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;

import java.util.*;

/**
 * Given a rich log with resources and locations for events (and start-and complete transitions),
 * we can generate intervals of where the resources are located.
 * <p>
 * assumption: a resource can idle between two activities:
 * the resource can stay in the previous activity's location,
 * or move to the next activity's location,
 * or with low probability, go to a random location.
 * <p>
 * another assumption: resources have all distinct names!
 *
 * @author Andreas Rogge-Solti
 */
public class LogToSensorIntervalConverter {

    private static SortedSensorIntervals allIntervals;
    private static Map<String, SortedSet<SensorInterval>> intervalsByResource;

    public static SortedSensorIntervals convertLog(XLog log, TimeUnit timeUnit, boolean fillGapsRandomly, long seed) {
        allIntervals = new SortedSensorIntervals();
        intervalsByResource = new HashMap<String, SortedSet<SensorInterval>>();

        StochasticNetUtils.setRandomSeed(seed);

        Set<String> locations = new HashSet<String>();

        for (XTrace trace : log) {
            Iterator<XEvent> eventIter = trace.iterator();

            while (eventIter.hasNext()) {
                XEvent event = eventIter.next();
                if (event.getAttributes().containsKey(PNSimulator.LOCATION_ROOM)) {
                    String location = extractLocation(event);
                    locations.add(location);
                    XEvent endEvent = eventIter.next(); // FIXME: this breaks, when we introduce concurrency into the model!!!

                    String resources = XOrganizationalExtension.instance().extractResource(event);
                    if (resources.contains(",")) {
                        String[] resourcesArray = resources.split(",");
                        for (String resource : resourcesArray) {
                            addResourceInterval(resource, event, endEvent, timeUnit);
                        }
                    } else {
                        addResourceInterval(resources, event, endEvent, timeUnit);
                    }
                    String caseResource = XConceptExtension.instance().extractInstance(event);
                    addResourceInterval(Person.CASE_PREFIX + caseResource, event, endEvent, timeUnit);
                }
            }
        }

        if (fillGapsRandomly) {
            // go through each resource's intervals - look in between and add some uniformly distributed fillers
            // maybe also a chance to
            for (String resource : intervalsByResource.keySet()) {
                SensorInterval lastInterval = null;
                for (SensorInterval interval : intervalsByResource.get(resource)) {
                    if (lastInterval != null) {
                        // tug at ends a bit randomly to a randomly selected center
                        int timeBetweenIntervals = (int) (interval.getStartTime() - lastInterval.getEndTime());
                        if (timeBetweenIntervals > 0) {
                            int middle = StochasticNetUtils.getRandomInt(timeBetweenIntervals);
                            if (middle > 0) {
                                int tugUp = StochasticNetUtils.getRandomInt(middle);
                                lastInterval.setEndTime(lastInterval.getEndTime() + tugUp);
                            }
                            if (middle < timeBetweenIntervals) {
                                int tugDown = StochasticNetUtils.getRandomInt(timeBetweenIntervals - middle);
                                interval.setStartTime(interval.getStartTime() - tugDown);
                            }
                        }

                    }
                    lastInterval = interval;
                }
            }
        }

        return allIntervals;
    }

    private static void addResourceInterval(String resource, XEvent event, XEvent endEvent, TimeUnit timeUnit) {
        String location = extractLocation(event);
        int startTime = (int) Math.floor(XTimeExtension.instance().extractTimestamp(event).getTime() / timeUnit.getUnitFactorToMillis());
        int endTime = (int) Math.floor(XTimeExtension.instance().extractTimestamp(endEvent).getTime() / timeUnit.getUnitFactorToMillis());
        if (endTime > startTime) { // prune empty intervals!
            SensorInterval interval = new SensorInterval(startTime, endTime, location, resource);
            allIntervals.add(interval);
            if (!intervalsByResource.containsKey(resource)) {
                intervalsByResource.put(resource, new TreeSet<SensorInterval>());
            }
            intervalsByResource.get(resource).add(interval);
        }
    }

    private static String extractLocation(XEvent event) {
        if (event.getAttributes().containsKey(PNSimulator.LOCATION_ROOM)) {
            return event.getAttributes().get(PNSimulator.LOCATION_ROOM).toString();
        }
        return null;
    }


}
