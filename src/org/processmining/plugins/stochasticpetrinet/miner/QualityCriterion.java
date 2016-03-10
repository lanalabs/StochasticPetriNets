package org.processmining.plugins.stochasticpetrinet.miner;

public enum QualityCriterion {
	FITNESS, // measures log replay fitness, i.e., indicates the number traces that can be replayed correctly. 
	PRECISION, // measures excess of modeled behavior over the one encountered in the log.
	GENERALIZATION, // measures the chance of the model to account for new traces.
	SIMPLICITY // measures the model complexity -> mostly counts the number of nodes and arcs in the model.
	
}
