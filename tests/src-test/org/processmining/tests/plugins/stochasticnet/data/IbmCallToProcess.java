package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="callToProcess")
public class IbmCallToProcess extends IbmCall{

	@Attribute(name="process", required=false)
	private String process;

	public String getProcess() {
		return process;
	}

	public void setProcess(String process) {
		this.process = process;
	}
	
	@Transient
	public String getNodeName(){
		return "callToProcess";
	}
}
