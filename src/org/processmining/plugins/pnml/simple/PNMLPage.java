package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.ElementListUnion;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "page")
public class PNMLPage extends AbstractPNMLElement {
    @Attribute(name = "id", required = false)
    private String id;

    @ElementListUnion({
            @ElementList(entry = "transition", type = PNMLTransition.class, inline = true, required = false),
            @ElementList(entry = "place", type = PNMLPlace.class, inline = true, required = false),
            @ElementList(entry = "arc", type = PNMLArc.class, inline = true, required = false)})
    private List<Object> list;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Object> getList() {
        return list;
    }

    public void setList(List<Object> list) {
        this.list = list;
    }

}
