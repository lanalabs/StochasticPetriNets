package org.processmining.plugins.stochasticpetrinet.analyzer.anomaly;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import weka.core.json.JSONNode;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;

@Plugin(name = ".json export of anomalies", returnLabels = {}, returnTypes = {}, parameterLabels = {StochasticNet.PARAMETER_LABEL, "File", "anomaly rate"}, userAccessible = true)
@UIExportPlugin(description = "Temporal anomaly regions of all transitions in a net", extension = "json")
public class AnomalousIntervalsComputerPlugin {

    @PluginVariant(variantLabel = ".json export of anomaly regions", requiredParameterLabels = {0, 1})
    public void exportAnomaliesToJSON(PluginContext context, AnomalyIntervals intervals, File file) throws Exception {
//		AnomalousIntervalsComputer anomalyComputer = new AnomalousIntervalsComputer();
//		
//		Double anomalyThreshold = null;
//	
//		while (anomalyThreshold == null){
//			try {
//				String input = JOptionPane.showInputDialog(null,"Please enter an anomaly rate to define the intervals.", "Anomaly Rate", JOptionPane.PLAIN_MESSAGE);
//			
//				if (input == null){
//					return;
//				} else {
//					anomalyThreshold = Double.valueOf(input);
//					if (anomalyThreshold <= 0 || anomalyThreshold >= 1){
//						throw new NumberFormatException();
//					}
//				}
//			} catch (NumberFormatException nfe){
//				JOptionPane.showMessageDialog(null, "You need to input a double value between 0 and 1 for the anomaly rate.\n"
//						+ "A typical value is 0.05 declaring 5 percent of the cases as anomalies.");
//			}
//		}
////		AnomalyRateSelector anomalyRateSelector = new AnomalyRateSelector();
//		
////		Thread t = new Thread(anomalyRateSelector);
////		t.start();
////		
////		t.join();
////		if (anomalyRateSelector.aborted){
////			return;
////		}
////		anomalyThreshold = anomalyRateSelector.anomalyThreshold;

//		Map<Transition, List<Pair<Double,Double>>> anomalylists = anomalyComputer.getAnomalousIntervals(null, net, anomalyThreshold);

        String jsonString = getJSONForAnomalies(intervals.getIntervals(), intervals.getNetLabel(), intervals.getAnomalyRate());

        if (file.canWrite()) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(jsonString);
            writer.flush();
            writer.close();
        }
    }

    /**
     * Returns a JSON-Object containing the anomalous regions supplied as parameter
     *
     * @param anomalyList Map associating Pairs of Doubles (anomalous intervals) to a transition
     * @param name        the name of the model that contains the transitions
     * @return String formatted as JSON
     */
    public String getJSONForAnomalies(Map<Transition, List<Pair<Double, Double>>> anomalyList, String name, Double anomalyRate) {
        JSONNode node = new JSONNode();

        node.addPrimitive("model", name);
        node.addPrimitive("anomalyRate", anomalyRate);

        JSONNode anomalies = node.addArray("temporalAnomalies");

        for (Transition t : anomalyList.keySet()) {
            JSONNode transitionNode = anomalies.addArrayElement(new Object());
            transitionNode.addPrimitive("transition", t.getLabel());

            JSONNode anomaliesArray = transitionNode.addArray("anomalousIntervals");
            for (Pair<Double, Double> anomalyPair : anomalyList.get(t)) {
                JSONNode anomalyNode = anomaliesArray.addArrayElement(new Object());
                anomalyNode.addPrimitive("from", anomalyPair.getFirst());
                anomalyNode.addPrimitive("to", anomalyPair.getSecond());
            }
        }
        StringBuffer buffer = new StringBuffer();
        node.toString(buffer);
        return buffer.toString();
    }

    @Plugin(name = "Compute Anomaly Intervals for Net",
            parameterLabels = {StochasticNet.PARAMETER_LABEL},
            returnLabels = {"Anomaly Intervals"},
            returnTypes = {AnomalyIntervals.class},
            userAccessible = true,
            help = "Computes anomaly intervals for a stochastic Petri net model.")

    @UITopiaVariant(affiliation = "WU Vienna", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
    public AnomalyIntervals computeAnomalyIntervals(UIPluginContext context, StochasticNet net) {
        AnomalousIntervalsComputer anomalyComputer = new AnomalousIntervalsComputer();

        Double anomalyThreshold = null;

        while (anomalyThreshold == null) {
            try {
                String input = JOptionPane.showInputDialog(null, "Please enter an anomaly rate to define the intervals.", "Anomaly Rate", JOptionPane.PLAIN_MESSAGE);

                if (input == null) {
                    if (context != null) {
                        context.getFutureResult(0).cancel(true);
                    }
                    return null;
                } else {
                    anomalyThreshold = Double.valueOf(input);
                    if (anomalyThreshold <= 0 || anomalyThreshold >= 1) {
                        throw new NumberFormatException();
                    }
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(null, "You need to input a double value between 0 and 1 for the anomaly rate.\n"
                        + "A typical value is 0.05 declaring 5 percent of the cases as anomalies.");
            }
        }

        Map<Transition, List<Pair<Double, Double>>> anomalyLists = anomalyComputer.getAnomalousIntervals(null, net, anomalyThreshold);

        AnomalyIntervals intervals = new AnomalyIntervals(anomalyLists, net.getLabel(), anomalyThreshold);
        return intervals;
    }


//	class AnomalyRateSelector implements Runnable {
//
//		private Double anomalyThreshold = null;
//		private boolean aborted = false;
//		
//		public void run() {
//			while (anomalyThreshold == null && !aborted){
//				try {
//					// This easy tweak worked for me (1.6+). Replaced the showXXXDialog with four lines of code to: 
//					//(1) create a JOptionPane object 
//					//(2) call its createDialog() method to get a JDialog object 
//					// (3) set the modality type of the JDialog object to modeless 
//					// (4) set the visibility of the JDialog to true
//					
//					JOptionPane pane = new JOptionPane("Select anomaly rate:", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
//					pane.setWantsInput(true);
//					JDialog dialog = pane.createDialog("Anomaly rate");
//					dialog.setModalityType(ModalityType.MODELESS);
//					dialog.setVisible(true);
//					
//					Object selectedValue = pane.getInputValue();
//					
//					String input = selectedValue.toString();
//					
////					String input = JOptionPane.showInputDialog(null,"Please enter an anomaly rate to define the intervals.", "Anomaly Rate", JOptionPane.PLAIN_MESSAGE);
//					
//					if (input == null){
//						aborted = true;
//					} else {
//						anomalyThreshold = Double.valueOf(input);
//						if (anomalyThreshold <= 0 || anomalyThreshold >= 1){
//							throw new NumberFormatException();
//						}
//					}
//				} catch (NumberFormatException nfe){
//					JOptionPane.showMessageDialog(null, "You need to input a double value between 0 and 1 for the anomaly rate.\n"
//							+ "A typical value is 0.05 declaring 5 percent of the cases as anomalies.");
//				}
//			}
//		}
//	}
}
