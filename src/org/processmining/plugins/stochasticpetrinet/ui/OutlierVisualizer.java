package org.processmining.plugins.stochasticpetrinet.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.distribution.RealDistribution;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.elements.ProMGraphCell;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsConnection;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatisticsList;
import org.processmining.plugins.stochasticpetrinet.analyzer.CaseStatistics.ReplayStep;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;

public class OutlierVisualizer implements ActionListener{
	
	private static final int SAMPLE_SIZE = 10000;

	private static final String DEFAULT_OUTLIER_RATE = "0.05";

	private double outlierRate = 0.05;
	
	private ProMJGraphPanel graphPanel;
	
	private JPanel myPanel;
	
	/**
	 * Shows the plot of the transition in a box
	 */
	private PlotPanelFreeChart plotForTransition;
	
	/**
	 * Shows the plot of the likelihood of a transition in a box
	 */
	private PlotPanelFreeChart plotForLikelihood;
	
	private JTextField outlierPercentageField;
	private JButton updateButton;
	private JTable caseList;
	
	private Map<Transition, Double> logLikelihoodCutoffs;
	private Map<Transition, GaussianKernelDistribution> logLikelihoodDistributions;
	private Map<String,TimedTransition> transitionsByName;
	
	private CaseStatisticsList caseStatistics;

	private StochasticNet stochasticNet;

	private Map<CaseStatistics, List<TimedTransition>> numberOfIndividualOutliers;
	
	private CaseStatistics selectedCaseStatistics;

	private PluginContext context;
	
	@Plugin(name = "Outlier Visualizer", returnLabels = { "Outlier Exporer" }, returnTypes = { JComponent.class }, parameterLabels = { CaseStatisticsList.PARAMETER_LABEL }, userAccessible = false)
	@Visualizer
	public JComponent visualize(PluginContext context, CaseStatisticsList likelihoodList) {
		this.context = context;
		try{
			CaseStatisticsConnection connection = context.getConnectionManager().getFirstConnection(CaseStatisticsConnection.class, context, likelihoodList);
			if (connection != null){
				this.stochasticNet = connection.getObjectWithRole(CaseStatisticsConnection.STOCHASTIC_NET);
			} else {
				return new JLabel("Cannot find connection to stochastic net of the statistics");
			}
		} catch (ConnectionCannotBeObtained e){
			return new JLabel(e.getMessage());
		}
		this.caseStatistics = likelihoodList;
		this.logLikelihoodDistributions = new HashMap<Transition, GaussianKernelDistribution>();
		transitionsByName = getTransitionsByName(stochasticNet.getTransitions());
		
		plotForTransition = new PlotPanelFreeChart();
		plotForTransition.setPreferredSize(new Dimension(300,150));
		plotForLikelihood = new PlotPanelFreeChart();
		plotForLikelihood.setPreferredSize(plotForTransition.getPreferredSize());
		JPanel distributionPanel = new JPanel();
		distributionPanel.setLayout(new GridLayout(2, 1));
		distributionPanel.add(plotForTransition);
		distributionPanel.add(plotForLikelihood);
		
		
		initList();
		
		myPanel = new JPanel();
		myPanel.setLayout(new BorderLayout());
		
		updateMap(-1);
		
		myPanel.add(distributionPanel,BorderLayout.EAST);
		
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout());
		JPanel headerPanel = new JPanel();
		headerPanel.add(new JLabel("enter outlier rate (recommended value: 0.05)"));
		outlierPercentageField = new JTextField(DEFAULT_OUTLIER_RATE);
		updateButton = new JButton("update");
		updateButton.addActionListener(this);
		headerPanel.add(outlierPercentageField);
		headerPanel.add(updateButton);
		infoPanel.add(headerPanel, BorderLayout.NORTH);
		
