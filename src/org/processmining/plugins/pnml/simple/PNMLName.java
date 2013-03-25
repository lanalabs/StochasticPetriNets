package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

@Element(name="name")
public class PNMLName {
	@Element(name="text")
	private String value;
	
	@Path("graphics")
	@Element(name="offset",required=false)
	private PNMLPoint offset;
	
	public PNMLName(){}
	
	public PNMLName(String name){
		this.value = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public PNMLPoint getOffset() {
		return offset;
	}

	public void setOffset(PNMLPoint offset) {
		this.offset = offset;
	}
}
