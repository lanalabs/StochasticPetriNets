package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "finalmarkings")
public class PNMLFinalMarkings {
    @ElementList(inline = true)
    private List<PNMLMarking> markings;

    public List<PNMLMarking> getMarkings() {
        return markings;
    }

    public void setMarkings(List<PNMLMarking> markings) {
        this.markings = markings;
    }
}
