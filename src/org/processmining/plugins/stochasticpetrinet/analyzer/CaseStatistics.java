package org.processmining.plugins.stochasticpetrinet.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;

/**
 * Case statistics for an individual case in a log.
 * Stores probabilistic information about a case that can be created during 
 * replay in a probabilistic model (e.g. {@link StochasticNet}).
 * 
 * @see StochasticManifestCollector
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class CaseStatistics {

	private long caseId;
	private List<ReplayStep> replaySteps;
	private List<Double> choices;
	private Double logLikelihood;
	private Double caseDuration;
	

	public CaseStatistics(long caseId) {
		this.caseId = caseId;
		this.replaySteps = new ArrayList<ReplayStep>();
		choices = new ArrayList<Double>();
		logLikelihood = 0.0;
	}
	
	public void addDuration(String transitionName, double duration, double density){
		this.replaySteps.add(new ReplayStep(transitionName, duration, density));
	}
	
	public void makeChoice(Double probability){
		assert(probability >= 0 && probability <= 1);
		choices.add(probability);
	}

	public Double getLogLikelihood() {
		return logLikelihood;
	}

	public void setLogLikelihood(Double logLikelihood) {
		this.logLikelihood = logLikelihood;
	}

	public Double getCaseDuration() {
		return caseDuration;
	}

	public void setCaseDuration(Double caseDuration) {
		this.caseDuration = caseDuration;
	}

	public List<ReplayStep> getReplaySteps() {
		return replaySteps;
	}

	public long getCaseId() {
		return caseId;
	}

	public List<Double> getChoices() {
		return choices;
	}
	
	public String toString(){
		return toString(";");
	}
	public String toString(String separator){
		StringBuilder builder = new StringBuilder();
		builder.append(caseId).append(separator);
		builder.append(logLikelihood).append(separator);
		builder.append(Arrays.toString(choices.toArray())).append(separator);
		builder.append(Arrays.toString(replaySteps.toArray())).append(separator);
		return builder.toString();
	}
	
	/**
	 * A Replay Step with the duration and 
	 * probabilistic information like density p(A=a | Model) 
	 * according to the probabilistic Model used in replay. 
	 * 
	 * @author Andreas Rogge-Solti
	 *
	 */
	public class ReplayStep{
		public String transitionName;
		public Double duration;
		public Double density;
		
		public ReplayStep(String transitionName, double duration, double density) {
			this.transitionName = transitionName;
			this.duration = duration;
			this.density = density;
		}

		public String toString(){
			return "{name: "+transitionName+", duration: "+duration+", density: "+density+"}";
		}
	}
}
