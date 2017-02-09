package org.processmining.plugins.stochasticpetrinet.prediction.experiment;

import java.util.List;

public class PredictionExperimentResult {

    private List<List<PredictionData>> predictionResults;

    public List<List<PredictionData>> getPredictionResults() {
        return predictionResults;
    }

    public void setPredictionResults(List<List<PredictionData>> predictionResults) {
        this.predictionResults = predictionResults;
    }

}
