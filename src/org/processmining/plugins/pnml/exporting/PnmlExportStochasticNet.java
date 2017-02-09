package org.processmining.plugins.pnml.exporting;

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
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.Writer;

@Plugin(name = "PNML export (Petri net)", returnLabels = {}, returnTypes = {}, parameterLabels = {"StochasticNet",
        "File"}, userAccessible = true)
@UIExportPlugin(description = "(stochastic) PNML files", extension = "pnml")
public class PnmlExportStochasticNet {

    @PluginVariant(variantLabel = "PNML export (Stochastic Petri net)", requiredParameterLabels = {0, 1})
    public void exportPetriNetToPNMLFile(PluginContext context, StochasticNet net, File file) throws Exception {
        Serializer serializer = new Persister();
        serializer.write(convertToPNML(context, net), file);
    }

    public void exportPetriNetToPNMLFile(PluginContext context, Petrinet net, Writer writer) throws Exception {
        Serializer serializer = new Persister();
        serializer.write(convertToPNML(context, net), writer);
    }

    private PNMLRoot convertToPNML(PluginContext context, Petrinet net) {
        StochasticNetToPNMLConverter converter = new StochasticNetToPNMLConverter();
        Marking marking = new Marking();
        try {
            if (context != null) {
                marking = context.tryToFindOrConstructFirstObject(Marking.class, InitialMarkingConnection.class,
                        InitialMarkingConnection.MARKING, net);
            }
        } catch (ConnectionCannotBeObtained e) {
            // don't care - stick with empty marking
        } finally {
            if (marking == null) {
                marking = StochasticNetUtils.getInitialMarking(null, net);
            }
        }
        GraphLayoutConnection layout;
        try {
            if (context != null && context.getConnectionManager() != null) {
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
