package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

public class EntropyCalculatorApproximate extends AbstractEntropyCalculator {

	private int n = 1000;
	
	public String getMeasureName() {
		return "Model entropy (approximated by "+n+" samples)";
	}

	protected Map<Outcome, Double> getOutcomesAndCounts(UIPluginContext context, Petrinet net, Marking initialMarking,
			AbstractionLevel level) {
		Map<Outcome, Double> outcomesAndCounts = new HashMap<>();
		
		// simulate traces deterministically:
		Semantics<Marking,Transition> semantics = StochasticNetUtils.getSemantics(net);
		
		PNSimulator simulator = new PNSimulator();
		PNSimulatorConfig config = new PNSimulatorConfig(n, TimeUnit.MINUTES);
		config.setDeterministicBoundedStateSpaceExploration(false);
		
		Marking finalMarking = StochasticNetUtils.getFinalMarking(context, net);
		
		XLog simulatedLog = simulator.simulate(context, net, semantics, config, initialMarking, finalMarking);
		XLogInfo info = XLogInfoFactory.createLogInfo(simulatedLog, XLogInfoImpl.STANDARD_CLASSIFIER);
		XEventClasses eventClasses = info.getEventClasses();
		Collection<XEventClass> classes = eventClasses.getClasses();
		Map<XEventClass,Integer> encoding = new HashMap<>();
		int i = 0; 
		for (XEventClass eClass : classes){
			encoding.put(eClass, i++);
		}
		
		for (XTrace trace : simulatedLog){
			Outcome o = new Outcome(trace, level, eventClasses, encoding);
			if (!outcomesAndCounts.containsKey(o)){
				outcomesAndCounts.put(o, 1.);
			} else {
				outcomesAndCounts.put(o, outcomesAndCounts.get(o)+1);
			}
		}
		
		return outcomesAndCounts;
	}
	
	public void setN(int n){
		this.n = n;
	}

	protected String getNameInfo() {
		return "approximate";
	}
}
