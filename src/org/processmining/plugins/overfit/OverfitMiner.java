package org.processmining.plugins.overfit;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.semantics.petrinet.Marking;

import java.util.HashMap;
import java.util.Map;

public class OverfitMiner extends AbstractMiner {

    private static final String SEPARATOR = "|";

    private int placeCounter;

    private Map<String, Place> prefixPlaces;
    private Map<String, Place> suffixPlaces;

    public OverfitMiner() {

        this.placeCounter = 1;
    }


    public Object[] mine(UIPluginContext context, XLog log) {
        prefixPlaces = new HashMap<String, Place>();
        suffixPlaces = new HashMap<String, Place>();

        Petrinet net = PetrinetFactory.newPetrinet("overfitting net");
        Place startPlace = net.addPlace("start");
        Marking initialMarking = new Marking();
        initialMarking.add(startPlace);

        if (context != null && context.getProgress() != null) {
            context.getProgress().setMinimum(0);
            context.getProgress().setMaximum(log.size());
            context.getProgress().setValue(0);
        }

        for (XTrace trace : log) {
            if (context != null && context.getProgress() != null) {
                context.getProgress().inc();
            }

            String completeTraceString = getTraceString(trace);
            // walk through trace String...
            int currentIndex = 0;
            Place lastPlace = startPlace;

            for (int i = 0; i < trace.size(); i++) {
                String eventName = XConceptExtension.instance().extractName(trace.get(i));
                currentIndex = completeTraceString.indexOf(SEPARATOR, currentIndex + 1);

                String prefix = completeTraceString.substring(0, currentIndex);
                String suffix = completeTraceString.substring(currentIndex, completeTraceString.length());

                if (suffixPlaces.containsKey(suffix) && prefixPlaces.containsKey(prefix)) {
                    if (suffixPlaces.get(suffix).equals(prefixPlaces.get(prefix))) {
                        break;
                        // this trace was visited already!
                    }
                }

                if (suffixPlaces.containsKey(suffix)) {
                    // we need to merge the current path with the suffix place
                    if (lastPlace.equals(suffixPlaces.get(suffix))) {
                        // done!
                        break;
                    } else {
                        connect(net, lastPlace, eventName, suffixPlaces.get(suffix));
                        prefixPlaces.put(prefix, suffixPlaces.get(suffix));
                    }
                } else if (prefixPlaces.containsKey(prefix)) {
                    lastPlace = prefixPlaces.get(prefix);
                } else {
                    // new path:
                    Place newPlace = net.addPlace("p" + placeCounter++);
                    connect(net, lastPlace, eventName, newPlace);
                    prefixPlaces.put(prefix, newPlace);
                    suffixPlaces.put(suffix, newPlace);
                    lastPlace = newPlace;
                }
            }
        }
        return new Object[]{net, initialMarking};
    }


    private void connect(Petrinet net, Place lastPlace, String eventName, Place place) {
        Transition t = net.addTransition(eventName);
        net.addArc(lastPlace, t);
        net.addArc(t, place);
    }


    private String getTraceString(XTrace trace) {
        StringBuilder traceBuilder = new StringBuilder();
        for (XEvent e : trace) {
            String eventString = String.valueOf(getEventId(e));
            traceBuilder.append(eventString);
            traceBuilder.append(SEPARATOR);
        }
        return traceBuilder.toString();
    }
}
