package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;

public class IbmLoopCondition {

	@Attribute(name="name")
	protected String condition;

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
