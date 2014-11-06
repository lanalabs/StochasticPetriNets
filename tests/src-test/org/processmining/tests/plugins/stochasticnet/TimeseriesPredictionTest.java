package org.processmining.tests.plugins.stochasticnet;

import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeseriesPredictor;

public class TimeseriesPredictionTest {

	@Test
	public void testTwoConflictingTransitions() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("Race_AB_enriched",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		TimeseriesPredictor predictor = new TimeseriesPredictor();
		
		XTrace observedEvents = new XTraceImpl(new XAttributeMapImpl());
		TestUtils.addEvent("t_init", observedEvents, 1000);
		
		Set<Transition> set = new HashSet<>();
			
		for (Transition t : net.getTransitions()){
			if (!t.getLabel().equals("t_init")){
				set.add(t);
			}
		}
		
		EfficientStochasticNetSemanticsImpl semantics = (EfficientStochasticNetSemanticsImpl) TimePredictor.getCurrentState(net, initialMarking, observedEvents);		
		
		predictor.getTransitionProbabilities(set, semantics);
		
	}
}
