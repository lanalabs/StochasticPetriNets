package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Case statistics for an individual case in a log.
 * Stores probabilistic information about a case that can be created during
 * replay in a probabilistic model (e.g. {@link StochasticNet}).
 *
 * @author Andreas Rogge-Solti
 * @see StochasticManifestCollector
 */
public class CaseStatistics {


    /**
     * the least probable 1 percent is considered as outliers per default
     */
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

    public void addReplayStep(ReplayStep step) {
        this.replaySteps.add(step);
    }

//	public void addDuration(TimedTransition transition, double duration, double density, Set<ReplayStep> predecessorTimedTransitions){
//		this.replaySteps.add(new ReplayStep(transition, duration, density, predecessorTimedTransitions));
//	}

    public void makeChoice(Double probability) {
        assert (probability >= 0 && probability <= 1);
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

    public String toString() {
        return toString(";");
    }

    public String toString(String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append(caseId).append(separator);
        builder.append(logLikelihood).append(separator);
        builder.append(Arrays.toString(choices.toArray())).append(separator);
        builder.append(Arrays.toString(replaySteps.toArray())).append(separator);
        return builder.toString();
    }
}
