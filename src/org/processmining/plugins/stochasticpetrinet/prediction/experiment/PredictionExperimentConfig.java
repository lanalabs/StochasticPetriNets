package org.processmining.plugins.stochasticpetrinet.prediction.experiment;

import java.awt.Dimension;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import com.fluxicon.slickerbox.components.RoundedPanel;

public class PredictionExperimentConfig {

	private int monitoringIterations = 20;
	private int simulatedTraces = 1000;
	private double timeUnitFactor = 1000;
	
	private boolean learnSPNFromData = true;
	private DistributionType learnedDistributionType = DistributionType.GAUSSIAN_KERNEL;
	private ExecutionPolicy executionPolicy = ExecutionPolicy.GLOBAL_PRESELECTION;
	
	private String resultFileName = "predictionResults.csv";
	private int workerCount = 4;
	
	private static int TEXTWIDTH = 500;
	
	private static final Integer[] WORKER_COUNTS = new Integer[]{1,2,4,8,16};


	public boolean letUserChooseValues(UIPluginContext context){
		PredictionExperimentPanel panel = new PredictionExperimentPanel(this);
		boolean chooseValues = letUserChooseValues(context, panel);
		return chooseValues;
	}
	
	protected boolean letUserChooseValues(UIPluginContext context, PredictionExperimentPanel panel){
		InteractionResult result = context.showConfiguration("Select Options for the repair of the log", panel);
		if (result.equals(InteractionResult.CANCEL)){
			return false;
		} else {
			getValuesFromPanel(panel);
			return true;
		}
	}
	
	protected void getValuesFromPanel(PredictionExperimentPanel panel) {
		monitoringIterations = panel.getMonitoringIterations();
		simulatedTraces = panel.getSimulatedTracesCount();
		timeUnitFactor = panel.getTimeUnitFactor();
		resultFileName = panel.getResultFileName();
		learnSPNFromData = panel.getLearnSPNFromData();
		learnedDistributionType = panel.getLearnedDistributionType();
		executionPolicy = panel.getExecutionPolicy();
		workerCount = panel.getWorkerCount();
	}
	
	private class PredictionExperimentPanel extends ProMPropertiesPanel {
		private static final long serialVersionUID = -2364906970481450404L;

		private ProMTextField simulatedTracesField;
		private ProMTextField monitoringIterationsField;
		private JCheckBox learnSPNFromDataBox;
		private JComboBox learnedDistributionTypeBox;
		private JComboBox learnedExecutionPolicyBox;
		private JComboBox unitFactorBox;
		private ProMTextField fileNameField;
		private JComboBox workerCountBox;
		
		PredictionExperimentConfig config;
		
		public PredictionExperimentPanel(PredictionExperimentConfig config) {
			super("Configuration for prediction experiment");
			this.config = config;
			this.init();
		}

		protected void init() {
			simulatedTracesField = addTextField("Number of simulated traces:",String.valueOf(config.getSimulatedTraces()));
			monitoringIterationsField = addTextField("Number of Monitoring iterations:", String.valueOf(config.getMonitoringIterations()));
			
			unitFactorBox = this.addComboBox("Time unit stored in the stochastic model:", StochasticNetUtils.UNIT_NAMES);
			unitFactorBox.setSelectedIndex(Arrays.binarySearch(StochasticNetUtils.UNIT_CONVERSION_FACTORS, config.getTimeUnitFactor()));
			
			fileNameField = addTextField("File name to store experiment results:",String.valueOf(config.getResultFileName()));
			learnSPNFromDataBox = addCheckBox("Learn stochastic Net properties from data?", config.getLearnSPNFromData());
			
			DistributionType[] supportedTypes = new DistributionType[]{DistributionType.NORMAL,DistributionType.EXPONENTIAL,DistributionType.GAUSSIAN_KERNEL,DistributionType.HISTOGRAM};
			if (StochasticNetUtils.splinesSupported()){
				supportedTypes = Arrays.copyOf(supportedTypes, supportedTypes.length+1);
				supportedTypes[supportedTypes.length-2] = DistributionType.LOGSPLINE;
			} else {
				add(new JLabel("To enable spline smoothers, make sure you have a running R installation \n" +
						"and the native jri-binary is accessible in your java.library.path!"));
			}
			
			learnedDistributionTypeBox = addComboBox("Learned distribution type for SPN:", supportedTypes);
			learnedDistributionTypeBox.setSelectedIndex(0);
			
			learnedExecutionPolicyBox = addComboBox("Assumed execution policy of the SPN:", ExecutionPolicy.values());
			learnedExecutionPolicyBox.setSelectedIndex(0);
			
			workerCountBox = this.addComboBox("Parallel Workers:", WORKER_COUNTS);
			workerCountBox.setSelectedIndex(Arrays.binarySearch(WORKER_COUNTS, config.getWorkerCount()));
		}
		
		public int getSimulatedTracesCount(){
			return Integer.valueOf(simulatedTracesField.getText());
		}
		
		public int getMonitoringIterations(){
			return Integer.valueOf(monitoringIterationsField.getText());
		}
		public boolean getLearnSPNFromData() {
			return learnSPNFromDataBox.isSelected();
		}
		public DistributionType getLearnedDistributionType(){
			return (DistributionType) learnedDistributionTypeBox.getSelectedItem();
		}
		public ExecutionPolicy getExecutionPolicy(){
			return (ExecutionPolicy) learnedExecutionPolicyBox.getSelectedItem();
		}
		public double getTimeUnitFactor(){
			return StochasticNetUtils.UNIT_CONVERSION_FACTORS[unitFactorBox.getSelectedIndex()];
		}

		public int getWorkerCount() {
			return WORKER_COUNTS[workerCountBox.getSelectedIndex()];
		}
		public String getResultFileName(){
			return fileNameField.getText();
		}
		protected RoundedPanel packInfo(String name, JComponent component) {
			RoundedPanel panel = super.packInfo(name, component);
			panel.getComponent(1).setPreferredSize(new Dimension(TEXTWIDTH,panel.getComponent(1).getPreferredSize().height));
			panel.getComponent(1).setMaximumSize(panel.getComponent(1).getPreferredSize());
			panel.getComponent(3).setPreferredSize(new Dimension(800-TEXTWIDTH,panel.getComponent(3).getPreferredSize().height));
			return panel;
		}
	}

	public int getMonitoringIterations() {
		return monitoringIterations;
	}
	
	public String getResultFileName() {
		return resultFileName;
	}

	public int getSimulatedTraces() {
		return simulatedTraces;
	}
	public boolean getLearnSPNFromData() {
		return learnSPNFromData;
	}
	public DistributionType getLearnedDistributionType(){
		return learnedDistributionType;
	}
	public ExecutionPolicy getExecutionPolicy(){
		return executionPolicy;
	}
	public double getTimeUnitFactor() {
		return timeUnitFactor;
	}

	public int getWorkerCount() {
		return workerCount ;
	}
}
