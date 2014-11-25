package org.processmining.plugins.stochasticpetrinet.prediction.timeseries;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.prediction.AbstractTimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNTimeSeriesSimulator;

public class TimeseriesPredictor extends AbstractTimePredictor{

	
	public TimeseriesPredictor(){
	}
	
	protected DescriptiveStatistics getPredictionStats(StochasticNet model, XTrace observedEvents, Date currentTime,
			Marking initialMarking) {
		
		EfficientStochasticNetSemanticsImpl  semantics = (EfficientStochasticNetSemanticsImpl) TimePredictor.getCurrentState(model, initialMarking, observedEvents);		
		Marking currentMarking = semantics.getCurrentState();
		
		// perform a headless simulation
		PNSimulator simulator = new PNTimeSeriesSimulator();
		
		
		PNSimulatorConfig config = new PNSimulatorConfig(1,model.getTimeUnit());
		config.setSimulateTraceless(true);
		Long lastEventTime;
		if (observedEvents.isEmpty()){
			lastEventTime = currentTime.getTime();
		} else {
			lastEventTime = XTimeExtension.instance().extractTimestamp(observedEvents.get(observedEvents.size()-1)).getTime();
		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		//long now = System.currentTimeMillis();
		
		
		
		StochasticNetUtils.useCache(true);
		double errorPercent = 100; // percentage in error of 99% confidence band
		int i = 0;
		while (errorPercent > ERROR_BOUND_PERCENT && i <MAX_RUNS){
			i++;
			stats.addValue((Long)simulator.simulateOneTrace(model, semantics, config, currentMarking, lastEventTime, currentTime.getTime(), i, false, null));
			semantics.setCurrentState(currentMarking);
			if (i % 100 == 0){
				// update error:
				errorPercent = getErrorPercent(stats);
			}
		}
		return stats;
	}

	

	private List<Set<Transition>> getConflictingTransitions(Semantics<Marking, Transition> semantics){
		// assume free choice!
		List<Set<Transition>> conflictingTransitions = new LinkedList<>();
		Marking marking = semantics.getCurrentState();
		Iterator<Place> markingIterator = marking.iterator();
		while (markingIterator.hasNext()){
			Place p = markingIterator.next();
			Collection<PetrinetEdge<? extends PetrinetNode,? extends PetrinetNode>> outEdges = p.getGraph().getOutEdges(p);
			if (outEdges.size() > 1){
				Set<Transition> conflictingTransitionsForThisPlace = new HashSet<>();
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outEdges){
					conflictingTransitionsForThisPlace.add((Transition) edge.getTarget());
				}
				conflictingTransitions.add(conflictingTransitionsForThisPlace);
			}
		}
		return conflictingTransitions;
	}
}
