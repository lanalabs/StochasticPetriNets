package org.processmining.plugins.stochasticpetrinet.enricher;

import java.io.File;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;

/**
 * Configuration to be used for the net to be mined.
 * @author Andreas Rogge-Solti
 *
 */
public class PerformanceEnricherConfig {
	
	private DistributionType type;
	private TimeUnit timeUnit;
	private ExecutionPolicy policy;
	
	private File correlationMatrixFile;
	
	public PerformanceEnricherConfig(DistributionType distType, TimeUnit timeUnit, ExecutionPolicy executionPolicy, File correlationMatrixFile) {
		this.type = distType;
		this.timeUnit = timeUnit;
		this.policy = executionPolicy;
		this.correlationMatrixFile = correlationMatrixFile;
	}

	public DistributionType getType() {
		return type;
	}

	public void setType(DistributionType type) {
		this.type = type;
	}

	public Double getUnitFactor() {
		return timeUnit.getUnitFactorToMillis();
	}

	public void setUnitFactor(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public ExecutionPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(ExecutionPolicy policy) {
		this.policy = policy;
	}

	public File getCorrelationMatrixFile() {
		return correlationMatrixFile;
	}

	public void setCorrelationMatrixFile(File correlationMatrixFile) {
		this.correlationMatrixFile = correlationMatrixFile;
	}

	public TimeUnit getTimeUnit() {
		return this.timeUnit;
	}
}
