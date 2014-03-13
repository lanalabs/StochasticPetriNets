package org.processmining.plugins.pnml.exporting;

import java.io.File;
import java.io.Writer;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

@Plugin(name = "PNML export (Petri net)", returnLabels = {}, returnTypes = {}, parameterLabels = { "StochasticNet",
		"File" }, userAccessible = true)
@UIExportPlugin(description = "(stochastic) PNML files", extension = "pnml")
public class PnmlExportStochasticNet {

	@PluginVariant(variantLabel = "PNML export (Stochastic Petri net)", requiredParameterLabels = { 0, 1 })
	public void exportPetriNetToPNMLFile(PluginContext context, Petrinet net, File file) throws Exception {
		if (net instanceof StochasticNet){
			Serializer serializer = new Persister();
			serializer.write(convertToPNML(context, net), file);
		} else {
			throw new IllegalArgumentException("Only stochastic Petri nets supported!");
		}
	}
	
	public void exportPetriNetToPNMLFile(PluginContext context, Petrinet net, Writer writer) throws Exception {
		if (net instanceof StochasticNet){
			Serializer serializer = new Persister();
			serializer.write(convertToPNML(context, net), writer);
		} else {
			throw new IllegalArgumentException("Only stochastic Petri nets supported!");
		}
	}

	private PNMLRoot convertToPNML(PluginContext context, Petrinet net) {
		StochasticNetToPNMLConverter converter = new StochasticNetToPNMLConverter();
		Marking marking = new Marking();
		try {
			if (context!=null){
				marking = context.tryToFindOrConstructFirstObject(Marking.class, InitialMarkingConnection.class,
						InitialMarkingConnection.MARKING, net);
			}
		} catch (ConnectionCannotBeObtained e) {
			// don't care - stick with empty marking
		}
		GraphLayoutConnection layout;
		try {
			if (context != null){
				layout = context.getConnectionManager().getFirstConnection(GraphLayoutConnection.class, context, net);
			} else {
				throw new ConnectionCannotBeObtained("No context available.", GraphLayoutConnection.class);
			}
		} catch (ConnectionCannotBeObtained e) {
			layout = new GraphLayoutConnection(net);
		}
		
		PNMLRoot root = converter.convertNet((StochasticNet) net, marking, layout);
		return root;
	}
}
