package org.processmining.tests.plugins.stochasticnet;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration.TimeSeriesType;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeseriesPredictor;

public class TimeseriesPredictionTest {

	@Test
	public void testTwoConflictingTransitions() throws Exception {

		System.out.println("LD_LIBRARY_PATH: "+System.getenv("LD_LIBRARY_PATH"));

		Object[] netAndMarking = TestUtils.loadModel("Race_ABC_enriched2",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		TimeseriesPredictor predictor = new TimeseriesPredictor();
		
		XTrace observedEvents = new XTraceImpl(new XAttributeMapImpl());
		TestUtils.addEvent("t_init", observedEvents, System.currentTimeMillis()+1000);
		
		Set<Transition> set = new HashSet<>();
			
		for (Transition t : net.getTransitions()){
			if (!t.getLabel().equals("t_init")){
				set.add(t);
			}
		}
		
		EfficientStochasticNetSemanticsImpl semantics = (EfficientStochasticNetSemanticsImpl) TimePredictor.getCurrentStateWithAlignment(net, initialMarking, observedEvents);		
		
		Pair<Double, Double> prediction = predictor.predict(net, observedEvents, new Date(), initialMarking);
		
//		getTransitionProbabilities(new Date(), 1, set, semantics);
		System.out.println("estimate: "+prediction.getFirst()+ "; confidence interval: "+prediction.getSecond());
	}
	
	
	@Test
	public void testTwoConflictingTransitionsBPIC() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("BPIC_12_trained",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		TimeseriesPredictor predictor = new TimeseriesPredictor();
		
		XTrace observedEvents = new XTraceImpl(new XAttributeMapImpl());
		String[] samples = ((TimedTransition)net.getTransitions().iterator().next()).getTrainingData().split("\n");
		long time = Long.valueOf(samples[samples.length-1].split(";")[2]);
		TestUtils.addEvent("A_SUBMITTED", observedEvents, time+1000);
		
		Pair<Double, Double> prediction = predictor.predict(net, observedEvents, XTimeExtension.instance().extractTimestamp(observedEvents.get(0)), initialMarking);
		
//		getTransitionProbabilities(new Date(), 1, set, semantics);
		System.out.println("estimate: "+prediction.getFirst()+ "; confidence interval: "+prediction.getSecond());
	}
	
	@Test
	public void testTwoConflictingTransitionsCallCenter() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("queue_model_enriched_training",true);
		
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = (Marking) netAndMarking[1];
		
		
		TimeSeriesConfiguration config = new TimeSeriesConfiguration();
		config.setTimeSeriesType(TimeSeriesType.NAIVE_METHOD);
		TimeseriesPredictor predictor = new TimeseriesPredictor(config);
		
		XTrace observedEvents = new XTraceImpl(new XAttributeMapImpl());
		TestUtils.addEvent("Start", observedEvents, 1000);
		TestUtils.addEvent("vru_entry", observedEvents, 2000);
		TestUtils.addEvent("vru_exit", observedEvents, 4000);
		TestUtils.addEvent("q_start", observedEvents, 4100);
		
		
		Set<Transition> set = new HashSet<>();
			
		for (Transition t : net.getTransitions()){
			if (t.getLabel().equals("stay") || t.getLabel().equals("hang_up")){
				set.add(t);
			}
		}
		
		Pair<Double, Double> prediction = predictor.predict(net, observedEvents, new Date(), initialMarking);
		
		System.out.println("estimate: "+prediction.getFirst()+ "; confidence interval: "+prediction.getSecond());
		
//		EfficientStochasticNetSemanticsImpl semantics = (EfficientStochasticNetSemanticsImpl) TimePredictor.getCurrentState(net, initialMarking, observedEvents);		
//		
//		predictor.getTransitionProbabilities(new Date(), 1, set, semantics);
		
	}
}
