package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;

public class IbmNode {

	@Attribute(required=false)
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

