package org.processmining.plugins.stochasticpetrinet.ui;

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
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMJGraphVisualizer;
import org.processmining.models.jgraph.elements.ProMGraphCell;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.analyzer.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutlierVisualizer implements ActionListener {

    private CaseStatisticsAnalyzer analyzer;
    private CaseStatistics selectedCaseStatistics;

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
    private PluginContext context;

//	private Map<String,TimedTransition> transitionsByName;

    @Plugin(name = "Outlier Visualizer", returnLabels = {"Outlier Exporer"}, returnTypes = {JComponent.class}, parameterLabels = {CaseStatisticsList.PARAMETER_LABEL}, userAccessible = false)
    @Visualizer
    public JComponent visualize(PluginContext context, CaseStatisticsList likelihoodList) {
        this.context = context;
        try {
            CaseStatisticsConnection connection = context.getConnectionManager().getFirstConnection(CaseStatisticsConnection.class, context, likelihoodList);
            if (connection != null) {
                StochasticNet stochasticNet = connection.getObjectWithRole(CaseStatisticsConnection.STOCHASTIC_NET);

                InitialMarkingConnection conn;
                Marking m = new Marking();
                try {
                    conn = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, stochasticNet);
                    m = conn.getObjectWithRole(InitialMarkingConnection.MARKING);
                } catch (ConnectionCannotBeObtained e1) {
                    e1.printStackTrace();
                }

                this.analyzer = new CaseStatisticsAnalyzer(stochasticNet, m, likelihoodList);
            } else {
                return new JLabel("Cannot find connection to stochastic net of the statistics");
            }
        } catch (ConnectionCannotBeObtained e) {
            return new JLabel(e.getMessage());
        }

//		transitionsByName = getTransitionsByName(stochasticNet.getTransitions());

        plotForTransition = new PlotPanelFreeChart();
        plotForTransition.setPreferredSize(new Dimension(300, 150));
        plotForLikelihood = new PlotPanelFreeChart();
        plotForLikelihood.setPreferredSize(plotForTransition.getPreferredSize());
        JPanel distributionPanel = new JPanel();
        distributionPanel.setLayout(new GridLayout(2, 1));
        distributionPanel.add(plotForTransition);
        distributionPanel.add(plotForLikelihood);


        myPanel = new JPanel();
        myPanel.setLayout(new BorderLayout());

        updateMap(-1);

        myPanel.add(distributionPanel, BorderLayout.EAST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout());
        JPanel headerPanel = new JPanel();
        headerPanel.add(new JLabel("enter outlier rate (e.g., to find 1 percent: 0.01)"));
        outlierPercentageField = new JTextField(String.valueOf(analyzer.getOutlierRate()));
        updateButton = new JButton("update");
        updateButton.addActionListener(this);
        headerPanel.add(outlierPercentageField);
        headerPanel.add(updateButton);
        infoPanel.add(headerPanel, BorderLayout.NORTH);

        // create table with cases:
        TableModel dataModel = new OutlierTableModel(analyzer);
        this.caseList = new JTable(dataModel);
        this.caseList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    System.out.println("selection event: first index = " + event.getFirstIndex() + ", last index = " + event.getLastIndex() + " is adjusting: " + event.getValueIsAdjusting());
                    updateTableSelection(caseList.getSelectedRow());
                }
            }
        });
        JScrollPane scrollpane = new JScrollPane(this.caseList);
        infoPanel.add(scrollpane, BorderLayout.CENTER);
        infoPanel.setPreferredSize(new Dimension(800, 300));

        myPanel.add(infoPanel, BorderLayout.SOUTH);

        return myPanel;
    }

    /**
     * Shows the graph
     *
     * @param context       {@link PluginContext} from which to retrieve connections to the petri net.
     * @param selectedIndex the index of the case in the {@link #caseStatistics} or -1, if no case is selected
     */
    private void updateMap(int selectedIndex) {
        ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();

        List<ReplayStep> outlierSteps = new ArrayList<ReplayStep>();
        List<ReplayStep> regularSteps = new ArrayList<ReplayStep>();
        selectedCaseStatistics = null;
        if (selectedIndex >= 0) {
            selectedCaseStatistics = analyzer.getCaseStatistics().get(selectedIndex);
            outlierSteps = analyzer.getIndividualOutlierSteps(selectedCaseStatistics);
            regularSteps = analyzer.getRegularSteps(selectedCaseStatistics);
        }
        Marking m = analyzer.getInitialMarking();

        // add initial marking labels
        for (Place p : m) {
            String label = "" + m.occurrences(p);
            map.putViewSpecific(p, AttributeMap.LABEL, label);
            map.putViewSpecific(p, AttributeMap.TOOLTIP, p.getLabel());
            map.putViewSpecific(p, AttributeMap.SHOWLABEL, !label.equals(""));
        }
        for (Transition t : analyzer.getStochasticNet().getTransitions()) {
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                ReplayStep step = getReplayStepForTransition(tt);

                if (outlierSteps.contains(step)) {
                    if (analyzer.isOutlierLikelyToBeAnError(step)) {
                        map.putViewSpecific(t, AttributeMap.FILLCOLOR, Color.RED);
                    } else {
                        map.putViewSpecific(t, AttributeMap.FILLCOLOR, Color.ORANGE);
                    }
                } else if (regularSteps.contains(step)) {
                    map.putViewSpecific(t, AttributeMap.FILLCOLOR, Color.GREEN);
                }
                map.putViewSpecific(t, AttributeMap.LABEL, t.getLabel() + " (" + tt.getWeight() + ")");

                String distributionParameters = Arrays.toString(tt.getDistributionParameters());
                distributionParameters = distributionParameters.substring(0, Math.min(50, distributionParameters.length() - 1)) + "...";
                map.putViewSpecific(t, AttributeMap.TOOLTIP, "<html>" + t.getLabel() + "\n<br>" +
                        (step == null ? "" : "duration: " + step.duration + "<br>\n" +
                                "density: " + step.density + "<br>\n" +
                                "loglikelihood: " + Math.log(step.density) + "<br>\n") +
                        "threshold: " + analyzer.getLogLikelihoodCutoff(tt) + "<br>\n" +
                        "priority: " + tt.getPriority() + "\n<br>" +
                        "weight: " + tt.getWeight() + "\n<br>" +
                        "type: " + tt.getDistributionType().toString() + "\n<br>" +
                        "parameters: " + distributionParameters + "</html>");
            } else {
                map.putViewSpecific(t, AttributeMap.TOOLTIP, t.getLabel());
            }
        }

        if (graphPanel != null) {
            myPanel.remove(graphPanel);
        }
        graphPanel = ProMJGraphVisualizer.instance().visualizeGraph(context, analyzer.getStochasticNet(), map);

        graphPanel.getGraph().addGraphSelectionListener(new GraphSelectionListener() {
            public void valueChanged(GraphSelectionEvent e) {
                for (Object selectedCell : e.getCells()) {
                    if (e.isAddedCell(selectedCell) && selectedCell instanceof ProMGraphCell) {
                        DirectedGraphNode node = ((ProMGraphCell) selectedCell).getNode();
                        if (node instanceof TimedTransition) {

                            TimedTransition transition = (TimedTransition) node;

                            // update graph plots
                            ReplayStep rs = getReplayStepForTransition(transition);

                            List<Plot> plots = new ArrayList<Plot>();
                            Plot plot = new Plot("duration of " + transition.getLabel());
                            plot.add(transition.getDistribution());
                            plots.add(plot);
                            plotForTransition.setPlots(plots);
                            plotForTransition.clearPointsOfInterest();

                            List<Plot> likelihoodPlots = new ArrayList<Plot>();
                            Plot likelihoodPlot = new Plot("log-Likelihood of " + transition.getLabel());
                            likelihoodPlot.add(analyzer.getLogLikelihoodDistribution(transition));
                            likelihoodPlots.add(likelihoodPlot);
                            plotForLikelihood.setPlots(likelihoodPlots);
                            plotForLikelihood.clearPointsOfInterest();

                            double cutOff = analyzer.getLogLikelihoodCutoff(transition);
                            plotForLikelihood.addPointOfInterest(MessageFormat.format("{0,number,#.##%}", analyzer.getOutlierRate()) + "-cutoff", cutOff, Color.BLUE, 1f);

                            if (rs != null) {
                                double value = Math.log(rs.density);
                                boolean outlier = value < cutOff;
                                plotForLikelihood.addPointOfInterest(MessageFormat.format("{0,number,#.##}", value), value, (outlier ? Color.RED : Color.GREEN.darker()), 1f);
                                plotForTransition.addPointOfInterest("duration: " + MessageFormat.format("{0,number,#.##}", rs.duration), rs.duration, (outlier ? Color.RED : Color.GREEN.darker()), 1f);
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
        if (selectedCaseStatistics != null) {
            for (ReplayStep rs : selectedCaseStatistics.getReplaySteps()) {
                if (tt.equals(rs.transition)) {
                    step = rs;
                }
            }
        }
        return step;
    }

    protected void updateTableSelection(int index) {
        // highlight path in model:
        updateMap(index);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(updateButton)) {
            try {
                analyzer.updateStatistics(Double.parseDouble(outlierPercentageField.getText()));
                if (this.caseList != null) {
                    this.caseList.setModel(new OutlierTableModel(analyzer));
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(myPanel, "Cannot parse " + outlierPercentageField.getText() + "\n" +
                        "Please choose values between 0.0 and 1.0 for the outlier rate!");
            } catch (IllegalArgumentException iae) {
                JOptionPane.showMessageDialog(myPanel, iae.getMessage());
            }
        }
    }

    private class OutlierTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 7908497541291624619L;

        private int maxActivityCount;

        private CaseStatisticsAnalyzer analyzer;

        public OutlierTableModel(CaseStatisticsAnalyzer analyzer) {
            this.analyzer = analyzer;
            maxActivityCount = analyzer.getMaxActivityCount();
        }

        public int getColumnCount() {
            return maxActivityCount + 3;
        }

        public int getRowCount() {
            return this.analyzer.getCaseStatistics().size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            CaseStatistics cs = this.analyzer.getCaseStatistics().get(rowIndex);
            if (columnIndex == 0)
                return cs.getCaseId();
            if (columnIndex == 1) {
                return analyzer.getOutlierCount(cs);
            }
            if (columnIndex == 2) {
                return cs.getLogLikelihood();
            }
            int stepIndex = columnIndex - 3;
            if (cs.getReplaySteps().size() > stepIndex) {
                return cs.getReplaySteps().get(stepIndex).toString();
            } else {
                return "";
            }
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Case Id";
                case 1:
                    return "Exceptions";
                case 2:
                    return "log-Likelihood";
                default:
                    return "activity " + (column - 2);
            }
        }
    }
}
