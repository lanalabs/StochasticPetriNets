package org.processmining.plugins.stochasticpetrinet.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
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

	
	/** the least probable 1 percent is considered as outliers per default */
	public static final double DEFAULT_OUTLIER_RATE = 0.01; 	
	
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
	
	public void addDuration(TimedTransition transition, double duration, double density, Set<TimedTransition> predecessorTimedTransitions){
		this.replaySteps.add(new ReplayStep(transition, duration, density, predecessorTimedTransitions));
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
		public TimedTransition transition;
		public Double duration;
		public Double density;
		
		public Set<TimedTransition> parents;
		public Set<TimedTransition> children;
		
		public ReplayStep(TimedTransition transition, double duration, double density, Set<TimedTransition> predecessorTimedTransitions) {
			this.transition = transition;
			this.duration = duration;
			this.density = density;
			this.parents = new HashSet<TimedTransition>();
			this.parents.addAll(predecessorTimedTransitions);
			this.children = new HashSet<TimedTransition>();
		}

		public String toString(){
			return "{"+transition.getLabel()+", dur: "+duration+", dens: "+density+"}";
		}
	}
}
