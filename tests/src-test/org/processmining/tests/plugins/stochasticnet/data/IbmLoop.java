package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="loop")
public class IbmLoop extends IbmProcess {

	@Attribute(required=false)
	protected boolean isConditionTestedFirst = false;
	
	@Element(name="loopCondition")
	protected IbmLoopCondition loopCondition;

	public boolean isConditionTestedFirst() {
		return isConditionTestedFirst;
	}

	public void setConditionTestedFirst(boolean isConditionTestedFirst) {
		this.isConditionTestedFirst = isConditionTestedFirst;
	}

	public IbmLoopCondition getLoopCondition() {
		return loopCondition;
	}

	public void setLoopCondition(IbmLoopCondition loopCondition) {
		this.loopCondition = loopCondition;
	}
	@Transient
	public String getNodeName(){
		return "loop";
	}
}
