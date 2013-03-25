package org.processmining.plugins.stochasticpetrinet.simulator;

import java.util.Arrays;

import javax.swing.JComboBox;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * A configuration window containing properties for the configuration
 * of the simple simulator {@link PNSimulator}.
 * 
 * @author Andreas Rogge-Solti
 * 
 */
public class PNSimulatorConfigUI {

	private class ConfigPanel extends ProMPropertiesPanel {
		private static final long serialVersionUID = 960834061791545027L;

		private PNSimulatorConfig config;

		private ProMTextField nameField;

		private ProMTextField numberOfTracesField;

		private ProMTextField maxEventsInOneTraceField;

		private ProMTextField arrivalRateField;

		//		private JTextField startDateField;

		private JComboBox timeUnitSelection;

		public ConfigPanel() {
			super("Configure simple simulation settings.");
			config = new PNSimulatorConfig();
			initGUI();
		}

		private void initGUI() {
			nameField = this.addTextField("name of the log:", config.logName);

			numberOfTracesField = this.addTextField("number of desired traces:", String.valueOf(config.numberOfTraces));

			maxEventsInOneTraceField = this.addTextField("maximum number of events / trace:", String
					.valueOf(config.maxEventsInOneTrace));

			arrivalRateField = this.addTextField("arrival rate of new instances:", String.valueOf(config.arrivalRate));

			timeUnitSelection = this.addComboBox("select time unit for time-axis:", StochasticNetUtils.UNIT_NAMES);
			timeUnitSelection.setSelectedIndex(Arrays.binarySearch(StochasticNetUtils.UNIT_CONVERSION_FACTORS,
					config.arrivalRate));
		}

		public PNSimulatorConfig getConfig() {
			updateConfig();
			return config;
		}

		private void updateConfig() {
			config.logName = nameField.getText();
			config.numberOfTraces = Integer.valueOf(numberOfTracesField.getText());
			config.maxEventsInOneTrace = Integer.valueOf(maxEventsInOneTraceField.getText());
			config.arrivalRate = Double.valueOf(arrivalRateField.getText());
			config.unitFactor = StochasticNetUtils.UNIT_CONVERSION_FACTORS[timeUnitSelection.getSelectedIndex()];
		}
	}

	public PNSimulatorConfig getConfig(UIPluginContext context) {
		ConfigPanel panel = new ConfigPanel();
		InteractionResult result = context.showConfiguration("Simple Simulation Parameters", panel);
		switch (result) {
			case CANCEL :
				context.getFutureResult(0).cancel(true);
				return null;
			default :
				return panel.getConfig();
		}
	}
}
