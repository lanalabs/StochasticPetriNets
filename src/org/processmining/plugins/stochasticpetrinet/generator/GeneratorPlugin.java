package org.processmining.plugins.stochasticpetrinet.generator;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;

/**
 * Generates block-structured {@link StochasticNet} models given different parameters specified in {@link GeneratorConfig}.
 *
 * @author Andreas Rogge-Solti
 */
public class GeneratorPlugin {

    @Plugin(name = "Generate block-structured stochastic Petri net",
            parameterLabels = {},
            returnLabels = {StochasticNet.PARAMETER_LABEL, "Initial Marking", "Final Marking"},
            returnTypes = {StochasticNet.class, Marking.class, Marking.class},
            userAccessible = true,
            help = "Performs a series of random structured insertion operations of new control flow constructs resulting in a random stochastic net that is by generation sound, free-choice and block-structured.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public Object[] generateNet(UIPluginContext context) {

        GeneratorConfigPanel panel = new GeneratorConfigPanel();

        GeneratorConfig config = panel.getConfig(context);
        Generator generator = new Generator(0);
        Object[] netAndMarkings = generator.generateStochasticNet(config);
        InitialMarkingConnection connection = new InitialMarkingConnection((PetrinetGraph) netAndMarkings[0], (Marking) netAndMarkings[1]);
        context.addConnection(connection);
        FinalMarkingConnection fmConnection = new FinalMarkingConnection((PetrinetGraph) netAndMarkings[0], (Marking) netAndMarkings[2]);
        context.addConnection(fmConnection);
        context.getFutureResult(0).setLabel(config.getName());
        return netAndMarkings;
    }
}
