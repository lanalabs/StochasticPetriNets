package org.processmining.plugins.stochasticpetrinet.external;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.jbpt.algo.tree.rpst.RPST;
import org.jbpt.bp.BehaviouralProfile;
import org.jbpt.bp.construct.BPCreatorUnfolding;
import org.jbpt.petri.Flow;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Node;
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
import org.processmining.plugins.stochasticpetrinet.external.Allocation.AllocType;

import java.util.*;

public class PetrinetModelAllocations {

    private Petrinet net;

    private Map<Transition, Set<Allocation>> locationAllocations;
    private Map<Transition, Set<Allocation>> resourceAllocations;

    public PetrinetModelAllocations(Petrinet net) {
        this.net = net;
        locationAllocations = new HashMap<Transition, Set<Allocation>>();
        resourceAllocations = new HashMap<Transition, Set<Allocation>>();
    }

    public void addAllocation(Transition transition, Allocation allocation) {
        if (!net.getTransitions().contains(transition)) {
            throw new IllegalArgumentException("Transition not in net!!");
        }
        Map<Transition, Set<Allocation>> target = allocation.getType().equals(AllocType.LOCATION) ? locationAllocations : resourceAllocations;
        if (!target.containsKey(transition)) {
            target.put(transition, new HashSet<Allocation>());
        }
        target.get(transition).add(allocation);
    }

    public Set<Allocation> getAllocations(Transition transition) {
        Set<Allocation> allocations = new HashSet<Allocation>();
        allocations.addAll(locationAllocations.get(transition));
        allocations.addAll(resourceAllocations.get(transition));
        return allocations;
    }

    /**
     * Use jbpt library to get relations of the behavioral profile of the net.
     * Only looks at real precendence/order between transitions.
     *
     * @param net a petri net model (PetrinetGraph)
     * @return a list of transition pairs that are in order.
     */
    public List<Pair<Transition, Transition>> getOrderRelation(PetrinetGraph net) {
        Pair<NetSystem, Map<Transition, org.jbpt.petri.Transition>> result = getPetrinetRepresentation(net);

        NetSystem system = result.getFirst();
        Map<Transition, org.jbpt.petri.Transition> transitionMapping = result.getSecond();

        BehaviouralProfile<NetSystem, Node> bp = BPCreatorUnfolding.getInstance().deriveRelationSet(system);
        List<Pair<Transition, Transition>> orderedRelations = new ArrayList<Pair<Transition, Transition>>();


        for (Transition tA : transitionMapping.keySet()) {
            org.jbpt.petri.Transition tAjbpt = transitionMapping.get(tA);
            for (Transition tB : transitionMapping.keySet()) {
                org.jbpt.petri.Transition tBjbpt = transitionMapping.get(tB);
                if (bp.areInOrder(tAjbpt, tBjbpt)) {
                    orderedRelations.add(new Pair<Transition, Transition>(tA, tB));
                }
            }
        }
        return orderedRelations;
    }

    private Pair<NetSystem, Map<Transition, org.jbpt.petri.Transition>> getPetrinetRepresentation(PetrinetGraph net2) {
        PetrinetWithMapping pnWithMapping = new PetrinetWithMapping(net2);
        pnWithMapping.constructMapping();

        Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);
        NetSystem system = new NetSystem(pnWithMapping.getJbptNet());
        //org.jbpt.petri.Marking marking = new org.jbpt.petri.Marking(pn);

