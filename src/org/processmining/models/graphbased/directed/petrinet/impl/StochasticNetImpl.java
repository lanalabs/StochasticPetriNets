package org.processmining.models.graphbased.directed.petrinet.impl;

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingConstants;

import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.InhibitorArc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.ResetArc;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

public class StochasticNetImpl extends AbstractResetInhibitorNet implements StochasticNet{

	public StochasticNetImpl(String label){
		super(true,false);
		getAttributeMap().put(AttributeMap.PREF_ORIENTATION, SwingConstants.WEST);
		getAttributeMap().put(AttributeMap.LABEL, label);
	}
	
	protected StochasticNetImpl getEmptyClone() {
		return new StochasticNetImpl(getLabel());
	}
	
	

	public TimedTransition addImmediateTransition(String label) {
		return addImmediateTransition(label, 1);
	}

	public TimedTransition addImmediateTransition(String label, double weight) {
		return addImmediateTransition(label, weight, 1);
	}

	public TimedTransition addImmediateTransition(String label, double weight, int priority) {
		TimedTransition t = new TimedTransition(label, this, null, weight, priority, DistributionType.IMMEDIATE,null);
		transitions.add(t);
		graphElementAdded(t);
		return t;
	}

	public TimedTransition addTimedTransition(String label, DistributionType type, double... distributionParameters) {
		return addTimedTransition(label, 1, type, distributionParameters);
	}

	public TimedTransition addTimedTransition(String label, double weight, DistributionType type,
			double... distributionParameters) {
		TimedTransition t = new TimedTransition(label, this, null, weight, 0, type, distributionParameters);
		transitions.add(t);
		graphElementAdded(t);
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
}
