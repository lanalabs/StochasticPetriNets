package org.processmining.plugins.stochasticpetrinet.simulator;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;

import javax.swing.*;

/**
 * A configuration window containing properties for the configuration
 * of the simple simulator {@link PNSimulator}.
 *
 * @author Andreas Rogge-Solti
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

        private JComboBox executionPolicySelection;

        public ConfigPanel(TimeUnit defaultTimeUnit, ExecutionPolicy defaultExecutionPolity) {
            super("Configure simple simulation settings.");
            config = new PNSimulatorConfig();
            config.executionPolicy = defaultExecutionPolity;
            config.unitFactor = defaultTimeUnit;
            initGUI();
        }

        private void initGUI() {
            nameField = this.addTextField("name of the log:", config.logName);

            numberOfTracesField = this.addTextField("number of desired traces:", String.valueOf(config.numberOfTraces));

            maxEventsInOneTraceField = this.addTextField("maximum number of events / trace:", String
                    .valueOf(config.maxEventsInOneTrace));

            arrivalRateField = this.addTextField("arrival rate of new instances:", String.valueOf(config.arrivalRate));

            timeUnitSelection = this.addComboBox("select time unit for time-axis:", TimeUnit.values());
//			timeUnitSelection.setSelectedIndex(Arrays.binarySearch(StochasticNetUtils.UNIT_CONVERSION_FACTORS,
//					config.unitFactor));
            timeUnitSelection.setSelectedItem(config.unitFactor);
            executionPolicySelection = this.addComboBox("select the execution policy:", ExecutionPolicy.values());
            executionPolicySelection.setSelectedItem(config.executionPolicy);
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
            config.unitFactor = (TimeUnit) timeUnitSelection.getSelectedItem();
            config.executionPolicy = (ExecutionPolicy) executionPolicySelection.getSelectedItem();
        }
    }

    private TimeUnit defaultTimeUnit = TimeUnit.MINUTES;
    private ExecutionPolicy defaultExecutionPolity = ExecutionPolicy.RACE_ENABLING_MEMORY;

    public PNSimulatorConfigUI(PetrinetGraph petriNet) {
        if (petriNet instanceof StochasticNet) {
            TimeUnit timeUnit = ((StochasticNet) petriNet).getTimeUnit();
            ExecutionPolicy policy = ((StochasticNet) petriNet).getExecutionPolicy();
            if (timeUnit != null) {
                defaultTimeUnit = timeUnit;
            }
            if (policy != null) {
                defaultExecutionPolity = policy;
            }
        }
    }

    public PNSimulatorConfig getConfig(UIPluginContext context) {
        ConfigPanel panel = new ConfigPanel(defaultTimeUnit, defaultExecutionPolity);
        InteractionResult result = context.showConfiguration("Simple Simulation Parameters", panel);
        switch (result) {
            case CANCEL:
                context.getFutureResult(0).cancel(true);
                return null;
            default:
                return panel.getConfig();
        }
    }
}
