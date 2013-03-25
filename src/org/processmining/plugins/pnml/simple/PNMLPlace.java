package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

@Root(name="place",strict=false)
public class PNMLPlace extends AbstractPNMLElement{
	@Attribute
	private String id;
	
	@Path(value="initialMarking")
	@Element(name="text",required=false)
	private Integer initialMarking = null;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getInitialMarking() {
		return initialMarking;
	}

	public void setInitialMarking(Integer initialMarking) {
		this.initialMarking = initialMarking;
	}
	
}
