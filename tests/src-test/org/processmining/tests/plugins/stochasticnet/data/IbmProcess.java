package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict=false)
@Element(name="process")
public class IbmProcess extends IbmNode{
	@Element(required=false)
	private String description;
	
	@Element
	private IbmFlowContent flowContent;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public IbmFlowContent getFlowContent() {
		return flowContent;
	}

	public void setFlowContent(IbmFlowContent flowContent) {
		this.flowContent = flowContent;
	}
}
