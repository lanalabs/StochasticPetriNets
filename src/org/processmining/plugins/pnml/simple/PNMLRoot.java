package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "pnml")
@Namespace(reference = "http://www.pnml.org/version-2009/grammar/pnml")
public class PNMLRoot {

    @ElementList(inline = true, required = false)
    private List<PNMLNet> net;

    @ElementList(inline = true, required = false)
    private List<PNMLModule> module;

    @Element(name = "finalmarkings", required = false)
    private PNMLFinalMarkings finalMarkings;

    public List<PNMLNet> getNet() {
        return net;
    }

    public List<PNMLModule> getModule() {
        return module;
    }

    public void setModule(List<PNMLModule> module) {
        this.module = module;
    }

    public void setNet(List<PNMLNet> net) {
        this.net = net;
    }

    public PNMLFinalMarkings getFinalMarkings() {
        return finalMarkings;
    }

    public void setFinalMarkings(PNMLFinalMarkings finalMarkings) {
        this.finalMarkings = finalMarkings;
    }

}
