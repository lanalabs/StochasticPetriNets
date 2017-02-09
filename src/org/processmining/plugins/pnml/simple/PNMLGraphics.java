package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

@Element(name = "graphics")
public class PNMLGraphics {

    @ElementList(name = "position", inline = true, required = false)
    private List<PNMLPoint> position;

    @Element(name = "dimension", required = false)
    private PNMLPoint dimension;

    @Element(name = "fill", required = false)
    private PNMLFill fill;

    @Element(name = "offset", required = false)
    private PNMLPoint offset;

    public List<PNMLPoint> getPosition() {
        return position;
    }

    public void setPosition(List<PNMLPoint> position) {
        this.position = position;
    }

    public PNMLPoint getDimension() {
        return dimension;
    }

    public void setDimension(PNMLPoint dimension) {
        this.dimension = dimension;
    }

    public PNMLFill getFill() {
        return fill;
    }

    public void setFill(PNMLFill fill) {
        this.fill = fill;
    }

    public PNMLPoint getOffset() {
        return offset;
    }

    public void setOffset(PNMLPoint offset) {
        this.offset = offset;
    }
}
