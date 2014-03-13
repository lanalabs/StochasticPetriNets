package org.processmining.plugins.pnml.exporting;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import org.processmining.plugins.pnml.PNMLParameter;
import org.processmining.plugins.pnml.simple.PNMLArc;
import org.processmining.plugins.pnml.simple.PNMLGraphics;
import org.processmining.plugins.pnml.simple.PNMLName;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLPage;
import org.processmining.plugins.pnml.simple.PNMLPlace;
import org.processmining.plugins.pnml.simple.PNMLPoint;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.pnml.simple.PNMLText;
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
		List<PNMLToolSpecific> toolTopSpecs = new ArrayList<PNMLToolSpecific>();
		PNMLToolSpecific toolTopSpec = new PNMLToolSpecific();
		toolTopSpec.setTool(PNMLToolSpecific.STOCHASTIC_ANNOTATION);
		toolTopSpec.setVersion(PNMLToolSpecific.STOCHASTIC_ANNOTATION_VERSION);
		toolTopSpec.setProperties(new HashMap<String, String>());
		toolTopSpec.getProperties().put(PNMLToolSpecific.EXECUTION_POLICY, net.getExecutionPolicy().toString());
		toolTopSpec.getProperties().put(PNMLToolSpecific.TIME_UNIT, net.getTimeUnit().toString());
		toolTopSpecs.add(toolTopSpec);
		pnmlNet.setToolspecific(toolTopSpecs);
		
		List<PNMLPage> pnmlPages = new ArrayList<PNMLPage>();
		PNMLPage pnmlPage = new PNMLPage();
		pnmlPage.setId("p1");
		pnmlPage.setList(new ArrayList<Object>());
		int nodeCount = 0;
		for (Transition t : net.getTransitions()){
			pnmlPage.getList().add(getPNMLTransition(t,nodeCount++,layout));
		}
		for (Place p : net.getPlaces()){
			pnmlPage.getList().add(getPNMLPlace(p,initialMarking,nodeCount++,layout));
		}
		int arcCount = 0;
		Set<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> arcs = new TreeSet<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>(new Comparator<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>>() {
			public int compare(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> o1,
					PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> o2) {
				return getId(o1.getSource()).compareTo(getId(o2.getSource()));
			}
		});
		arcs.addAll(net.getEdges());
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : arcs){
			pnmlPage.getList().add(getPNMLArc(arc, arcCount++,layout));
		}
		pnmlPages.add(pnmlPage);
		pnmlNet.setPage(pnmlPages);
		pnmlNets.add(pnmlNet);
		root.setNet(pnmlNets);
		net.getLabel();
		
		return root;
	}

	private Object getPNMLArc(PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc, int arcCount, GraphLayoutConnection layout) {
		PetrinetNode source = arc.getSource();
		PetrinetNode target = arc.getTarget();
		
		PNMLArc pnmlArc = new PNMLArc();
		pnmlArc.setSource(getId(source));
		pnmlArc.setTarget(getId(target));
		pnmlArc.setId("a"+arcCount);
		if (layout.getEdgePoints(arc) != null && layout.getEdgePoints(arc).size()>0){
			pnmlArc.setGraphics(getArcGraphics(layout.getEdgePoints(arc)));
		}
		return pnmlArc;
	}

	

	private Object getPNMLPlace(Place p, Marking initialMarking, int nodeCount, GraphLayoutConnection layout) {
		PNMLPlace place = new PNMLPlace();
		place.setId(getId(p, nodeCount));
		place.setName(new PNMLName(getName(p)));
		int initialPlaces = 0;
		if (initialMarking.contains(p)){
			initialPlaces = initialMarking.occurrences(p);
		}
		if (initialPlaces>0){
			place.setInitialMarking(new PNMLText(String.valueOf(initialPlaces)));
		}
		place.setGraphics(getGraphics(p, layout));
		return place;
	}

	private Object getPNMLTransition(Transition t, int nodeCount, GraphLayoutConnection layout) {
		PNMLTransition transition = new PNMLTransition();
		transition.setId(getId(t,nodeCount));
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

	private String getName(AbstractGraphNode node){
		return node.getLabel();
	}
	private String getId(AbstractGraphNode node){
		return getId(node,0);
	}
	private String getId(AbstractGraphNode node, int nodeCount) {
		if (!node.getAttributeMap().containsKey("id")){
			node.getAttributeMap().put("id", "n"+nodeCount);
		}
		return String.valueOf(node.getAttributeMap().get("id"));
	}

	private PNMLGraphics getGraphics(AttributeMapOwner a, GraphLayoutConnection layout) {
		PNMLGraphics graphics = new PNMLGraphics();
		Dimension size = layout.getSize(a);
		size.width = (int) (size.width/PNMLParameter.getScaleForViewInProM());
		size.height = (int) (size.height/PNMLParameter.getScaleForViewInProM());
		if (layout.getPosition(a)!= null){
			Point2D position = layout.getPosition(a);
			graphics.setPosition(new ArrayList<PNMLPoint>());
			graphics.getPosition().add(new PNMLPoint((int)(position.getX()/PNMLParameter.getScaleForViewInProM()+size.getWidth()/2.0), (int)(position.getY()/PNMLParameter.getScaleForViewInProM()+size.getHeight()/2.0)));
		}
		if (size != null){
			graphics.setDimension(new PNMLPoint(size.width, size.height));
		}
		return graphics;
	}
	private PNMLGraphics getArcGraphics(List<Point2D> edgePoints) {
		PNMLGraphics graphics = new PNMLGraphics();
		graphics.setPosition(new ArrayList<PNMLPoint>());
		for (Point2D point : edgePoints){
			PNMLPoint p = new PNMLPoint(Math.ceil(point.getX()/PNMLParameter.getScaleForViewInProM()), Math.ceil(point.getY()/PNMLParameter.getScaleForViewInProM()));
			graphics.getPosition().add(p);
		}
		return graphics;
	}
}
