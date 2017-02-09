package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Element(name = "arc")
public class PNMLArc extends AbstractPNMLElement {
    @Attribute
    private String id;
    @Attribute
    private String source;
    @Attribute
    private String target;

    @Element(name = "arctype", required = false)
    private PNMLText arcType;

    @Element(name = "inscription", required = false)
    private PNMLText inscription;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public PNMLText getInscription() {
        return inscription;
    }

    public void setInscription(PNMLText inscription) {
        this.inscription = inscription;
    }

    public PNMLText getArcType() {
        return arcType;
    }

    public void setArcType(PNMLText arcType) {
        this.arcType = arcType;
    }
}
