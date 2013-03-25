package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;

@Element(name="arc")
public class PNMLArc extends AbstractPNMLElement{
	@Attribute
	private String id;
	@Attribute
	private String source;
	@Attribute
	private String target;
	
	@Path("inscription")
	@Element(name="text",required=false)
	private Integer inscription = null; 
	
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
	public int getInscription() {
		return inscription;
	}
	public void setInscription(int inscription) {
		this.inscription = inscription;
	}
	
}
