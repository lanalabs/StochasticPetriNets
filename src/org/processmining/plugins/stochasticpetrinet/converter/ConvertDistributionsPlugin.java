package org.processmining.plugins.stochasticpetrinet.converter;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * Converts all timed distributions of a {@link StochasticNet} to a given distribution type (except for immediate and deterministic transitions) 
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class ConvertDistributionsPlugin {

		@Plugin(name = "Convert Distributions in stochastic Petri net", 
			parameterLabels = { StochasticNet.PARAMETER_LABEL }, 
			returnLabels = { StochasticNet.PARAMETER_LABEL, "Marking" }, 
			returnTypes = { StochasticNet.class, Marking.class }, 
			userAccessible = true,
			help = "Creates a new copy of the net enriched with performance data.")

	@UITopiaVariant(affiliation = "Vienna University of Economics and Business", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
	public static Object[] convertStochasticNet(PluginContext context, StochasticNet net){
		DistributionType type = (DistributionType) JOptionPane.showInputDialog(new JPanel(),
				"Choose the target distribution type", "Convert Distributions in Net",
				JOptionPane.PLAIN_MESSAGE, null, ToStochasticNet.SUPPORTED_CONVERSION_TYPES, ToStochasticNet.SUPPORTED_CONVERSION_TYPES[0]);
		if (type == null){
			if (context != null){
				context.getFutureResult(0).cancel(true);
				context.getFutureResult(1).cancel(true);
			}
			return null;
		}
		Marking marking = StochasticNetUtils.getInitialMarking(context, net);
		return ToStochasticNet.convertStochasticNetToType(context, net, marking, type);
	}
		
	@Plugin(name = "Remove stochastic information from Petri net", 
				parameterLabels = { StochasticNet.PARAMETER_LABEL }, 
				returnLabels = { "Petri net", "Marking" }, 
				returnTypes = { Petrinet.class, Marking.class }, 
				userAccessible = true,
				help = "Creates a new copy of the net stripped of performance data.")

	@UITopiaVariant(affiliation = "Vienna University of Economics and Business", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
	public static Object[] stripStochasticInformation(PluginContext context, StochasticNet net){
		Marking marking = StochasticNetUtils.getInitialMarking(context, net);
		return ToStochasticNet.asPetriNet(context, net, marking);
	}
}