        for (Place p : initialMarking) {
            org.jbpt.petri.Place place = pnWithMapping.getPlaceMapping().get(p);
            system.putTokens(place, 1);
        }
        return new Pair<NetSystem, Map<Transition, org.jbpt.petri.Transition>>(system, pnWithMapping.getTransitionMapping());
    }

    Map<Transition, Double> getTransitionProbabilities(PetrinetGraph net) {
        // TODO: maybe keep it simple by sampling some logs, or doing a sophisticated computation
        // idea: reuse order relations
        // 1. identify local probability by checking weights of conflicting transitions
        // 2. compute RPST tree structure and compute probability by using the formulas of loops (apply probabilities to branches)
        // 3. compute probability of nodes by traversing the upwards path to the root.
        Map<Transition, Double> localProbabilities = computeLocalProbabilities(net);

//		Map<Transition, Double> pathProbabilities = new HashMap<Transition, Double>();

        PetrinetWithMapping pnWithMapping = new PetrinetWithMapping(net);


        RPST<Flow, Node> rpst = new RPST<Flow, Node>(pnWithMapping.getJbptNet());
        System.out.println(rpst);

        // TODO: fix this later with real probabilities from the RPST
        return localProbabilities;
    }

    private Map<Transition, Double> computeLocalProbabilities(PetrinetGraph net) {
        Map<Transition, Double> localProbabilities = new HashMap<Transition, Double>();

        for (Transition transition : net.getTransitions()) {
            double totalWeight = 0;
            double totalRates = 0;
            double weight = 1;
            double firingRate = 1;
            double probabilityToFire = 1;
            boolean allImmediate = true;
            boolean someImmediate = false;

            if (transition instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) transition;
                weight = tt.getWeight();
                if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                    someImmediate = true;
                } else if (tt.getDistributionType().equals(DistributionType.EXPONENTIAL)) {
                    firingRate = 1.0 / tt.getDistributionParameters()[0]; // 1 / mean = rate
                    allImmediate = false;
                }
            }
            Set<Place> prePlaces = new HashSet<Place>();
            // get pre-set of transition (if one place, there might be a conflict)
            for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getInEdges(transition)) {
                prePlaces.add((Place) edge.getSource());
            }
            if (prePlaces.size() == 1) {
                for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getOutEdges(prePlaces.iterator().next())) {
                    Transition possiblyConflictingTransition = (Transition) edge.getTarget();
                    if (!possiblyConflictingTransition.equals(transition)) {
                        double otherWeight = 1;
                        double otherRate = 1;
                        if (possiblyConflictingTransition instanceof TimedTransition) {
                            TimedTransition otherTT = (TimedTransition) possiblyConflictingTransition;
                            otherWeight = otherTT.getWeight();
                            allImmediate &= otherTT.getDistributionType().equals(DistributionType.IMMEDIATE);
                            someImmediate |= otherTT.getDistributionType().equals(DistributionType.IMMEDIATE);
                            if (otherTT.getDistributionType().equals(DistributionType.EXPONENTIAL)) {
                                otherRate = 1.0 / otherTT.getDistributionParameters()[0]; // 1 / mean = rate
                            }
                        }
                        totalWeight += otherWeight;
                        totalRates += otherRate;
                    } else {
                        totalWeight += weight;
                        totalRates += firingRate;
                    }
                }
                boolean raceBetweenTransitions = !allImmediate && !someImmediate;
                if (raceBetweenTransitions) {
                    probabilityToFire = firingRate / totalRates;
                } else {
                    probabilityToFire = weight / totalWeight;
                }
            }
            localProbabilities.put(transition, probabilityToFire);
        }

        return localProbabilities;
    }

    public Set<Allocatable> getAllLocationAllocations() {
        Set<Allocatable> locations = new HashSet<Allocatable>();
        for (Set<Allocation> allocations : locationAllocations.values()) {
            for (Allocation alloc : allocations) {
                for (Allocatable all : alloc.getAllAllocatables()) {
                    locations.add(all);
                }
            }
        }
        return locations;
    }

    public Map<Transition, Set<Allocation>> getLocationAllocations() {
        return locationAllocations;
    }

    public double getReverseEntropy(AllocType type) {
        Map<String, Map<Transition, Double>> reverseDistributions = collectReverseDistributions(type);
        DescriptiveStatistics entropies = new DescriptiveStatistics();
        for (String alloc : reverseDistributions.keySet()) {
            Map<Transition, Double> transitionProbs = reverseDistributions.get(alloc);
            normalizeDistribution(transitionProbs);
            entropies.addValue(StochasticNetUtils.getEntropy(transitionProbs));
        }
        return entropies.getMean();
    }

    private void normalizeDistribution(Map<Transition, Double> transitionProbs) {
        double sum = 0.;
        for (Double d : transitionProbs.values()) {
            sum += d;
        }
        for (Transition t : transitionProbs.keySet()) {
            transitionProbs.put(t, transitionProbs.get(t) / sum);
        }
    }

    private Map<String, Map<Transition, Double>> collectReverseDistributions(AllocType type) {
        Map<Transition, Set<Allocation>> transitionAllocations = null;
        switch (type) {
            case LOCATION:
                transitionAllocations = locationAllocations;
                break;
            case RESOURCE:
                transitionAllocations = resourceAllocations;
                break;
        }
        Map<String, Map<Transition, Double>> reverseDistributions = new HashMap<String, Map<Transition, Double>>();
        for (Transition trans : transitionAllocations.keySet()) {
            double transPrior = 1.0 / transitionAllocations.size(); // TODO: improve by getting real probability of transition.
            Set<Allocation> allocations = transitionAllocations.get(trans);
            for (Allocation alloc : allocations) {
                Map<String, Double> probs = alloc.getProbabilitiesOfAllocations();
                for (String key : probs.keySet()) {
                    if (!reverseDistributions.containsKey(key)) {
                        reverseDistributions.put(key, new HashMap<Transition, Double>());
                    }
                    double prob = reverseDistributions.get(key).containsKey(trans) ? reverseDistributions.get(key).get(trans) : 0.0;
                    reverseDistributions.get(key).put(trans, prob + transPrior * probs.get(key));
                }
            }
        }
        return reverseDistributions;
    }
}
