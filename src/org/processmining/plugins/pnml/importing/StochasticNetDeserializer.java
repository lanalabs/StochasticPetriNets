package org.processmining.plugins.pnml.importing;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.helper.UserCancelledException;
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
import org.processmining.plugins.pnml.simple.*;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class StochasticNetDeserializer {

    public StochasticNetDeserializer() {
    }

    class ExecutionPolicyDeterminer implements Runnable {
        StochasticNet net;

        public ExecutionPolicyDeterminer(StochasticNet net) {
            this.net = net;
        }

        public void run() {
            Object result = JOptionPane.showInputDialog(null, "Select execution policy for net " + net.getLabel(),
                    "Execution policy not specified", JOptionPane.QUESTION_MESSAGE, null, ExecutionPolicy.values(),
                    ExecutionPolicy.RACE_ENABLING_MEMORY);
            if (result != null) {
                net.setExecutionPolicy((ExecutionPolicy) result);
            }
        }
    }

    ;

    class TimeUnitDeterminer implements Runnable {
        StochasticNet net;

        public TimeUnitDeterminer(StochasticNet net) {
            this.net = net;
        }

        public void run() {
            Object result = JOptionPane.showInputDialog(null, "Select time unit for net " + net.getLabel(), "Time unit not specified",
                    JOptionPane.QUESTION_MESSAGE, null, TimeUnit.values(), TimeUnit.MINUTES);
            if (result != null) {
                net.setTimeUnit((TimeUnit) result);
            }
        }
    }

    ;


    public Object[] convertToNet(PluginContext context, PNMLRoot pnml, String filename, boolean addConnections) throws UserCancelledException {
        PNMLNet pnmlNet = null;
        PNMLMarking pnmlFinalMarking = null;
        if (pnml.getModule() != null && pnml.getModule().size() > 0) {
            pnmlNet = pnml.getModule().get(0).getNets().get(0);
            pnmlFinalMarking = pnml.getModule().get(0).getFinalmarkings().get(0);
        } else if (pnml.getNet() != null && pnml.getNet().size() > 0) {
            pnmlNet = pnml.getNet().get(0);
            if (pnml.getFinalMarkings() != null) {
                pnmlFinalMarking = pnml.getFinalMarkings().getMarkings().get(0);
            }
        }

        if (pnml.getNet() != null && pnml.getNet().size() > 1) {
            String message = "More than one net in PNML - only converting the first one.";
            log(context, message);
        }

        Marking initialMarking = new Marking();
        Marking finalMarking = null;

        StochasticNet net = new StochasticNetImpl(pnmlNet.getId() + " imported from (" + filename + ")");

        GraphLayoutConnection layout = new GraphLayoutConnection(net);

        Map<String, Object> objects = new HashMap<String, Object>();

        Point2D offset = new Point2D.Double(0, 0);
        boolean containsExecutionPolicy = false, containsTimeUnit = false;

        if (pnmlNet.getToolspecific() != null && !pnmlNet.getToolspecific().isEmpty()) {
            PNMLToolSpecific toolSpecific = pnmlNet.getToolspecific().get(0);
            if (toolSpecific.getTool().equals(PNMLToolSpecific.STOCHASTIC_ANNOTATION)) {
                if (toolSpecific.getProperties() != null) {
                    if (toolSpecific.getProperties().containsKey(PNMLToolSpecific.EXECUTION_POLICY)) {
                        net.setExecutionPolicy(ExecutionPolicy.fromString(toolSpecific.getProperties().get(PNMLToolSpecific.EXECUTION_POLICY)));
                        containsExecutionPolicy = true;
                    }
                    if (toolSpecific.getProperties().containsKey(PNMLToolSpecific.TIME_UNIT)) {
                        net.setTimeUnit(TimeUnit.fromString(toolSpecific.getProperties().get(PNMLToolSpecific.TIME_UNIT)));
                        containsTimeUnit = true;
                    }
                }
            }
        }
        if (!containsExecutionPolicy) {
            if (context instanceof UIPluginContext) {
                try {
                    SwingUtilities.invokeLater(new ExecutionPolicyDeterminer(net));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
                log(context, "Assuming race enabling memory for net " + net.getLabel());
            }
        }
        if (!containsTimeUnit) {
            if (context instanceof UIPluginContext) {
                try {
                    SwingUtilities.invokeLater(new TimeUnitDeterminer(net));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                net.setTimeUnit(TimeUnit.MINUTES);
                log(context, "Assuming 'minutes' as the time unit in net " + net.getLabel());
            }
        }

        Collection<PNMLPage> pages = new LinkedList<PNMLPage>();
        if (pnmlNet.getPage() != null && pnmlNet.getPage().size() > 0) {
            pages.addAll(pnmlNet.getPage());
        } else {
            pages.add(pnmlNet);
        }

        for (PNMLPage page : pages) {
            Dimension2D size = getSize(page);
            Point2D position = getPosition(page, offset);

            if (position != null) {
                layout.setSize(net, size);
                layout.setPosition(net, position);
            }


            // add places and transitions
            for (Object o : page.getList()) {
                String key = null;
                if (o instanceof PNMLTransition) {
                    boolean invisible = false;

                    PNMLTransition transition = (PNMLTransition) o;
                    key = transition.getId();
                    PNMLToolSpecific stochasticAnnotation = null;
                    if (transition.getToolspecific() != null) {
                        for (PNMLToolSpecific spec : transition.getToolspecific()) {
                            if (spec.getProperties() != null && PNMLToolSpecific.STOCHASTIC_ANNOTATION.equals(spec.getTool())) {
                                stochasticAnnotation = spec;
                            } else if (PNMLToolSpecific.PROM.equals(spec.getTool())) {
                                String activity = spec.getActivity();
                                if (activity != null && activity.equals("$invisible$")) {
                                    invisible = true;
                                }
                            }
                        }
                    }
                    if (stochasticAnnotation != null) {

                        if (stochasticAnnotation.getProperties().containsKey(PNMLToolSpecific.PRIORITY)) {
                            int priority = Integer.parseInt(stochasticAnnotation.getProperties().get(PNMLToolSpecific.PRIORITY));
                            double weight = Double.parseDouble(stochasticAnnotation.getProperties().get(PNMLToolSpecific.WEIGHT));
                            invisible = stochasticAnnotation.getProperties().containsKey(PNMLToolSpecific.INVISIBLE) ? Boolean.parseBoolean(stochasticAnnotation.getProperties().get(PNMLToolSpecific.INVISIBLE)) : false;
                            DistributionType type = DistributionType.fromString(stochasticAnnotation.getProperties().get(PNMLToolSpecific.DISTRIBUTION_TYPE));
                            String parametersString = stochasticAnnotation.getProperties().get(PNMLToolSpecific.DISTRIBUTION_PARAMETERS);
                            String trainingData = stochasticAnnotation.getProperties().get(PNMLToolSpecific.TRAINING_DATA);
                            double[] parameters = null;
                            if (parametersString != null && !parametersString.isEmpty()) {
                                String[] stringParameters = parametersString.split(PNMLToolSpecific.VALUES_SEPARATOR);
                                parameters = new double[stringParameters.length];
                                for (int i = 0; i < stringParameters.length; i++) {
                                    double val = 0;
                                    try {
                                        val = Double.parseDouble(stringParameters[i]);
                                    } catch (NumberFormatException nfe) {
                                        val = 0;
                                    }
                                    parameters[i] = val;
                                }
                            }
                            if (type.equals(DistributionType.IMMEDIATE)) {
                                objects.put(key, net.addImmediateTransition(getName(transition), weight, priority, trainingData));
                            } else {
                                objects.put(key, net.addTimedTransition(getName(transition), weight, type, trainingData, parameters));
                            }
                        } else {
                            objects.put(key, net.addTransition(getName(transition)));
                            invisible |= stochasticAnnotation.getProperties().containsKey(PNMLToolSpecific.INVISIBLE) ? Boolean.parseBoolean(stochasticAnnotation.getProperties().get(PNMLToolSpecific.INVISIBLE)) : false;
                        }
                    } else {
                        objects.put(key, net.addTransition(getName(transition)));
                    }
                    ((Transition) objects.get(key)).setInvisible(invisible);
                }
                if (o instanceof PNMLPlace) {
                    PNMLPlace place = (PNMLPlace) o;
                    key = place.getId();
                    Place p = net.addPlace(place.getName() != null && place.getName().getValue() != null ? place.getName().getValue() : place.getId());
                    objects.put(key, p);
                    if (place.getInitialMarking() != null && Integer.valueOf(place.getInitialMarking().getText()) > 0) {
                        initialMarking.add(p, Integer.valueOf(place.getInitialMarking().getText()));
                    }
                }

                if (key != null) {
                    ((AbstractGraphNode) objects.get(key)).getAttributeMap().put("id", key);
                    Point2D pos = getPosition((AbstractPNMLElement) o, offset);
                    Dimension2D dim = getSize((AbstractPNMLElement) o);
                    if (pos != null) {
                        layout.setPosition((AttributeMapOwner) objects.get(key), pos);
                    }
                    if (dim != null) {
                        layout.setSize((AttributeMapOwner) objects.get(key), dim);
                        ((AbstractGraphElement) objects.get(key)).getAttributeMap().put(AttributeMap.SIZE, dim);
                    }
                }
            }

            // add arcs
            for (Object o : page.getList()) {
                if (o instanceof PNMLArc) {
                    PNMLArc arc = (PNMLArc) o;

                    Object source = objects.get(arc.getSource());
                    Object target = objects.get(arc.getTarget());
                    Arc a = null;
                    if (source instanceof Transition && target instanceof Place) {
                        a = net.addArc((Transition) source, (Place) target);
                    } else if (source instanceof Place && target instanceof Transition) {
                        a = net.addArc((Place) source, (Transition) target);
                    }
                    if (arc.getGraphics() != null && arc.getGraphics().getPosition() != null) {
                        List<PNMLPoint> points = arc.getGraphics().getPosition();
                        layout.setEdgePoints(a, getEdgePoints(points, offset));
                    }
                }
            }
        }

        if (initialMarking.isEmpty()) {
            initialMarking = StochasticNetUtils.getInitialMarking(context, net);
        }

        if (addConnections && context != null) {
            InitialMarkingConnection conn = new InitialMarkingConnection(net, initialMarking);
            context.addConnection(conn);
            LayoutUtils.setLayout(net, layout);
            context.addConnection(layout);
            if (pnmlFinalMarking != null) {
                finalMarking = new Marking();
                for (PNMLPlaceRef placeRef : pnmlFinalMarking.getPlaces()) {
                    Place p = (Place) objects.get(placeRef.getIdRef());
                    finalMarking.add(p, placeRef.getTokens());
                }
                FinalMarkingConnection fConn = new FinalMarkingConnection(net, finalMarking);
                context.addConnection(fConn);
            }
        }

        return new Object[]{net, initialMarking, finalMarking};
    }

    private void log(PluginContext context, String message) {
        if (context != null) {
            context.log(message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Converts PNML points to edge points of an arc in the {@link PetrinetGraph}.
     *
     * @param points list of {@link PNMLPoint}s
     * @param offset the offset of the page
     * @return
     */
    private List<Point2D> getEdgePoints(List<PNMLPoint> points, Point2D offset) {
        if (points == null) {
            return null;
        }
        List<Point2D> pointArray = new ArrayList<Point2D>();
        for (PNMLPoint p : points) {
            pointArray.add(new Point2D.Double((p.getX() + offset.getX()) * PNMLParameter.getScaleForViewInProM(), (p.getY() + offset.getY()) * PNMLParameter.getScaleForViewInProM()));
        }
        return pointArray;
    }

    private Point2D getPosition(AbstractPNMLElement element, Point2D offset) {
        Dimension2D size = getSize(element);
        if (element.getGraphics() != null && element.getGraphics().getPosition() != null) {
            List<PNMLPoint> positions = element.getGraphics().getPosition();
            Point2D position = new Point2D.Double(0, 0);
            if (positions.size() == 1) {
                position = new Point2D.Double(element.getGraphics().getPosition().get(0).getX() * PNMLParameter.getScaleForViewInProM(), element.getGraphics().getPosition().get(0).getY() * PNMLParameter.getScaleForViewInProM());
            }
            if (size != null) {
                position.setLocation(position.getX() - size.getWidth() / 2, position.getY() - size.getHeight() / 2);
            }
            if (offset != null) {
                position.setLocation(position.getX() + offset.getX(), position.getY() + offset.getY());
            }
            return position;
        }
        return null;
    }

    private Dimension2D getSize(AbstractPNMLElement element) {
        if (element.getGraphics() != null && element.getGraphics().getDimension() != null) {
            return new Dimension((int) (element.getGraphics().getDimension().getX() * PNMLParameter.getScaleForViewInProM()), (int) (element.getGraphics().getDimension().getY() * PNMLParameter.getScaleForViewInProM()));
        }
        return null;
    }

    private String getName(PNMLTransition transition) {
        return transition.getName() == null ? transition.getId() == null ? "unnamed" : transition.getId() : transition.getName().getValue();
    }

    public class StochasticNetVisitor {

    }
}
