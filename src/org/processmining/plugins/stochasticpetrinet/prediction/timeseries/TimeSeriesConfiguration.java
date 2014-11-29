package org.processmining.plugins.stochasticpetrinet.prediction.timeseries;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.AutoArimaTimeSeries;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.AverageMethodTimeSeries;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.NaiveMethodTimeSeries;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.SeasonalNaiveMethodTimeSeries;
import org.processmining.plugins.stochasticpetrinet.simulator.timeseries.TimeSeries;

public class TimeSeriesConfiguration {
	
	
	public enum TimeSeriesType{
		AUTO_ARIMA, AVERAGE_METHOD, NAIVE_METHOD, SEASONAL_METHOD, DRIFT_METHOD;
		
		public Class<? extends TimeSeries<Double>> getTimeSeriesClass(){
			switch(this){
				case AUTO_ARIMA:
					return AutoArimaTimeSeries.class;
				case SEASONAL_METHOD:
					return SeasonalNaiveMethodTimeSeries.class;
				case NAIVE_METHOD:
					return NaiveMethodTimeSeries.class;
				case AVERAGE_METHOD:
				default:
					return AverageMethodTimeSeries.class;
			}
		}
	}
	
	private List<AvailableScripts> scripts;
	
	/** used window  */
	private Calendar lag;
	
	/** the granularity for aggregation can be minute, hour, day... */
	private Calendar aggregation; 
	
	private TimeSeriesType type;
	
	public TimeSeriesConfiguration(){
		this.scripts = new ArrayList<>();
		scripts.add(AvailableScripts.METRIC_SCRIPT);
		scripts.add(AvailableScripts.CATEGORICAL_SCRIPT);
		
		lag = new GregorianCalendar();
		lag.setTime(new Date(0l));
		lag.add(Calendar.DAY_OF_MONTH, 14); // two weeks window of hourly data
		
		aggregation = new GregorianCalendar();
		aggregation.setTime(new Date(0l));
		aggregation.add(Calendar.HOUR_OF_DAY, 1); // hourly data by default 
		
		type = TimeSeriesType.AVERAGE_METHOD;
	}
	
	public enum AvailableScripts{
		METRIC_SCRIPT, CATEGORICAL_SCRIPT;
		
		public String getPath(){
			switch(this){
				case METRIC_SCRIPT:
					return "scripts/metric_prediction.r";
				case CATEGORICAL_SCRIPT:
					return "scripts/categorical_prediction.r";
			}
			return "unspecified Path!!";
		}
	}
	
	public void setTimeSeriesType(TimeSeriesType type){
		this.type = type;
	}
	
	public List<AvailableScripts> getScriptsToLoad(){
		return scripts;
	}
	
	public void setScriptsToLoad(List<AvailableScripts> scripts){
		this.scripts = scripts;
	}
	
	public long getIndexForTime(long time){
		return (long)Math.floor(time/aggregation.getTimeInMillis());
	}

	public void setLag(Calendar lag) {
		this.lag = lag;
	}

	public void setAggregation(Calendar aggregation) {
		this.aggregation = aggregation;
	}

	public TimeSeries<Double> createNewTimeSeries(TimedTransition timedT) {
		Class<? extends TimeSeries<Double>> myClass = type.getTimeSeriesClass();
		try {
			return myClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
