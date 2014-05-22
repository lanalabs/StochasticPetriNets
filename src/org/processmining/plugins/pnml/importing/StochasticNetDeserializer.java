package org.processmining.plugins.pnml.importing;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.AbstractGraphElement;
import org.processmining.models.graphbased.AbstractGraphNode;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.AttributeMapOwner;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.configurable.impl.LayoutUtils;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.PNMLParameter;
import org.processmining.plugins.pnml.simple.AbstractPNMLElement;
import org.processmining.plugins.pnml.simple.PNMLArc;
import org.processmining.plugins.pnml.simple.PNMLMarking;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLPage;
import org.processmining.plugins.pnml.simple.PNMLPlace;
import org.processmining.plugins.pnml.simple.PNMLPlaceRef;
import org.processmining.plugins.pnml.simple.PNMLPoint;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.pnml.simple.PNMLToolSpecific;
import org.processmining.plugins.pnml.simple.PNMLTransition;

public class StochasticNetDeserializer {

	public StochasticNetDeserializer(){
	}
	
	public Object[] convertToNet(PluginContext context, PNMLRoot pnml, String filename, boolean addConnections){
		PNMLNet pnmlNet = null;
		PNMLMarking pnmlFinalMarking = null;
		if (pnml.getModule() != null && pnml.getModule().size()>0){
			pnmlNet = pnml.getModule().get(0).getNets().get(0);
			pnmlFinalMarking = pnml.getModule().get(0).getFinalmarkings().get(0);
		} else if (pnml.getNet() != null && pnml.getNet().size()>0){
			pnmlNet = pnml.getNet().get(0);
			if (pnml.getFinalMarkings()!=null){
				pnmlFinalMarking = pnml.getFinalMarkings().getMarkings().get(0);
			}
		}
		
		if (pnml.getNet()!=null && pnml.getNet().size() > 1){
			System.out.println("More than one net in PNML - only converting the first one.");
		}
		
		Marking initialMarking = new Marking();
		Marking finalMarking = null;
		
		StochasticNet net = new StochasticNetImpl(pnmlNet.getId()+" imported from ("+filename+")");
		
		GraphLayoutConnection layout = new GraphLayoutConnection(net);
		
		Map<String, Object> objects = new HashMap<String, Object>();
		
		Point2D offset = new Point2D.Double(0,0);
		if (pnmlNet.getToolspecific()!=null && !pnmlNet.getToolspecific().isEmpty()){
			PNMLToolSpecific toolSpecific = pnmlNet.getToolspecific().get(0);
			if (toolSpecific.getTool().equals(PNMLToolSpecific.STOCHASTIC_ANNOTATION)){
				if (toolSpecific.getProperties()!= null){
					if (toolSpecific.getProperties().containsKey(PNMLToolSpecific.EXECUTION_POLICY)){
						net.setExecutionPolicy(ExecutionPolicy.fromString(toolSpecific.getProperties().get(PNMLToolSpecific.EXECUTION_POLICY)));
					}
					if (toolSpecific.getProperties().containsKey(PNMLToolSpecific.TIME_UNIT)){
						net.setTimeUnit(TimeUnit.fromString(toolSpecific.getProperties().get(PNMLToolSpecific.TIME_UNIT)));
					}
				}
			}
		}
		
		Collection<PNMLPage> pages = new LinkedList<>();
		if (pnmlNet.getPage()!=null && pnmlNet.getPage().size() > 0){
			pages.addAll(pnmlNet.getPage());
		} else {
			pages.add(pnmlNet);
		}
		
		for (PNMLPage page: pages){
			Dimension2D size = getSize(page);
			Point2D position = getPosition(page, offset);
			
			if (position != null){
				layout.setSize(net, size);
				layout.setPosition(net, position);
//				offset.setLocation(offset.getX()+position.getX(),offset.getY()+position.getY());
			}
			

			// add places and transitions
			for (Object o : page.getList()){
				String key = null;
				if (o instanceof PNMLTransition){
					PNMLTransition transition = (PNMLTransition) o;
					key = transition.getId();
					PNMLToolSpecific stochasticAnnotation = null;
					if (transition.getToolspecific() != null){
						for (PNMLToolSpecific spec : transition.getToolspecific()){
							if(spec.getProperties() != null && PNMLToolSpecific.STOCHASTIC_ANNOTATION.equals(spec.getTool())){
								stochasticAnnotation = spec;
							}
						}
					}
					if (stochasticAnnotation != null){
						int priority = Integer.parseInt(stochasticAnnotation.getProperties().get(PNMLToolSpecific.PRIORITY));
						double weight = Double.parseDouble(stochasticAnnotation.getProperties().get(PNMLToolSpecific.WEIGHT));
						DistributionType type = DistributionType.fromString(stochasticAnnotation.getProperties().get(PNMLToolSpecific.DISTRIBUTION_TYPE));
						String parametersString = stochasticAnnotation.getProperties().get(PNMLToolSpecific.DISTRIBUTION_PARAMETERS);
						double[] parameters = null;
						if (parametersString != null && !parametersString.isEmpty()){
							String[] stringParameters = parametersString.split(PNMLToolSpecific.VALUES_SEPARATOR);
							parameters = new double[stringParameters.length];
							for (int i = 0; i < stringParameters.length; i++){
								double val = 0;
								try{
									val = Double.parseDouble(stringParameters[i]);
								} catch (NumberFormatException nfe){
									val = 0;
								}
								parameters[i] = val;
							}
						}
						if (type.equals(DistributionType.IMMEDIATE)){
							objects.put(key, net.addImmediateTransition(getName(transition), weight, priority));
						} else {
							objects.put(key, net.addTimedTransition(getName(transition), weight, type, parameters));
						}
					} else {
						objects.put(key, net.addTransition(getName(transition)));
					}
				}
				if (o instanceof PNMLPlace){
					PNMLPlace place = (PNMLPlace) o;
					key = place.getId();
					Place p = net.addPlace(place.getName()!=null&&place.getName().getValue()!=null?place.getName().getValue():place.getId());
					objects.put(key, p);
					if (place.getInitialMarking() != null && Integer.valueOf(place.getInitialMarking().getText())>0){
						initialMarking.add(p,Integer.valueOf(place.getInitialMarking().getText()));
					}
				}
				
				if (key != null) {
					((AbstractGraphNode)objects.get(key)).getAttributeMap().put("id", key);
					Point2D pos = getPosition((AbstractPNMLElement) o,offset);
					Dimension2D dim = getSize((AbstractPNMLElement) o);
					if (pos != null){
						layout.setPosition((AttributeMapOwner) objects.get(key), pos);
					}
					if (dim != null){
						layout.setSize((AttributeMapOwner) objects.get(key), dim);
						((AbstractGraphElement)objects.get(key)).getAttributeMap().put(AttributeMap.SIZE, dim);
					}
				}
			}
			
			// add arcs
			for (Object o : page.getList()){
				if (o instanceof PNMLArc){
					PNMLArc arc = (PNMLArc) o;
					
					Object source = objects.get(arc.getSource());
					Object target = objects.get(arc.getTarget());
					Arc a = null;
					if (source instanceof Transition && target instanceof Place){
						a = net.addArc((Transition)source, (Place)target);
					} else if (source instanceof Place && target instanceof Transition){
						a = net.addArc((Place)source,(Transition)target);
					}
					if (arc.getGraphics() != null && arc.getGraphics().getPosition() != null){
						List<PNMLPoint> points = arc.getGraphics().getPosition();
						layout.setEdgePoints(a, getEdgePoints(points, offset));
					}
				}
			}
		}
		if (addConnections && context != null){
			InitialMarkingConnection conn = new InitialMarkingConnection(net, initialMarking);
			context.addConnection(conn);
			LayoutUtils.setLayout(net, layout);
			context.addConnection(layout);
			if (pnmlFinalMarking != null){
				finalMarking = new Marking();
				for (PNMLPlaceRef placeRef : pnmlFinalMarking.getPlaces()){
					Place p = (Place) objects.get(placeRef.getIdRef());
					finalMarking.add(p,placeRef.getTokens());
				}
				FinalMarkingConnection fConn = new FinalMarkingConnection(net, finalMarking);
				context.addConnection(fConn);
			}
		}
		
		return new Object[]{net,initialMarking,finalMarking};
	}
	
