package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(strict = false)
@Element(name = "decision")
public class IbmDecision extends IbmGateway {

	@Attribute
	private boolean isInclusive;

	public boolean isInclusive() {
		return isInclusive;
	}

	public void setInclusive(boolean isInclusive) {
		this.isInclusive = isInclusive;
	}

}
