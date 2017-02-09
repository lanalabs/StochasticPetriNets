package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Element(name = "fill")
public class PNMLFill {

    @Attribute(name = "color", required = false)
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
