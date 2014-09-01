package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="service")
public class IbmService extends IbmNode {

	@Transient
	public String getNodeName(){
		return "service";
	}
}
