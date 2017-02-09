package org.processmining.plugins.stochasticpetrinet.external;

import org.jbpt.petri.PetriNet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.Map;

public class PetrinetWithMapping {
    private PetrinetGraph promNet;
    private PetriNet jbptNet;

    private Map<Transition, org.jbpt.petri.Transition> transitionMapping;
    private Map<Place, org.jbpt.petri.Place> placeMapping;

    public PetrinetWithMapping(PetrinetGraph net) {
        this.promNet = net;
    }

    public void constructMapping() {
        jbptNet = new PetriNet();
        transitionMapping = new HashMap<Transition, org.jbpt.petri.Transition>();
        placeMapping = new HashMap<Place, org.jbpt.petri.Place>();

        for (Transition t : promNet.getTransitions()) {
            transitionMapping.put(t, jbptNet.addTransition(new org.jbpt.petri.Transition(t.getLabel())));
        }
        for (Place p : promNet.getPlaces()) {
            placeMapping.put(p, jbptNet.addPlace(new org.jbpt.petri.Place(p.getLabel())));
        }
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : promNet.getEdges()) {
            if (edge.getSource() instanceof Transition) {
                jbptNet.addFlow(transitionMapping.get(edge.getSource()), placeMapping.get(edge.getTarget()));
            } else {
                jbptNet.addFlow(placeMapping.get(edge.getSource()), transitionMapping.get(edge.getTarget()));
            }
        }
    }

    public PetrinetGraph getPromNet() {
        return promNet;
    }

    public void setPromNet(PetrinetGraph promNet) {
        this.promNet = promNet;
    }

    public PetriNet getJbptNet() {
        return jbptNet;
    }

    public void setJbptNet(PetriNet jbptNet) {
        this.jbptNet = jbptNet;
    }

    public Map<Transition, org.jbpt.petri.Transition> getTransitionMapping() {
        return transitionMapping;
    }

    public void setTransitionMapping(Map<Transition, org.jbpt.petri.Transition> transitionMapping) {
        this.transitionMapping = transitionMapping;
    }

    public Map<Place, org.jbpt.petri.Place> getPlaceMapping() {
        return placeMapping;
    }

    public void setPlaceMapping(Map<Place, org.jbpt.petri.Place> placeMapping) {
        this.placeMapping = placeMapping;
    }


}
