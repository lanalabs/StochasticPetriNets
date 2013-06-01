package org.processmining.plugins.stochasticpetrinet.enricher;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;

/**
 * Configuration to be used for the net to be mined.
 * @author Andreas Rogge-Solti
 *
 */
public class PerformanceEnricherConfig {
	
	private DistributionType type;
	private Double unitFactor;
	private ExecutionPolicy policy;
	
	public PerformanceEnricherConfig(DistributionType distType, Double timeUnit, ExecutionPolicy executionPolicy) {
		this.type = distType;
		this.unitFactor = timeUnit;
		this.policy = executionPolicy;
	}

	public DistributionType getType() {
		return type;
	}

	public void setType(DistributionType type) {
		this.type = type;
	}

	public Double getUnitFactor() {
		return unitFactor;
	}

	public void setUnitFactor(Double unitFactor) {
		this.unitFactor = unitFactor;
	}

	public ExecutionPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(ExecutionPolicy policy) {
		this.policy = policy;
	}	
}
