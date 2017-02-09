package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class PNMLText {

    @Element(name = "text", required = false)
    private String text;

    public PNMLText() {
    }

    public PNMLText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
