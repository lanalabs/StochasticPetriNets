package org.processmining.plugins.overfit;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XEvent;

import java.util.Map;

/**
 * Created by andreas on 9/15/16.
 */
public abstract class AbstractMiner {

    protected Map<String, Integer> eventNames;
    protected int counter;

    public AbstractMiner() {
        this.counter = 1;
    }

    protected Integer getEventId(XEvent e) {
        String name = XConceptExtension.instance().extractName(e);
        return getEventId(name);
    }

    protected Integer getEventId(String name) {
        if (!eventNames.containsKey(name)) {
            eventNames.put(name, counter++);
        }
        return eventNames.get(name);
    }
}
