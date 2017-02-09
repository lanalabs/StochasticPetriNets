package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "transition", strict = false)
public class PNMLTransition extends AbstractPNMLElement {
    @Attribute
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
