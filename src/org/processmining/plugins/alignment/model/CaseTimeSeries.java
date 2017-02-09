package org.processmining.plugins.alignment.model;

import com.timeseries.TimeSeriesBit;

import java.util.ArrayList;
import java.util.List;

public class CaseTimeSeries {

    public static final String PARAMETER_LABEL = "Case Time Series";

    private List<TimeSeriesBit> timeSeries;

    public CaseTimeSeries() {
        timeSeries = new ArrayList<>();
    }

    public List<TimeSeriesBit> getTimeSeriesList() {
        return this.timeSeries;
    }

    public TimeSeriesBit getTimeSeries(int caseId) {
        return this.timeSeries.get(caseId);
    }

    public void add(TimeSeriesBit timeSeriesBit) {
        this.timeSeries.add(timeSeriesBit);
    }

    public String[] getNames() {
        String[] names = new String[timeSeries.size()];
        int i = 0;
        for (TimeSeriesBit ts : timeSeries) {
            names[i++] = "case" + i;
        }
        return names;
    }

}
