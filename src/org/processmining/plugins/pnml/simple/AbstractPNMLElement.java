package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root
public abstract class AbstractPNMLElement {

    @Element(name = "graphics", required = false)
    protected PNMLGraphics graphics;

    @ElementList(inline = true, required = false)
    protected List<PNMLToolSpecific> toolspecific;

    @Element(name = "name", required = false)
    protected PNMLName name;

    public PNMLGraphics getGraphics() {
        return graphics;
    }

    public void setGraphics(PNMLGraphics graphics) {
        this.graphics = graphics;
    }

    public List<PNMLToolSpecific> getToolspecific() {
        return toolspecific;
    }

    public void setToolspecific(List<PNMLToolSpecific> toolspecific) {
        this.toolspecific = toolspecific;
    }

    public PNMLName getName() {
        return name;
    }

    public void setName(PNMLName name) {
        this.name = name;
    }
}
