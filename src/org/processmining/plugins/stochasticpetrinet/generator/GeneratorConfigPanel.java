package org.processmining.plugins.stochasticpetrinet.generator;

import com.fluxicon.slickerbox.components.NiceSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GeneratorConfigPanel {
    private class ConfigPanel extends ProMPropertiesPanel {
        private static final long serialVersionUID = 960834061791545027L;

        private GeneratorConfig config;

        private ProMTextField nameField;
        private ProMTextField numberOfTransitionsField;

        private NiceSlider degreeOfParalellismSlider;
        private NiceSlider degreeOfExclusivitySlider;
        private NiceSlider degreeOfSequenceSlider;
        private NiceSlider degreeOfLoopsSlider;

        private JCheckBox containsLoops;
        private JCheckBox addInitTransition;

        @SuppressWarnings("rawtypes")
        private JComboBox distributionTypeComboBox;

        public ConfigPanel() {
            super("Configure simple simulation settings.");
            config = new GeneratorConfig();
            initGUI();
        }

        private void initGUI() {
            nameField = this.addTextField("name of the net:", config.getName());

            numberOfTransitionsField = this.addTextField("number of transitions:", String.valueOf(config.getTransitionSize()));

            degreeOfParalellismSlider = SlickerFactory.instance().createNiceIntegerSlider("parallelism", 0, 100, 20, Orientation.HORIZONTAL);
            this.add(degreeOfParalellismSlider);
            degreeOfExclusivitySlider = SlickerFactory.instance().createNiceIntegerSlider("exclusiveness", 0, 100, 20, Orientation.HORIZONTAL);
            this.add(degreeOfExclusivitySlider);
            degreeOfSequenceSlider = SlickerFactory.instance().createNiceIntegerSlider("sequential", 0, 100, 60, Orientation.HORIZONTAL);
            this.add(degreeOfSequenceSlider);

            containsLoops = this.addCheckBox("add loops:", config.isContainsLoops());
            containsLoops.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource().equals(containsLoops)) {
                        degreeOfLoopsSlider.setEnabled(containsLoops.isSelected());
                    }
                }
            });
            addInitTransition = this.addCheckBox("dedicated immediate init transition?", config.isCreateDedicatedImmediateStartTransition());

            degreeOfLoopsSlider = SlickerFactory.instance().createNiceIntegerSlider("loops", 0, 100, 0, Orientation.HORIZONTAL);
            degreeOfLoopsSlider.setEnabled(containsLoops.isSelected());
            this.add(degreeOfLoopsSlider);

            distributionTypeComboBox = this.addComboBox("distribution type:", DistributionType.values());
        }

        public GeneratorConfig getConfig() {
            updateConfig();
            return config;
        }

        private void updateConfig() {
            config.setName(nameField.getText());
            config.setTransitionSize(Integer.valueOf(numberOfTransitionsField.getText()));
            config.setContainsLoops(containsLoops.isSelected());
            config.setCreateDedicatedImmediateStartTransition(addInitTransition.isSelected());
            config.setDegreeOfParallelism(degreeOfParalellismSlider.getSlider().getValue());
            config.setDegreeOfExclusiveChoices(degreeOfExclusivitySlider.getSlider().getValue());
            config.setDegreeOfSequences(degreeOfSequenceSlider.getSlider().getValue());
            config.setDegreeOfLoops(degreeOfLoopsSlider.getSlider().getValue());
            config.setDistributionType((DistributionType) distributionTypeComboBox.getSelectedItem());
        }
    }

    public GeneratorConfig getConfig(UIPluginContext context) {
        ConfigPanel panel = new ConfigPanel();
        InteractionResult result = context.showConfiguration("PN-Generation Parameters", panel);
        while (true) {
            switch (result) {
                case CANCEL:
                    context.getFutureResult(0).cancel(true);
                    return null;
                default:
                    try {
                        return panel.getConfig();
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(panel, e.getMessage());
                    }
            }
        }
    }
}
