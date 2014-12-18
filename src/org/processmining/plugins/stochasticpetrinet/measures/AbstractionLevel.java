package org.processmining.plugins.stochasticpetrinet.measures;

public abstract class AbstractionLevel {

	public abstract String getName();
	
	public abstract int[] abstractFrom(int[] rawEncoding);
}
