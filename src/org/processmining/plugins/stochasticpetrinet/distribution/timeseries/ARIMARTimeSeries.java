//package org.processmining.plugins.stochasticpetrinet.distribution.timeseries;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.math3.distribution.RealDistribution;
//import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
//import org.rosuda.REngine.REngine;
//
///**
// * A ARIMA time series powered by R
// * 
// * @author Andreas Rogge-Solti
// *
// */
//public class ARIMARTimeSeries extends StatefulTimeseriesDistribution {
//
//	private List<Double> values;
//	
//	boolean initialized = false;
//
//	private long lastTime;
//	
//	private REngine engine;
//	
//	public ARIMARTimeSeries(RealDistribution noiseDist) {
//		super(noiseDist);
//		this.values = new ArrayList<>();
//		this.engine = RProvider.getREngine();
//	}
//
//	protected double getCurrentSeriesValue(long currentTime) {
//		if (!initialized){
//			this.lastTime = currentTime;
//			values.add(e)
//		}
//		
//		return 0;
//	}
//
//}
