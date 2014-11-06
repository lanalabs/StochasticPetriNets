package org.processmining.plugins.stochasticpetrinet.prediction.timeseries;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.rosuda.JRI.Rengine;

public class TimeseriesPredictor {

	/** reference to the R engine */
	protected Rengine engine;
	
	public TimeseriesPredictor(){
		engine = RProvider.getEngine();
	}
	
	public void predict(StochasticNet net, XTrace observedEvents, Date currentTime, Marking initialMarking) {
		EfficientStochasticNetSemanticsImpl  semantics = (EfficientStochasticNetSemanticsImpl) TimePredictor.getCurrentState(net, initialMarking, observedEvents);		
		
		List<Set<Transition>> conflictingTransitions = getConflictingTransitions(semantics);
		
		Collection<Transition> enabledTransitions = semantics.getEnabledTransitions();
		
		
		if (!conflictingTransitions.isEmpty()){
			// resolve conflict at each conflict herd (there might be multiple concurrently enabled conflicting transitions)
			
			// TODO: take care of multiple conflicts! - For now, assume only one is there
			assert conflictingTransitions.size()==1;
			
			// resolve conflict by asking categorical time series!
			Set<Transition> firstConflictingTransitions = conflictingTransitions.get(0);
		
			Map<Transition, Double> transitionProbabilities = getTransitionProbabilities(firstConflictingTransitions, semantics);
			
		}
		
		// find next 
		semantics.getExecutableTransitions();
	}
	
	public Map<Transition, Double> getTransitionProbabilities(Set<Transition> conflictingTransitions, EfficientStochasticNetSemanticsImpl semantics) {
		Map<Transition, Double> probabilities = new HashMap<>();
		
		// collect training data and sort them according to the date
		Map<Long, String> sortedDecisions = new TreeMap<>();
		
		
		for (Transition t : conflictingTransitions){
			assert t instanceof TimedTransition;
			
			TimedTransition tt = (TimedTransition) t;
			String trainingData = tt.getTrainingData();
			
			String[] entries = trainingData.split("\n");
			// ignore header!
			for (int i = 1; i < entries.length; i++){
				String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
				sortedDecisions.put(Long.valueOf(entryParts[2]), semantics.getTransitionId(t)+StochasticManifestCollector.DELIMITER+entryParts[0]+StochasticManifestCollector.DELIMITER+entryParts[1]);
			}
		}
		for (Map.Entry<Long, String> entry : sortedDecisions.entrySet()){
			System.out.println(entry.getKey()+";"+entry.getValue());
		}
		
		return probabilities;
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
