package org.processmining.plugins.alignment.model;

import java.util.ArrayList;
import java.util.List;

import com.timeseries.TimeSeriesBit;

public class CaseTimeSeries {

	public static final String PARAMETER_LABEL = "Case Time Series";
	
	private List<TimeSeriesBit> timeSeries;
	
	public CaseTimeSeries(){
		timeSeries = new ArrayList<>();
	}
	
	public List<TimeSeriesBit> getTimeSeriesList(){
		return this.timeSeries;
	}
	
	public TimeSeriesBit getTimeSeries(int caseId){
		return this.timeSeries.get(caseId);
	}

	public void add(TimeSeriesBit timeSeriesBit) {
		this.timeSeries.add(timeSeriesBit);
		
	}

}
