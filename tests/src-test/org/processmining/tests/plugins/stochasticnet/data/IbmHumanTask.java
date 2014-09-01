package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="humanTask")
public class IbmHumanTask extends IbmTask {

	@Transient
	public String getNodeName(){
		return "humanTask";
	}
}
