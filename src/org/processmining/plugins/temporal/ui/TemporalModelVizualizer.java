package org.processmining.plugins.temporal.ui;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.temporal.model.TemporalModel;

import javax.swing.*;
import java.awt.*;

public class TemporalModelVizualizer {

    @Plugin(name = "Temporal Model Visualizer", returnLabels = {"Visualized Temporal Model"}, returnTypes = {JComponent.class}, parameterLabels = {TemporalModel.PARAMETER_LABEL}, userAccessible = false)
    @Visualizer
    public static JComponent visualize(PluginContext context, TemporalModel model) {

        JComponent component = new JPanel();
        TemporalModelPanel panel = new TemporalModelPanel(model);

        component.setLayout(new BorderLayout());

        component.add(new VisualControls(panel), BorderLayout.NORTH);
        component.add(panel, BorderLayout.CENTER);

        return component;
    }
}