		// create table with cases:
		TableModel dataModel = new OutlierTableModel(this.caseStatistics, this.numberOfIndividualOutliers);
		this.caseList = new JTable(dataModel);
		this.caseList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if (!event.getValueIsAdjusting()){
					System.out.println("selection event: first index = " + event.getFirstIndex()+", last index = "+event.getLastIndex()+" is adjusting: "+event.getValueIsAdjusting());
					updateTableSelection(caseList.getSelectedRow());
				}
	        }
	    });
	    JScrollPane scrollpane = new JScrollPane(this.caseList);
	    infoPanel.add(scrollpane, BorderLayout.CENTER);
	    infoPanel.setPreferredSize(new Dimension(800,300));
		
		myPanel.add(infoPanel, BorderLayout.SOUTH);
		
		return myPanel;
	}

	/**
	 * Shows the graph
	 * @param context {@link PluginContext} from which to retrieve connections to the petri net. 
	 * @param selectedIndex the index of the case in the {@link #caseStatistics} or -1, if no case is selected
	 */
	private void updateMap(int selectedIndex) {
		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
		
		List<TimedTransition> outlierTransitions = new ArrayList<TimedTransition>();
		List<TimedTransition> regularTransitions = new ArrayList<TimedTransition>();
		selectedCaseStatistics = null;
		if (selectedIndex >= 0){
			selectedCaseStatistics = caseStatistics.get(selectedIndex);
			outlierTransitions = numberOfIndividualOutliers.get(selectedCaseStatistics);
			for (ReplayStep rs : selectedCaseStatistics.getReplaySteps()){
				if (transitionsByName.containsKey(rs.transitionName)){
					regularTransitions.add(transitionsByName.get(rs.transitionName));
				}
			}
		}
		InitialMarkingConnection conn;
		Marking m = new Marking();
		try {
			conn = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, this.stochasticNet);
			m = conn.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained e1) {
			e1.printStackTrace();
		}
		// add initial marking labels
		for (Place p : m) {
			String label = "" + m.occurrences(p);
			map.putViewSpecific(p, AttributeMap.LABEL, label);
			map.putViewSpecific(p, AttributeMap.TOOLTIP, p.getLabel());
			map.putViewSpecific(p, AttributeMap.SHOWLABEL, !label.equals(""));
		}
		for (Transition t : this.stochasticNet.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition)t;
				if (outlierTransitions.contains(tt)){
					map.putViewSpecific(t, AttributeMap.FILLCOLOR, Color.ORANGE);
				} else if (regularTransitions.contains(tt)){
					map.putViewSpecific(t, AttributeMap.FILLCOLOR, Color.GREEN);
				}
				map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel()+" ("+tt.getWeight()+")");
				ReplayStep step = getReplayStepForTransition(tt);
				String distributionParameters = Arrays.toString(tt.getDistributionParameters());
				distributionParameters = distributionParameters.substring(0, Math.min(50,distributionParameters.length()-1))+"...";
				map.putViewSpecific(t, AttributeMap.TOOLTIP, "<html>"+t.getLabel()+"\n<br>" +
						(step==null?"":"duration: "+step.duration+"<br>\n"+
								"density: "+step.density+"<br>\n"+
								"loglikelihood: "+Math.log(step.density)+"<br>\n") +
						"threshold: "+logLikelihoodCutoffs.get(tt)+"<br>\n" +
						"priority: "+tt.getPriority()+"\n<br>" +
						"weight: "+tt.getWeight()+"\n<br>" +
						"type: "+tt.getDistributionType().toString()+"\n<br>" +
						"parameters: "+distributionParameters+"</html>");
			} else {
				map.putViewSpecific(t, AttributeMap.TOOLTIP, t.getLabel());
			}
		}
		
		if (graphPanel != null){
			myPanel.remove(graphPanel);
		}
		graphPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, this.stochasticNet, map);
		
		graphPanel.getGraph().addGraphSelectionListener(new GraphSelectionListener() {
			public void valueChanged(GraphSelectionEvent e) {
				for (Object selectedCell : e.getCells()) {
					if (e.isAddedCell(selectedCell) && selectedCell instanceof ProMGraphCell) {
						DirectedGraphNode node = ((ProMGraphCell) selectedCell).getNode();
						if (node instanceof TimedTransition){
							
							TimedTransition transition = (TimedTransition)node;
							
							// update graph plots
							ReplayStep rs = getReplayStepForTransition(transition);
							
							List<Plot> plots = new ArrayList<Plot>();
							Plot plot = new Plot("duration of "+transition.getLabel());
//							plot.add(new DistributionWrapper(transition.getDistribution()));
							plot.add(transition.getDistribution());
							plots.add(plot);
							plotForTransition.setPlots(plots);
							plotForTransition.clearPointsOfInterest();
							
							List<Plot> likelihoodPlots = new ArrayList<Plot>();
							Plot likelihoodPlot = new Plot("log-Likelihood of "+transition.getLabel());
							likelihoodPlot.add(logLikelihoodDistributions.get(transition));
							likelihoodPlots.add(likelihoodPlot);
							plotForLikelihood.setPlots(likelihoodPlots);
							plotForLikelihood.clearPointsOfInterest();
							
							double cutOff = logLikelihoodCutoffs.get(transition);
							plotForLikelihood.addPointOfInterest(MessageFormat.format("{0,number,#.##%}", outlierRate)+"-cutoff", cutOff, Color.BLUE, 1f);
							
							if (rs != null){
								
								double value = Math.log(rs.density);
								boolean outlier = value<cutOff;
								plotForLikelihood.addPointOfInterest(MessageFormat.format("{0,number,#.##}", value), value, (outlier?Color.RED:Color.GREEN.darker()), 1f);
								plotForTransition.addPointOfInterest("duration: "+MessageFormat.format("{0,number,#.##}",rs.duration), rs.duration, (outlier?Color.RED:Color.GREEN.darker()), 1f);
							}
							plotForTransition.revalidate();
							plotForTransition.repaint();
							plotForLikelihood.revalidate();
							plotForLikelihood.repaint();
						}
					}
				}
			}
		});
		
		myPanel.add(graphPanel, BorderLayout.CENTER);
		myPanel.revalidate();
		myPanel.repaint();
	}

	private ReplayStep getReplayStepForTransition(TimedTransition tt) {
		ReplayStep step = null;
		if (selectedCaseStatistics != null){
			for (ReplayStep rs : selectedCaseStatistics.getReplaySteps()){
				if (transitionsByName.containsKey(rs.transitionName)){
					TimedTransition stepTransition = transitionsByName.get(rs.transitionName);
					if (stepTransition.equals(tt)){
						step = rs;
					}
				}
			}
		}
		return step;
	}

	protected void updateTableSelection(int index) {
		// highlight path in model:
		updateMap(index);
		
	}

	private Map<String, TimedTransition> getTransitionsByName(Collection<Transition> transitions) {
		Map<String, TimedTransition> transitionsByName = new HashMap<String, TimedTransition>();
		for (Transition t : transitions){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				if (!tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
					transitionsByName.put(tt.getLabel(), tt);
				}
			}
		}
		return transitionsByName;
	}

	protected void updateList(double outlierRate) {
		if (outlierRate < 0 || outlierRate >= 1){
			JOptionPane.showMessageDialog(myPanel, "Please choose values between 0.0 and 1.0 for the outlier rate!");
		} else {
			this.outlierRate = outlierRate;
			
			initList();
		}
	}
	
	private void initList() {
		logLikelihoodCutoffs = new HashMap<Transition, Double>();
		for (Transition t : stochasticNet.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
					// ignore immediate transitions
				} else {
					double[] loglikelihoods = null;
					if (logLikelihoodDistributions.containsKey(tt)){
						// we already have sampled from the distribution, just recompute the cutoff
						loglikelihoods = new double[SAMPLE_SIZE];
						List<Double> oldSampleList = logLikelihoodDistributions.get(tt).getValues();
						for (int i = 0; i < oldSampleList.size(); i++){
							loglikelihoods[i] = oldSampleList.get(i);
						}
					} else {
						RealDistribution d = tt.getDistribution();
						double[] samples = d.sample(SAMPLE_SIZE);
						loglikelihoods = new double[samples.length];
						for (int i = 0; i < samples.length; i++){
							loglikelihoods[i] = Math.log(d.density(samples[i]));
						}
						GaussianKernelDistribution logLikelihoodDistribution = new GaussianKernelDistribution();
						logLikelihoodDistribution.addValues(loglikelihoods);
						this.logLikelihoodDistributions.put(tt, logLikelihoodDistribution);
					}
					
					// threshold is based on probability (we can use the value of the i-th entry in an ordered set
					// i is the ratio determined by i/SAMPLE_SIZE = outlierRate 
					double index = outlierRate * SAMPLE_SIZE;
					double remainder = index-Math.floor(index);
					
					Arrays.sort(loglikelihoods);
					
					double logLikelihoodAtCutoff = (1-remainder) * loglikelihoods[(int)Math.ceil(index)]
					                              + (remainder) * loglikelihoods[(int)Math.floor(index)];
					logLikelihoodCutoffs.put(tt, logLikelihoodAtCutoff);
				}
			}
		}
		
		
		numberOfIndividualOutliers = new HashMap<CaseStatistics, List<TimedTransition>>();
		for (CaseStatistics cs : caseStatistics){
			List<TimedTransition> outlierTransitions = new ArrayList<TimedTransition>();
			for (ReplayStep step : cs.getReplaySteps()){
				TimedTransition tt = transitionsByName.get(step.transitionName);
				if (tt != null){
					double logLikelihoodOfActivity = Math.log(step.density);
					if (logLikelihoodOfActivity < logLikelihoodCutoffs.get(tt)){
						// outlier
						outlierTransitions.add(tt);
					}
				}
			}
			numberOfIndividualOutliers.put(cs, outlierTransitions);
		}
		// order cases by number of outliers first and then rank them in this group by overall likelihood
		Collections.sort(caseStatistics, new CaseComparator(numberOfIndividualOutliers));
		
		if (this.caseList!=null){
			this.caseList.setModel(new OutlierTableModel(this.caseStatistics, numberOfIndividualOutliers));
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(updateButton)){
			updateList(Double.parseDouble(outlierPercentageField.getText()));
		}
	}
	
	/**
	 * 
	 * Compares cases by individual outliers and ranks them by their loglikelihood. 
	 * 
	 * @author Andreas Rogge-Solti
	 *
	 */
	private class CaseComparator implements Comparator<CaseStatistics>{

		private Map<CaseStatistics, List<TimedTransition>> numberOfIndividualOutliers;

		public CaseComparator(Map<CaseStatistics, List<TimedTransition>> individualOutliers){
			this.numberOfIndividualOutliers = individualOutliers;
		}
		
		public int compare(CaseStatistics o1, CaseStatistics o2) {
			int outlierCount1 = 0;
			int outlierCount2 = 0;
			if (numberOfIndividualOutliers.containsKey(o1)){
				outlierCount1 = numberOfIndividualOutliers.get(o1).size();
			}
			if (numberOfIndividualOutliers.containsKey(o2)){
				outlierCount2 = numberOfIndividualOutliers.get(o2).size();
			}
			if (outlierCount1 != outlierCount2){
				return outlierCount2-outlierCount1;
			}
			// compare by loglikelihood to rank them:
			return o1.getLogLikelihood().compareTo(o2.getLogLikelihood());
		}
	}
	
	private class OutlierTableModel extends AbstractTableModel{
		private static final long serialVersionUID = 7908497541291624619L;
		
		private CaseStatisticsList caseStatisticList;
		private Map<CaseStatistics, List<TimedTransition>> outlierCount;
		private int maxActivityCount;

		public OutlierTableModel(CaseStatisticsList likelihoods, Map<CaseStatistics, List<TimedTransition>> outliers){
			this.caseStatisticList = likelihoods;
			this.outlierCount = outliers;
			maxActivityCount = getMaxActivityCount(); 
		}
		
		private int getMaxActivityCount() {
			int maxActivities = 0;
			for (CaseStatistics cs : caseStatisticList){
				maxActivities = Math.max(maxActivities, cs.getReplaySteps().size());
			}
			return maxActivities;
		}

		public int getColumnCount() {
			return maxActivityCount+3;
		}

		public int getRowCount() {
			return caseStatisticList.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			CaseStatistics cs = caseStatisticList.get(rowIndex);
			int outliers  = outlierCount.get(cs).size();
			if (columnIndex == 0)
				return cs.getCaseId();
			if (columnIndex == 1){
				return outliers;
			}
			if (columnIndex == 2){
				return cs.getLogLikelihood(); 
			}
			int stepIndex = columnIndex-3;
			if (cs.getReplaySteps().size()>stepIndex){
				return cs.getReplaySteps().get(stepIndex).toString();
			} else {
				return "";
			}
		}

		public String getColumnName(int column) {
			switch(column){
				case 0:
					return "Case Id";
				case 1: 
					return "Exceptions";
				case 2:
					return "log-Likelihood";
				default:
					return "activity "+(column - 2);
			}
		}
	}
}