	/**
	 * Converts PNML points to edge points of an arc in the {@link PetrinetGraph}.
	 * @param points list of {@link PNMLPoint}s
	 * @param offset the offset of the page
	 * @return
	 */
	private List<Point2D> getEdgePoints(List<PNMLPoint> points,Point2D offset) {
		if (points == null){
			return null;
		}
		List<Point2D> pointArray = new ArrayList<Point2D>();
		for (PNMLPoint p : points){
			pointArray.add(new Point2D.Double((p.getX()+offset.getX())*PNMLParameter.getScaleForViewInProM(),(p.getY()+offset.getY())*PNMLParameter.getScaleForViewInProM()));
		}
		return pointArray;
	}

	private Point2D getPosition(AbstractPNMLElement element, Point2D offset) {
		Dimension2D size = getSize(element);
		if (element.getGraphics()!= null && element.getGraphics().getPosition()!= null){
			List<PNMLPoint> positions = element.getGraphics().getPosition();
			Point2D position = new Point2D.Double(0,0);
			if (positions.size() == 1){
				position = new Point2D.Double(element.getGraphics().getPosition().get(0).getX()*PNMLParameter.getScaleForViewInProM(),element.getGraphics().getPosition().get(0).getY()*PNMLParameter.getScaleForViewInProM());
			}
			if (size != null){
				position.setLocation(position.getX() - size.getWidth()/2, position.getY()-size.getHeight()/2);
			}
			if (offset != null){
				position.setLocation(position.getX() +offset.getX(), position.getY()+offset.getY());
			}
			return position;
		}
		return null;
	}

	private Dimension2D getSize(AbstractPNMLElement element) {
		if (element.getGraphics()!= null && element.getGraphics().getDimension()!= null){
			return new Dimension((int)(element.getGraphics().getDimension().getX()*PNMLParameter.getScaleForViewInProM()),(int)(element.getGraphics().getDimension().getY()*PNMLParameter.getScaleForViewInProM()));
		}
		return null;
	}

	private String getName(PNMLTransition transition) {
		return transition.getName()==null?transition.getId()==null?"unnamed":transition.getId():transition.getName().getValue();
	}

	public class StochasticNetVisitor{
		
	}
}
