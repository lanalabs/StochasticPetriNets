package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="connection")
public class IbmConnection extends IbmNode {
	
	@Attribute(name="node", required=false)
	@Path("source")
	private String sourceNode;
	
	@Attribute(name="contactPoint", required=false)
	@Path("source")
	private String sourceContactPoint;

	@Attribute(name="node", required=false)
	@Path("target")
	private String targetNode;
	
	@Attribute(name="contactPoint", required=false)
	@Path("target")
	private String targetContactPoint;

	public String getSourceNode() {
		return sourceNode;
	}

	public void setSourceNode(String sourceNode) {
		this.sourceNode = sourceNode;
	}

	public String getSourceContactPoint() {
		return sourceContactPoint;
	}

	public void setSourceContactPoint(String sourceContactPoint) {
		this.sourceContactPoint = sourceContactPoint;
	}

	public String getTargetNode() {
		return targetNode;
	}

	public void setTargetNode(String targetNode) {
		this.targetNode = targetNode;
	}

	public String getTargetContactPoint() {
		return targetContactPoint;
	}

	public void setTargetContactPoint(String targetContactPoint) {
		this.targetContactPoint = targetContactPoint;
	}
	
	@Transient
	public String getNodeName(){
		return "connection";
	}
}
