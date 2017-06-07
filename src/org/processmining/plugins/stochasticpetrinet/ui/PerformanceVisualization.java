package org.processmining.plugins.stochasticpetrinet.ui;

import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.elements.ProMGraphCell;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PerformanceVisualization {
    private static PlotPanelFreeChart plotForTransition;

    private static ProMJGraphPanel graphPanel;

    private static JPanel myPanel;


    @Plugin(name = "Stochastic Petri Net Visualizer",
            level = PluginLevel.Regular,
            returnLabels = {"Visualized Stochastic Petri Net"},
            returnTypes = {JComponent.class},
            parameterLabels = {"Stochastic Petri Net"},
            userAccessible = true)
    @Visualizer
    public JComponent visualize(PluginContext context, StochasticNet sNet) {

        myPanel = new JPanel();
        myPanel.setLayout(new BorderLayout());

        plotForTransition = new PlotPanelFreeChart();
        plotForTransition.setPreferredSize(new Dimension(500, 300));

        ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();
        InitialMarkingConnection conn;
        Marking m = new Marking();
        try {
            conn = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, sNet);
            m = conn.getObjectWithRole(InitialMarkingConnection.MARKING);
        } catch (ConnectionCannotBeObtained e1) {
            e1.printStackTrace();
        }

        for (Place p : m) {
            String label = "" + m.occurrences(p);
            map.putViewSpecific(p, AttributeMap.LABEL, label);
            map.putViewSpecific(p, AttributeMap.TOOLTIP, p.getLabel());
            map.putViewSpecific(p, AttributeMap.SHOWLABEL, !label.equals("") && !label.equals("null"));
        }
        for (Transition t : sNet.getTransitions()) {
            map.putViewSpecific(t, AttributeMap.SHOWLABEL, t.getLabel()!=null && !t.getLabel().equals("") && !t.getLabel().equals("null"));
            map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel());
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel() + " (" + tt.getWeight() + ")");
                String parameters = Arrays.toString(tt.getDistributionParameters());
                if (parameters.length() > 101) {
                    parameters = parameters.substring(0, 100) + "..." + "(" + tt.getDistributionParameters().length + " parameters in total)";
                }
                map.putViewSpecific(t, AttributeMap.TOOLTIP, "<html>" + t.getLabel() + "\n<br>" +
                        "priority: " + tt.getPriority() + "\n<br>" +
                        "weight: " + tt.getWeight() + "\n<br>" +
                        "type: " + tt.getDistributionType().toString() + "\n<br>" +
                        "parameters: " + parameters + "</html>");
            } else {
                map.putViewSpecific(t, AttributeMap.TOOLTIP, t.getLabel());
            }
        }

        graphPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, sNet, map);
        graphPanel.getGraph().addGraphSelectionListener(new GraphSelectionListener() {

            public void valueChanged(GraphSelectionEvent e) {
                for (Object selectedCell : e.getCells()) {
                    if (e.isAddedCell(selectedCell) && selectedCell instanceof ProMGraphCell) {
                        DirectedGraphNode node = ((ProMGraphCell) selectedCell).getNode();
                        if (node instanceof TimedTransition) {
                            TimedTransition transition = (TimedTransition) node;
                            List<Plot> plots = new ArrayList<Plot>();
                            Plot plot = new Plot(transition.getLabel());
//							plot.add(new DistributionWrapper(transition.getDistribution()));
                            plot.add(transition.getDistribution());
                            plots.add(plot);
                            plotForTransition.setUnit(((StochasticNet) transition.getGraph()).getTimeUnit().toString());
							plotForTransition.setTrainingData(transition.getTrainingData());
                            plotForTransition.setPlots(plots);
                        } else {
                            plotForTransition.displayMessage("no timing information available for: " + node.getLabel());
                        }
                    }
                }
            }
        });

        myPanel.add(graphPanel, BorderLayout.CENTER);
        myPanel.add(plotForTransition, BorderLayout.SOUTH);

//		.addGraphSelectionListener(new GraphSelectionListener() {
//			public void valueChanged(GraphSelectionEvent e) {
//				// selection of a transition would change the stats
//				if (e.getCell() instanceof ProMGraphCell) {
//					DirectedGraphNode cell = ((ProMGraphCell) e.getCell()).getNode();
//					if (cell instanceof TransitionP) {
//						elStatPanel.setTransition((Transition) mapView2OrigNode.get(cell));
//					} else if (cell instanceof PlaceP) {
//						elStatPanel.setPlace((Place) mapView2OrigNode.get(cell));
//					}
//
//					graph.getModel().beginUpdate();
//					graph.getModel().endUpdate();
//					graph.refresh();
//				}
//			}
//		});
        return myPanel;
    }
}
