package org.processmining.plugins.stochasticpetrinet.prediction.experiment;

import java.util.Date;

public class PredictionData {

    private int caseId;

    private Date caseStartDate;
    private Date caseEndDate;

    private Date predictionTimeDate;

    public PredictionData(int caseId, Date caseStartDate, Date caseEndDate, Date predictionTimeDate, int predictions) {
        this.caseId = caseId;
        this.caseStartDate = caseStartDate;
        this.caseEndDate = caseEndDate;
        this.predictionTimeDate = predictionTimeDate;
        this.predictionDates = new Date[predictions];
    }


    /**
     * 0: naive prediction (maximum of (average process duration - elapsed time) and 0)
     * 1: state transition single last activity
     * 2: state transition set of last activities
     * 3: state transition list of last activities
     * 4: state transition multibag of last activities
     * 5: SPN model average
     * 6: SPN model constrained
     */
    private Date[] predictionDates;

//	private Date predictedNaiveDate;
//	private Date predictedStateTransitionSingleLastActivityDate;
//	private Date predictedStateTransitionSetDate;
//	private Date predictedStateTransitionListDate;
//	private Date predictedStateTransitionMultiBagDate;
//	private Date predictedSPNAverageDate;
//	private Date predictedSPNConstrainedDate;

    public int getCaseId() {
        return caseId;
    }

    public void setCaseId(int caseId) {
        this.caseId = caseId;
    }

    public Date getCaseStartDate() {
        return caseStartDate;
    }

    public void setCaseStartDate(Date caseStartDate) {
        this.caseStartDate = caseStartDate;
    }

    public Date getCaseEndDate() {
        return caseEndDate;
    }

    public void setCaseEndDate(Date caseEndDate) {
        this.caseEndDate = caseEndDate;
    }

    public Date getPredictionTimeDate() {
        return predictionTimeDate;
    }

    public void setPredictionTimeDate(Date predictionTimeDate) {
        this.predictionTimeDate = predictionTimeDate;
    }

    //	public Date getPredictedNaiveDate() {
//		return predictedNaiveDate;
//	}
//	public void setPredictedNaiveDate(Date predictedNaiveDate) {
//		this.predictedNaiveDate = predictedNaiveDate;
//	}
//	public Date getPredictedStateTransitionSingleLastActivityDate() {
//		return predictedStateTransitionSingleLastActivityDate;
//	}
//	public void setPredictedStateTransitionSingleLastActivityDate(Date predictedStateTransitionSingleLastActivityDate) {
//		this.predictedStateTransitionSingleLastActivityDate = predictedStateTransitionSingleLastActivityDate;
//	}
//	public Date getPredictedStateTransitionSetDate() {
//		return predictedStateTransitionSetDate;
//	}
//	public void setPredictedStateTransitionSetDate(Date predictedStateTransitionSetDate) {
//		this.predictedStateTransitionSetDate = predictedStateTransitionSetDate;
//	}
//	public Date getPredictedStateTransitionListDate() {
//		return predictedStateTransitionListDate;
//	}
//	public void setPredictedStateTransitionListDate(Date predictedStateTransitionListDate) {
//		this.predictedStateTransitionListDate = predictedStateTransitionListDate;
//	}
//	public Date getPredictedStateTransitionMultiBagDate() {
//		return predictedStateTransitionMultiBagDate;
//	}
//	public void setPredictedStateTransitionMultiBagDate(Date predictedStateTransitionMultiBagDate) {
//		this.predictedStateTransitionMultiBagDate = predictedStateTransitionMultiBagDate;
//	}
//	public Date getPredictedSPNAverageDate() {
//		return predictedSPNAverageDate;
//	}
//	public void setPredictedSPNAverageDate(Date predictedSPNAverageDate) {
//		this.predictedSPNAverageDate = predictedSPNAverageDate;
//	}
//	public Date getPredictedSPNConstrainedDate() {
//		return predictedSPNConstrainedDate;
//	}
//	public void setPredictedSPNConstrainedDate(Date predictedSPNConstrainedDate) {
//		this.predictedSPNConstrainedDate = predictedSPNConstrainedDate;
//	}
    public Date[] getPredictionDates() {
        return predictionDates;
    }

    public void setPredictionDates(Date[] predictionDates) {
        this.predictionDates = predictionDates;
    }
}
