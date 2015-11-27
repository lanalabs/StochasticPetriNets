package org.processmining.plugins.stochasticpetrinet.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.jbpt.bp.BehaviouralProfile;
import org.jbpt.bp.construct.BPCreatorUnfolding;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Node;
import org.jbpt.petri.PetriNet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class PetrinetModelAllocations {
	
	private Petrinet net;
	
	private Map<Transition, Set<Allocation>> allocations;
	
	public PetrinetModelAllocations(Petrinet net){
		this.net = net;
		allocations = new HashMap<Transition, Set<Allocation>>();
	}

	public void addAllocation(Transition transition, Allocation allocation){
		if (!net.getTransitions().contains(transition)){
			throw new IllegalArgumentException("Transition not in net!!");
		}
		if (!allocations.containsKey(transition)){
			allocations.put(transition, new HashSet<Allocation>());
		}
		allocations.get(transition).add(allocation);
	}
	
	public Set<Allocation> getAllocations(Transition transition){
		return allocations.get(transition);
	}
	
	/**
	 * Use jbpt library to get relations of the behavioral profile of the net.
	 * Only looks at real precendence/order between transitions.
	 * 
	 * @param net a petri net model (PetrinetGraph) 
	 * @return a list of transition pairs that are in order.
	 */
	public List<Pair<Transition, Transition>> getOrderRelation(PetrinetGraph net){
		Pair<NetSystem, Map<Transition, org.jbpt.petri.Transition>> result = getPetrinetRepresentation(net);
		
		NetSystem system = result.getFirst();
		Map<Transition, org.jbpt.petri.Transition> transitionMapping = result.getSecond();
		
		BehaviouralProfile<NetSystem, Node> bp = BPCreatorUnfolding.getInstance().deriveRelationSet(system);
		List<Pair<Transition, Transition>> orderedRelations = new ArrayList<Pair<Transition,Transition>>();
		
		
		for (Transition tA : transitionMapping.keySet()){
			org.jbpt.petri.Transition tAjbpt = transitionMapping.get(tA);
			for (Transition tB : transitionMapping.keySet()){
				org.jbpt.petri.Transition tBjbpt = transitionMapping.get(tB);
				if (bp.areInOrder(tAjbpt, tBjbpt)){
					orderedRelations.add(new Pair<Transition, Transition>(tA, tB));
				}
			}
		}
		return orderedRelations;
	}
	
	private Pair<NetSystem, Map<Transition, org.jbpt.petri.Transition>> getPetrinetRepresentation(PetrinetGraph net2) {
		PetriNet pn = new PetriNet();
		Map<Transition, org.jbpt.petri.Transition> transitionMapping = new HashMap<Transition, org.jbpt.petri.Transition>();
		Map<Place, org.jbpt.petri.Place> placeMapping = new HashMap<Place, org.jbpt.petri.Place>();
		
		for (Transition t : net.getTransitions()){
			transitionMapping.put(t, pn.addTransition(new org.jbpt.petri.Transition(t.getLabel())));
		}
		for (Place p : net.getPlaces()){
			placeMapping.put(p, pn.addPlace(new org.jbpt.petri.Place(p.getLabel())));
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()){
			if (edge.getSource() instanceof Transition){
				pn.addFlow(transitionMapping.get(edge.getSource()),placeMapping.get(edge.getTarget()));
			} else {
				pn.addFlow(placeMapping.get(edge.getSource()), transitionMapping.get(edge.getTarget()));
			}
		}
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net); 
		NetSystem system = new NetSystem(pn);
		//org.jbpt.petri.Marking marking = new org.jbpt.petri.Marking(pn);
		
		for (Place p : initialMarking){
			org.jbpt.petri.Place place = placeMapping.get(p);
			system.putTokens(place, 1);
		}
		return new Pair<NetSystem, Map<Transition,org.jbpt.petri.Transition>>(system, transitionMapping);
	}

	List<Pair<Transition, Double>> getTransitionProbabilities(PetrinetGraph net){
		// TODO: maybe keep it simple by sampling some logs, or doing a sophisticated computation
		// idea: reuse order relations
		// 1. identify local probability by checking weights of conflicting transitions
		// 2. compute RPST tree structure and compute probability by using the formulas of loops (apply probabilities to branches) 
		// 3. compute probability of nodes by traversing the upwards path to the root.
		Map<Transition, Double> localProbabilities = computeLocalProbabilities(net);
		
		
		Map<Transition, Double> pathProbabilities = new HashMap<Transition, Double>();
		
		
		
		return null;
	}

	private Map<Transition, Double> computeLocalProbabilities(PetrinetGraph net) {
		Map<Transition, Double> localProbabilities = new HashMap<Transition, Double>();
		
		for (Transition transition : net.getTransitions()){
			double totalWeight = 0;
			double totalRates = 0;
			double weight = 1;
			double firingRate = 1;
			double probabilityToFire = 1;
			boolean allImmediate = true;
			boolean someImmediate = false;
			
			if (transition instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) transition;
				weight = tt.getWeight();
				if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
					someImmediate = true;
				} else if (tt.getDistributionType().equals(DistributionType.EXPONENTIAL)){
					firingRate = 1.0/tt.getDistributionParameters()[0]; // 1 / mean = rate
					allImmediate = false;
				}
			}
			Set<Place> prePlaces = new HashSet<Place>();
			// get pre-set of transition (if one place, there might be a conflict)
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(transition)){
				prePlaces.add((Place) edge.getSource());
			}
			if (prePlaces.size() == 1){
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(prePlaces.iterator().next())){
					Transition possiblyConflictingTransition = (Transition) edge.getTarget();
					if (!possiblyConflictingTransition.equals(transition)){
						double otherWeight = 1;
						double otherRate = 1;
						if (possiblyConflictingTransition instanceof TimedTransition){
							TimedTransition otherTT = (TimedTransition) possiblyConflictingTransition;
							otherWeight = otherTT.getWeight();
							allImmediate &= otherTT.getDistributionType().equals(DistributionType.IMMEDIATE);
							someImmediate |= otherTT.getDistributionType().equals(DistributionType.IMMEDIATE);
							if (otherTT.getDistributionType().equals(DistributionType.EXPONENTIAL)){
								otherRate = 1.0 / otherTT.getDistributionParameters()[0]; // 1 / mean = rate
							}
						}
						totalWeight+= otherWeight;
						totalRates += otherRate;
					} else {
						totalWeight += weight;
						totalRates += firingRate;
					}
				}
				boolean raceBetweenTransitions = !allImmediate && !someImmediate;
				if (raceBetweenTransitions){
					probabilityToFire = firingRate / totalRates;
				} else {
					probabilityToFire = weight / totalWeight;
				}
			}
			localProbabilities.put(transition, probabilityToFire);
			
		}
		
		return localProbabilities;
	}
}
