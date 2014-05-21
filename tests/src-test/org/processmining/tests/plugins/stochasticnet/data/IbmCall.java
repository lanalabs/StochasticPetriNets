package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;

public abstract class IbmCall extends IbmNode {

	@Attribute(required=false)
	private boolean callSynchronously;
	
	public boolean isCallSynchronously() {
		return callSynchronously;
	}

	public void setCallSynchronously(boolean callSynchronously) {
		this.callSynchronously = callSynchronously;
	}
	
	// ignore additional inputs and outputs 
	
}
