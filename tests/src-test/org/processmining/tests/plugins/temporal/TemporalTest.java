package org.processmining.tests.plugins.temporal;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.temporal.TemporalMiner;
import org.processmining.plugins.temporal.model.TemporalModel;
import org.processmining.plugins.temporal.ui.TemporalModelVizualizer;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class TemporalTest {

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
