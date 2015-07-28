package org.processmining.tests.plugins.temporal;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Test;
import org.processmining.plugins.alignment.TimeSeriesComparer;
import org.processmining.plugins.alignment.TimeSeriesConverter;
import org.processmining.plugins.alignment.model.CaseTimeSeries;
import org.processmining.plugins.alignment.ui.TimeSeriesPanel;
import org.processmining.plugins.temporal.TemporalMiner;
import org.processmining.plugins.temporal.model.TemporalModel;
import org.processmining.plugins.temporal.ui.TemporalModelVizualizer;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class TemporalTest {
	
	@Test
	public void testTimeSeriesConverter() throws Exception {
		XLog log = TestUtils.loadLog("financial_log.xes.gz");
		System.out.println("log loaded with "+log.size()+" traces.");
		CaseTimeSeries caseTimeSeries = TimeSeriesConverter.convert(null, log);
		System.out.println("log converted to time series.");
		System.out.println(caseTimeSeries.getTimeSeries(0));
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new TimeSeriesPanel(caseTimeSeries.getTimeSeries(0)));
		frame.pack();
		frame.setPreferredSize(new Dimension(300, 100));
		frame.setVisible(true);
		
		long currentTime = System.currentTimeMillis();
		System.out.println("starting distance matrix computation...");
		double[] distanceMatrix = TimeSeriesComparer.getDistances(null, caseTimeSeries);
		long afterComputation = System.currentTimeMillis();
		System.out.println("computation took : "+(afterComputation-currentTime)+" ms");
		System.out.println("distance between 0 and 1: "+distanceMatrix[0]);

		Thread.sleep(1000000);
	}
	
	@Test
	public void testParallelTimeSeries() throws Exception {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("A", trace, 0, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace, 10, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace, 70, XLifecycleExtension.StandardModel.COMPLETE);
		TestUtils.addEvent("A", trace, 100, XLifecycleExtension.StandardModel.COMPLETE);
		log.add(trace);
		
		XTrace trace2 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("C", trace2, 5, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace2, 20, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace2, 75, XLifecycleExtension.StandardModel.COMPLETE);
		TestUtils.addEvent("C", trace2, 110, XLifecycleExtension.StandardModel.COMPLETE);
		log.add(trace2);
		
		XTrace trace3 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("A", trace3, 15, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace3, 20, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace3, 80, XLifecycleExtension.StandardModel.COMPLETE);
		TestUtils.addEvent("A", trace3, 210, XLifecycleExtension.StandardModel.COMPLETE);
		log.add(trace3);
		
		
		XTrace trace4 = XFactoryRegistry.instance().currentDefault().createTrace();
		TestUtils.addEvent("A", trace4, 15, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("B", trace4, 20, XLifecycleExtension.StandardModel.START);
		TestUtils.addEvent("A", trace4, 180, XLifecycleExtension.StandardModel.COMPLETE);
		TestUtils.addEvent("B", trace4, 210, XLifecycleExtension.StandardModel.COMPLETE);
		log.add(trace4);
		
		
		
		CaseTimeSeries caseTimeSeries = TimeSeriesConverter.convert(null, log);
		System.out.println(caseTimeSeries.getTimeSeries(0));
		

		long currentTime = System.currentTimeMillis();
		System.out.println("starting distance matrix computation...");
		double[] distanceMatrix = TimeSeriesComparer.getDistances(null, caseTimeSeries);
		long afterComputation = System.currentTimeMillis();
		System.out.println("computation took : "+(afterComputation-currentTime)+" ms");
		System.out.println("distance between 0 and 1: "+distanceMatrix[0]);
		
		
//		AbstractDistanceBasedAlgorithm algorithm = new SLINK(CosineDistanceFunction.STATIC);
//		ArrayAdapter<T, A> connection = new FlatMatrixAdapter();
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(new TimeSeriesPanel(caseTimeSeries.getTimeSeries(0)));
		frame.pack();
		frame.setPreferredSize(new Dimension(300, 100));
		frame.setVisible(true);
		Thread.sleep(1000000);
	}

	public static void main(String[] args) throws Exception {
		//XLog log = TestUtils.loadLog("FiveActivities.xes");
		//XLog log = TestUtils.loadLog("ThreeActivities.xes");
		XLog log = TestUtils.loadLog("loan_process_filtered_only_A.xes");
		
		
		TemporalMiner miner = new TemporalMiner();
		TemporalModel model = miner.mine(null, log);
		
		JComponent comp = TemporalModelVizualizer.visualize(null, model);
		
		JFrame frame = new JFrame("Test Visualization");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setContentPane(comp);
		
		frame.pack(); 
		frame.setVisible(true);
	}
}
