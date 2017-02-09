package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "place", strict = false)
public class PNMLPlace extends AbstractPNMLElement {
    @Attribute
    private String id;

    @Element(name = "initialMarking", required = false)
    private PNMLText initialMarking = null;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PNMLText getInitialMarking() {
        return initialMarking;
    }

    public void setInitialMarking(PNMLText initialMarking) {
        this.initialMarking = initialMarking;
    }

}
