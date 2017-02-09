package org.processmining.models.graphbased.directed.petrinet.impl;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.*;

import javax.swing.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StochasticNetImpl extends AbstractResetInhibitorNet implements StochasticNet {

    // maintain a set of all nodes for quicker access:
    private Set<PetrinetNode> nodes;
    private Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> edges;

    private ExecutionPolicy executionPolicy;
    private TimeUnit timeUnit;

    public StochasticNetImpl(String label) {
        super(true, false);
        getAttributeMap().put(AttributeMap.PREF_ORIENTATION, SwingConstants.WEST);
        getAttributeMap().put(AttributeMap.LABEL, label);
        nodes = new HashSet<PetrinetNode>();
        edges = new HashSet<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>();
    }

    protected StochasticNetImpl getEmptyClone() {
        return new StochasticNetImpl(getLabel());
    }


    public TimedTransition addImmediateTransition(String label) {
        return addImmediateTransition(label, 1);
    }

    public TimedTransition addImmediateTransition(String label, double weight) {
        return addImmediateTransition(label, weight, 1, null);
    }

    public TimedTransition addImmediateTransition(String label, double weight, int priority, String trainingData) {
        TimedTransition t = new TimedTransition(label, this, null, weight, priority, DistributionType.IMMEDIATE, null);
        t.setTrainingData(trainingData);
        transitions.add(t);
        nodes.add(t);
        graphElementAdded(t);
        return t;
    }

    public TimedTransition addTimedTransition(String label, DistributionType type, double... distributionParameters) {
        return addTimedTransition(label, 1, type, distributionParameters);
    }

    public TimedTransition addTimedTransition(String label, double weight, DistributionType type,
                                              double... distributionParameters) {
        return addTimedTransition(label, weight, type, "", distributionParameters);
    }

    public TimedTransition addTimedTransition(String label, double weight, DistributionType type,
                                              String trainingData, double... distributionParameters) {
        TimedTransition t = new TimedTransition(label, this, null, weight, 0, type, distributionParameters);
        transitions.add(t);
        nodes.add(t);
        graphElementAdded(t);
        t.setTrainingData(trainingData);
        return t;
    }

    /**
     * Replaces {@link Transition}s by {@link TimedTransition}s
     */
    @Override
    protected synchronized Map<DirectedGraphElement, DirectedGraphElement> cloneFrom(AbstractResetInhibitorNet net,
                                                                                     boolean transitions, boolean places, boolean arcs, boolean resets, boolean inhibitors) {
        Map<DirectedGraphElement, DirectedGraphElement> mapping = new HashMap<DirectedGraphElement, DirectedGraphElement>();

        if (transitions) {
            for (Transition t : net.transitions) {
                TimedTransition copy = addTimedTransition(t.getLabel(), DistributionType.UNDEFINED);
                copy.setInvisible(t.isInvisible());
                mapping.put(t, copy);
            }
        }
        if (places) {
            for (Place p : net.places) {
                Place copy = addPlace(p.getLabel());
                mapping.put(p, copy);
            }
        }
        if (arcs) {
            for (Arc a : net.arcs) {
                mapping.put(a, addArcPrivate((PetrinetNode) mapping.get(a.getSource()), (PetrinetNode) mapping.get(a
                        .getTarget()), a.getWeight(), a.getParent()));
            }
        }
        if (inhibitors) {
            for (InhibitorArc a : net.inhibitorArcs) {
                mapping.put(a, addInhibitorArc((Place) mapping.get(a.getSource()), (Transition) mapping.get(a
                        .getTarget()), a.getLabel()));
            }
        }
        if (resets) {
            for (ResetArc a : net.resetArcs) {
                mapping.put(a, addResetArc((Place) mapping.get(a.getSource()), (Transition) mapping.get(a.getTarget()),
                        a.getLabel()));
            }
        }
        getAttributeMap().clear();
        AttributeMap map = net.getAttributeMap();
        for (String key : map.keySet()) {
            getAttributeMap().put(key, map.get(key));
        }

        return mapping;
    }

    //	public synchronized Place addPlace(String label, ExpandableSubNet parent) {
//		Place p = super.addPlace(label, parent);
//		nodes.add(p);
//		return p;
//	}
//	public synchronized Transition addTransition(String label, ExpandableSubNet parent) {
//		Transition t = super.addTransition(label, parent);
//		nodes.add(t);
//		return t;
//	}
//	public synchronized Transition removeTransition(Transition transition) {
//		Transition toRemove = super.removeTransition(transition);
//		nodes.remove(toRemove);
//		return toRemove;
//	}
//	public synchronized Place removePlace(Place place) {
//		Place toRemove = super.removePlace(place);
//		nodes.remove(toRemove);
//		return toRemove;
//	}
    @Override
    public void graphElementAdded(Object element) {
        if (element instanceof PetrinetNode) {
            PetrinetNode node = (PetrinetNode) element;
            nodes.add(node);
        }
        if (element instanceof PetrinetEdge<?, ?>) {
            edges.add((PetrinetEdge<PetrinetNode, PetrinetNode>) element);
        }
        super.graphElementAdded(element);
    }

    public void graphElementRemoved(Object element) {
        if (element instanceof PetrinetNode) {
            PetrinetNode node = (PetrinetNode) element;
            nodes.remove(node);
        }
        if (element instanceof PetrinetEdge<?, ?>) {
            edges.remove(element);
        }
        super.graphElementRemoved(element);
    }

    public synchronized Set<PetrinetNode> getNodes() {
        return nodes;
    }

    public synchronized Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> getEdges() {
        return edges;
    }

    public ExecutionPolicy getExecutionPolicy() {
        return executionPolicy;
    }

    public void setExecutionPolicy(ExecutionPolicy executionPolicy) {
        this.executionPolicy = executionPolicy;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }
}
