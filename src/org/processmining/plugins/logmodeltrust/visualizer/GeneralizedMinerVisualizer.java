package org.processmining.plugins.logmodeltrust.visualizer;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.deckfour.xes.model.XLog;
import org.jgraph.JGraph;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.logmodeltrust.miner.GeneralizedMiner;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.visualization.tree.TreeLayoutBuilder;

public class GeneralizedMinerVisualizer implements ChangeListener {
	
	private JSlider modelTrust; 
	private JSlider logTrust;
	
	private double trustInModel = 1;
	private double trustInLog = 1;
	
	private JPanel graphPanel;
	
	private JPanel panel;
	private GeneralizedMiner miner;
	
	@Plugin(name = "Generalized Miner Visualizer", returnLabels = { "Visualized Generalized Miner" }, returnTypes = { JComponent.class }, parameterLabels = { GeneralizedMiner.PARAMETER_LABEL }, userAccessible = true)
	@Visualizer
	public static JComponent visualize(PluginContext context, GeneralizedMiner gMiner) {
		GeneralizedMinerVisualizer visualizer = new GeneralizedMinerVisualizer();
		return visualizer.getVisualization(context, gMiner);
	}

	private JComponent getVisualization(PluginContext context, GeneralizedMiner gMiner) {
		miner = gMiner;
		panel = new JPanel(new BorderLayout());
		
		modelTrust = new JSlider(0, 100, 100);
		logTrust = new JSlider(0, 100, 100);
		
		JComponent controls = new JPanel();
		controls.add(new JLabel("trust in model:"));
		controls.add(modelTrust);
		controls.add(new JLabel(" "));
		controls.add(new JLabel("trust in log:"));
		controls.add(logTrust);
		panel.add(controls, BorderLayout.SOUTH);
		
		modelTrust.addChangeListener(this);
		logTrust.addChangeListener(this);
		
		Pair<XLog, ProcessTree> bestPair = miner.getFittingPair(1, 1);
		
		updateGraph(bestPair.getSecond());
		
		return panel;
	}

	private void updateGraph(ProcessTree tree) {
		TreeLayoutBuilder builder = new TreeLayoutBuilder(tree);
		JGraph graph = builder.getJGraph();
		
		if (graphPanel != null){
			panel.remove(graphPanel);
		}
		graphPanel = new JPanel();
		graphPanel.add(new JScrollPane(graph));
		panel.add(graphPanel, BorderLayout.CENTER);
		panel.repaint();
		panel.revalidate();
	}

	public void stateChanged(ChangeEvent e) {
		boolean changed = false;
		if (e.getSource().equals(modelTrust)){
			double trust = modelTrust.getValue() / 100.;
			if (trustInModel != trust){
				changed = true;
			}
			trustInModel = trust;
		} else if (e.getSource().equals(logTrust)){
			double trust = logTrust.getValue() / 100.;
			if (trustInLog != trust){
				changed = true;
			}
			trustInLog = trust;
		}
		if (changed){
			updateModelAndLogPair();
		}
	}

	private void updateModelAndLogPair() {
		Pair<XLog,ProcessTree> logTreePair = miner.getFittingPair(trustInLog, trustInModel);
		System.out.println(logTreePair.getSecond());
		updateGraph(logTreePair.getSecond());
		
		// align log and model:
		System.out.println("new fitness: "+StochasticNetUtils.getDistance(miner.getLastModelPetriNet(), logTreePair.getFirst()));
	}
}
