package org.processmining.plugins.pnml.simple;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name="graphics")
public class PNMLGraphics {
	
	@ElementList(name="position",inline=true,required=false)
	private List<PNMLPoint> position;
	
	@Element(name="dimension", required=false)
	private PNMLPoint dimension;
	
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
	
	
}
