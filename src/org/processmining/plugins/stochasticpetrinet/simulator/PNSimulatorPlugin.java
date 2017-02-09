package org.processmining.plugins.stochasticpetrinet.simulator;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

/**
 * Very basic Petri net simulator that can handle 1-safe P/T nets with arbitrary duration distributions.
 *
 * @author Andreas Rogge-Solti
 * @deprecated Use CPN Tools or TimeNET instead for simulation of Petri Nets
 */
public class PNSimulatorPlugin {
    // Disabled (use of CPN-Tools is preferred)

    /**
     * Should be able to simulate very simple (stochastic) P/T Petri nets.
     * Caution: Use of established and performance simulation tools, such as CPN-Tools, is strongly encouraged.
     * This plugin was only made for quick feedback on models without the round-trip to another tool.
     *
     * @param context
     * @param petriNet the net to be simulated
     * @return a XES-log to be used in ProM.
     * @deprecated In most cases, you want to use real and mature Petri net simulators (e.g., CPN Tools) instead!
     */
    @Deprecated
    @Plugin(name = "Perform a simple simulation of a (stochastic) Petri net",
            parameterLabels = {"Petri Net"},
            returnLabels = {"Artificial Log"},
            returnTypes = {XLog.class},
            userAccessible = true,
            help = "Simulates runs through a petri net model and stores the simulated traces in a log.")

    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public static XLog options(final UIPluginContext context, final PetrinetGraph petriNet) {
        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(petriNet);
        PNSimulator simulator = new PNSimulator();
        XLog log = simulator.simulate(context, petriNet, semantics);
        context.getFutureResult(0).setLabel(XConceptExtension.instance().extractName(log));
        return log;
    }


}
