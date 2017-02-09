package org.processmining.plugins.pnml.importing;

import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.InputStream;

@Plugin(name = "Import Stochastic Petri Nets from PNML file", parameterLabels = {"Filename"}, returnLabels = {
        StochasticNet.PARAMETER_LABEL, "Marking", "Marking"}, returnTypes = {StochasticNet.class, Marking.class, Marking.class})
@UIImportPlugin(description = "PNML Stochastic Petri Net files", extensions = {"pnml"})
public class PnmlImportStochasticNet extends AbstractImportPlugin {

    protected Object[] importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
            throws Exception {
        Serializer serializer = new Persister();
        PNMLRoot pnml = serializer.read(PNMLRoot.class, input);

        StochasticNetDeserializer converter = new StochasticNetDeserializer();
        return converter.convertToNet(context, pnml, filename, true);
    }

}
