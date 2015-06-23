package org.processmining.tests.plugins.overfit;

import java.awt.Dimension;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.plugins.overfit.OverfitMiner;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class OverfitTest {

	
	public static void main(String[] args) throws Exception {
		//XLog log = TestUtils.loadLog("FiveActivities.xes");
		//XLog log = TestUtils.loadLog("ThreeActivities.xes");
		XLog log = TestUtils.loadLog("loan_process_filtered_only_A.xes");
		
		
		OverfitMiner miner = new OverfitMiner();
		Object[] netAndMarking = miner.mine(null, log);
		
		JComponent comp = getVisualization(TestUtils.getDummyConsoleProgressContext(), (Petrinet)netAndMarking[0]);
		comp.setPreferredSize(new Dimension(700, 500));
		
		JFrame frame = new JFrame("Test Visualization");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.setContentPane(comp);
		
		frame.pack(); 
		frame.setVisible(true);
	}

	private static JComponent getVisualization(PluginContext context, Petrinet net) {
		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		InitialMarkingConnection conn;
		
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition)t;
				map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel()+" ("+tt.getWeight()+")");
				String parameters = Arrays.toString(tt.getDistributionParameters());
				if (parameters.length()>101){
					parameters = parameters.substring(0, 100)+"..."+"("+tt.getDistributionParameters().length+" parameters in total)";
				}
				map.putViewSpecific(t, AttributeMap.TOOLTIP, "<html>"+t.getLabel()+"\n<br>" +
						"priority: "+tt.getPriority()+"\n<br>" +
						"weight: "+tt.getWeight()+"\n<br>" +
						"type: "+tt.getDistributionType().toString()+"\n<br>" +
						"parameters: "+parameters+"</html>");
			} else {
				map.putViewSpecific(t, AttributeMap.TOOLTIP, t.getLabel());
			}
		}
		
		ProMJGraphPanel graphPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, net, map);
		return graphPanel;
	}
}
