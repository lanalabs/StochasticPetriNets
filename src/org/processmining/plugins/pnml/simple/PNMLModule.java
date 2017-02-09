package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "module")
public class PNMLModule {

    @ElementList(inline = true)
    protected List<PNMLNet> nets;

    @ElementList(name = "finalmarkings", required = false)
    protected List<PNMLMarking> finalmarkings;


    public List<PNMLMarking> getFinalmarkings() {
        return finalmarkings;
    }

    public void setFinalmarkings(List<PNMLMarking> finalmarkings) {
        this.finalmarkings = finalmarkings;
    }

    public List<PNMLNet> getNets() {
        return nets;
    }

    public void setNets(List<PNMLNet> nets) {
        this.nets = nets;
    }

}
