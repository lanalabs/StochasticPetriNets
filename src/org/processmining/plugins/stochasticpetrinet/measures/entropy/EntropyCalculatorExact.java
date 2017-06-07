package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.measures.AbstractionLevel;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EntropyCalculatorExact extends AbstractEntropyCalculator {

    protected PNSimulatorConfig config;

    public EntropyCalculatorExact() {
        this.config = new PNSimulatorConfig(Integer.MAX_VALUE, TimeUnit.MINUTES);
        this.config.setDeterministicBoundedStateSpaceExploration(true);
    }

    public String getMeasureName() {
        return "Model entropy measure (exact)";
    }

    protected Map<Outcome, Double> getOutcomesAndCounts(UIPluginContext context, Petrinet net, Marking initialMarking, AbstractionLevel level) {
        Map<Outcome, Double> outcomesAndCounts = new HashMap<>();

        // simulate traces deterministically:
        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(net);

        PNSimulator simulator = new PNSimulator();


        XLog simulatedLog = simulator.simulate(context, net, semantics, config, initialMarking);
        XLogInfo info = XLogInfoFactory.createLogInfo(simulatedLog, XLogInfoImpl.STANDARD_CLASSIFIER);
        XEventClasses eventClasses = info.getEventClasses();
        Collection<XEventClass> classes = eventClasses.getClasses();
        Map<XEventClass, Integer> encoding = new HashMap<>();
        int i = 0;
        for (XEventClass eClass : classes) {
            encoding.put(eClass, i++);
        }

        for (XTrace trace : simulatedLog) {
            Outcome o = new Outcome(trace, level, eventClasses, encoding);
            double probability = Math.exp(StochasticNetUtils.getLogProbability(trace));
            if (!outcomesAndCounts.containsKey(o)) {
                outcomesAndCounts.put(o, probability);
            } else {
                outcomesAndCounts.put(o, outcomesAndCounts.get(o) + probability);
            }
        }

        return outcomesAndCounts;
    }

    protected String getNameInfo() {
        return "exact";
    }


}
