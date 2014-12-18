package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import org.processmining.plugins.stochasticpetrinet.measures.AbstractMeasure;
import org.processmining.plugins.stochasticpetrinet.measures.AbstractionLevel;

/**
 * A class that captures the entropy of a net.
 */
public class EntropyMeasure extends AbstractMeasure<Double>{

	private AbstractionLevel level;
	
	public EntropyMeasure(AbstractionLevel level){
		this.level = level;
	}
	
	public String getName() {
		return "Entropy "+level.getName();
	}
	
}
