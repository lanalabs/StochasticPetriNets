package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;

@Element(name = "name")
public class PNMLName {
    @Element(name = "text", required = false)
    private String value;

    @Element(name = "graphics", required = false)
    private PNMLGraphics graphics;

    public PNMLName() {
    }

    public PNMLName(String name) {
        this.value = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public PNMLGraphics getGraphics() {
        return graphics;
    }

    public void setGraphics(PNMLGraphics graphics) {
        this.graphics = graphics;
    }

}
