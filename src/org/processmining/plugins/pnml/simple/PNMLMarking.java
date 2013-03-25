package org.processmining.plugins.pnml.simple;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name="marking")
public class PNMLMarking {
	@ElementList(inline=true,required=false)
	private List<PNMLPlaceRef> places;

	public List<PNMLPlaceRef> getPlaces() {
		return places;
	}

	public void setPlaces(List<PNMLPlaceRef> places) {
		this.places = places;
	}	
}
