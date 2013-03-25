package org.processmining.plugins.pnml.exporting;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.AbstractGraphNode;
import org.processmining.models.graphbased.AttributeMapOwner;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.simple.PNMLArc;
import org.processmining.plugins.pnml.simple.PNMLGraphics;
import org.processmining.plugins.pnml.simple.PNMLName;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLPage;
import org.processmining.plugins.pnml.simple.PNMLPlace;
import org.processmining.plugins.pnml.simple.PNMLPoint;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.pnml.simple.PNMLToolSpecific;
import org.processmining.plugins.pnml.simple.PNMLTransition;

public class StochasticNetToPNMLConverter {
	public PNMLRoot convertNet(StochasticNet net, Marking initialMarking, GraphLayoutConnection layout){
		PNMLRoot root = new PNMLRoot();
		List<PNMLNet> pnmlNets = new ArrayList<PNMLNet>();
		PNMLNet pnmlNet = new PNMLNet();
		// add a page:
		pnmlNet.setType(PNMLNet.PT_NET_CLASS);
		pnmlNet.setId(net.getLabel());
		List<PNMLPage> pnmlPages = new ArrayList<PNMLPage>();
		PNMLPage pnmlPage = new PNMLPage();
		pnmlPage.setId("p1");
		pnmlPage.setList(new ArrayList<Object>());
		for (Transition t : net.getTransitions()){
			pnmlPage.getList().add(getPNMLTransition(t,layout));
		}
		for (Place p : net.getPlaces()){
			pnmlPage.getList().add(getPNMLPlace(p,initialMarking,layout));
		}
		int arcCount = 0;
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : net.getEdges()){
			PetrinetNode source = arc.getSource();
			PetrinetNode target = arc.getTarget();
			pnmlPage.getList().add(getPNMLArc(source,target, arcCount++,layout));
		}
		pnmlPages.add(pnmlPage);
		pnmlNet.setPage(pnmlPages);
		pnmlNets.add(pnmlNet);
		root.setNet(pnmlNets);
		net.getLabel();
		
		return root;
	}

	private Object getPNMLArc(PetrinetNode source, PetrinetNode target, int arcCount, GraphLayoutConnection layout) {
		PNMLArc pnmlArc = new PNMLArc();
		pnmlArc.setSource(getId(source));
		pnmlArc.setTarget(getId(target));
		pnmlArc.setId("a"+arcCount);
		return pnmlArc;
	}

	private Object getPNMLPlace(Place p, Marking initialMarking, GraphLayoutConnection layout) {
		PNMLPlace place = new PNMLPlace();
		place.setId(getId(p));
		int initialPlaces = 0;
		if (initialMarking.contains(p)){
			initialPlaces = initialMarking.occurrences(p);
		}
		place.setInitialMarking(initialPlaces);
		place.setGraphics(getGraphics(p, layout));
		return place;
	}

	private Object getPNMLTransition(Transition t, GraphLayoutConnection layout) {
		PNMLTransition transition = new PNMLTransition();
		transition.setId(getId(t));
		transition.setName(new PNMLName(t.getLabel()));
		if (t instanceof TimedTransition){
			TimedTransition tt = (TimedTransition) t;
			List<PNMLToolSpecific> list = new ArrayList<PNMLToolSpecific>();
			PNMLToolSpecific specific = new PNMLToolSpecific();
			specific.setTool(PNMLToolSpecific.STOCHASTIC_ANNOTATION);
			specific.setVersion(PNMLToolSpecific.STOCHASTIC_ANNOTATION_VERSION);
			specific.setProperties(new HashMap<String, String>());
			specific.getProperties().put(PNMLToolSpecific.PRIORITY, String.valueOf(tt.getPriority()));
			specific.getProperties().put(PNMLToolSpecific.WEIGHT, String.valueOf(tt.getWeight()));
			specific.getProperties().put(PNMLToolSpecific.DISTRIBUTION_TYPE, tt.getDistributionType().toString());
			double[] parameters = tt.getDistributionParameters();
			StringBuilder params = new StringBuilder();
			if (parameters != null){
				for (double p : parameters){
					if (params.length()!=0){
						params.append(PNMLToolSpecific.VALUES_SEPARATOR);
					}
					params.append(String.valueOf(p));
				}
			}
			specific.getProperties().put(PNMLToolSpecific.DISTRIBUTION_PARAMETERS, params.toString());
			list.add(specific);
			transition.setToolspecific(list);
		}
		transition.setGraphics(getGraphics(t,layout));
		return transition;
	}

	private String getId(AbstractGraphNode node) {
		if(node.getAttributeMap().containsKey("id")){
			return String.valueOf(node.getAttributeMap().get("id"));
		}
		return node.getId().toString().substring(5);
	}

	private PNMLGraphics getGraphics(AttributeMapOwner a, GraphLayoutConnection layout) {
		PNMLGraphics graphics = new PNMLGraphics();
		Point2D position = layout.getPosition(a);
		Dimension size = layout.getSize(a);
		graphics.setPosition(new ArrayList<PNMLPoint>());
		graphics.getPosition().add(new PNMLPoint((int)(position.getX()-size.getWidth()/2.0), (int)(position.getY()-size.getHeight()/2.0)));
		graphics.setDimension(new PNMLPoint(size.width, size.height));
		return graphics;
	}
}
