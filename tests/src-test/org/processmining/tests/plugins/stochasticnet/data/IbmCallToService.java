package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="callToService")
public class IbmCallToService extends IbmCall {
	@Attribute(name="service", required=false)
	private String service;

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}
	
	@Transient
	public String getNodeName(){
		return "callToService";
	}
}
