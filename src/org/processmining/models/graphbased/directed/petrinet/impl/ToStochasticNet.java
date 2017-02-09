package org.processmining.models.graphbased.directed.petrinet.impl;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.*;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.BernsteinExponentialApproximation;

import java.util.HashMap;
import java.util.Map;

public class ToStochasticNet {

    public static final DistributionType[] SUPPORTED_CONVERSION_TYPES = new DistributionType[]{DistributionType.EXPONENTIAL, DistributionType.NORMAL, DistributionType.BERNSTEIN_EXPOLYNOMIAL};

    @PluginVariant(variantLabel = "From any Marked Petrinet", requiredParameterLabels = {0, 1})
    public static Object[] fromPetrinet(PluginContext context, PetrinetGraph net, Marking marking)
            throws ConnectionCannotBeObtained {
        if (marking != null) {
            if (context != null) {
                // Check for connection
                context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net, marking);
            }
        }
        Object[] transformed = fromPetriNetExternal(context, net, marking);
        if (context != null) {
            context.getFutureResult(0).setLabel(net.getLabel());
            context.getFutureResult(1).setLabel("Initial Marking of " + net.getLabel());
        }
        return transformed;
    }

    public static Object[] fromPetriNetExternal(PluginContext context, PetrinetGraph net, Marking marking) {
        StochasticNetImpl newNet = new StochasticNetImpl(net.getLabel());
        return cloneFromNet(context, net, marking, newNet);
    }

    public static Object[] asPetriNet(PluginContext context, StochasticNet net, Marking marking) {
        PetrinetImpl newNet = new PetrinetImpl(net.getLabel());
        return cloneFromNet(context, net, marking, newNet);
    }

    protected static Object[] cloneFromNet(PluginContext context, PetrinetGraph net, Marking marking,
                                           AbstractResetInhibitorNet newNet) {
        Map<DirectedGraphElement, DirectedGraphElement> mapping = newNet.cloneFrom(net);

        Marking newMarking = ToResetInhibitorNet.cloneMarking(marking, mapping);

        if (context != null) {
            context.addConnection(new InitialMarkingConnection((PetrinetGraph) newNet, newMarking));
        }
        return new Object[]{newNet, newMarking};
    }


    public static Object[] fromStochasticNet(PluginContext context, StochasticNet net, Marking marking) {
        StochasticNetImpl newNet = new StochasticNetImpl(net.getLabel());
        newNet.setExecutionPolicy(net.getExecutionPolicy());
        newNet.setTimeUnit(net.getTimeUnit());
        Map<DirectedGraphElement, DirectedGraphElement> mapping = newNet.cloneFrom(net);

        Marking newMarking = ToResetInhibitorNet.cloneMarking(marking, mapping);

        for (Transition t : net.getTransitions()) {
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                TimedTransition target = (TimedTransition) mapping.get(t);
                target.setDistributionType(tt.getDistributionType());
                target.setDistributionParameters((tt.getDistributionParameters() != null ? tt.getDistributionParameters().clone() : null));
                target.setPriority(tt.getPriority());
                target.setWeight(tt.getWeight());
                target.setTrainingData(tt.getTrainingData());
                target.setDistribution(target.initDistribution(0));
            }
        }

        if (context != null) {
            context.addConnection(new InitialMarkingConnection(newNet, newMarking));
        }


        return new Object[]{newNet, newMarking};
    }

    /**
     * Converts all timed transitions (except immediate and deterministic transitions) to the specified type in the net.
     *
     * @param context
     * @param net
     * @param marking
     * @param type
     * @return
     */
    public static Object[] convertStochasticNetToType(PluginContext context, StochasticNet net, Marking marking, DistributionType type) {
        StochasticNetImpl newNet = new StochasticNetImpl(net.getLabel());
        newNet.setExecutionPolicy(net.getExecutionPolicy());
        newNet.setTimeUnit(net.getTimeUnit());
        Map<DirectedGraphElement, DirectedGraphElement> mapping = newNet.cloneFrom(net);

        Marking newMarking = ToResetInhibitorNet.cloneMarking(marking, mapping);

        for (Transition t : net.getTransitions()) {
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                TimedTransition target = (TimedTransition) mapping.get(t);

                switch (tt.getDistributionType()) {
                    case IMMEDIATE:
                    case DETERMINISTIC:
                        target.setDistributionType(tt.getDistributionType());
                        target.setDistributionParameters((tt.getDistributionParameters() != null ? tt.getDistributionParameters().clone() : null));
                        target.setPriority(tt.getPriority());
                        target.setWeight(tt.getWeight());
                        target.setTrainingData(tt.getTrainingData());
                        target.setDistribution(target.initDistribution(0));
                        break;
                    default:
                        switch (type) {
                            case BERNSTEIN_EXPOLYNOMIAL:
                                target.setDistributionType(type);
                                BernsteinExponentialApproximation bea = new BernsteinExponentialApproximation(
                                        tt.getDistribution(), 0.0, tt.getDistribution().getSupportUpperBound());
                                target.setDistributionParameters(bea.getParameters());
                                target.setPriority(tt.getPriority());
                                target.setWeight(tt.getWeight());
                                target.setTrainingData(tt.getTrainingData());
                                target.setDistribution(bea);
                                break;
                            case NORMAL:
                                target.setDistributionType(type);
                                target.setDistributionParameters(tt.getDistribution().getNumericalMean(),
                                        Math.sqrt(tt.getDistribution().getNumericalVariance()));
                                target.setPriority(tt.getPriority());
                                target.setWeight(tt.getWeight());
                                target.setTrainingData(tt.getTrainingData());
                                target.setDistribution(target.initDistribution(0));
                                break;
                            case EXPONENTIAL:
                                target.setDistributionType(type);
                                target.setDistributionParameters(tt.getDistribution().getNumericalMean());
                                target.setPriority(tt.getPriority());
                                target.setWeight(tt.getWeight());
                                target.setTrainingData(tt.getTrainingData());
                                target.setDistribution(target.initDistribution(0));
                                break;
                            default:
                                throw new IllegalArgumentException("Type " + type + " not supported yet for conversion");
                        }
                        break;
                }
            }
        }

        if (context != null) {
            context.addConnection(new InitialMarkingConnection(newNet, newMarking));
        }

        return new Object[]{newNet, newMarking};
    }


    /**
     * Adopted from exportPN2DOT method from the EventToActivityMatcher plugin
     *
     * @param net
     * @author Thomas Baier, Andreas Rogge-Solti
     */
    public static String convertPetrinetToDOT(Petrinet net) {
        String lsep = System.getProperty("line.separator");

        String resultString = "digraph G { " + lsep;
        resultString += "ranksep=\".3\"; fontsize=\"14\"; remincross=true; margin=\"0.0,0.0\"; fontname=\"Arial\";rankdir=\"LR\";" + lsep;
        resultString += "edge [arrowsize=\"0.5\"];\n";
        resultString += "node [height=\".2\",width=\".2\",fontname=\"Arial\",fontsize=\"14\"];\n";
        resultString += "ratio=0.4;" + lsep;

        Map<PetrinetNode, String> idMapping = new HashMap<>();
        int id = 1;
        for (Transition tr : net.getTransitions()) {

            String label = tr.getLabel();
            String shape = "shape=\"box\"";
            if (tr instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) tr;
                label += "\\n" + StochasticNetUtils.printDistribution(tt.getDistribution());
                if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                    shape += ",margin=\"0, 0.1\"";
                }
            }
            if (tr.isInvisible()) {
                shape += ",color=\"black\",fontcolor=\"white\"";
            }
            id = checkId(tr, idMapping, id);
            resultString += idMapping.get(tr) + " [" + shape + ",label=\"" + label + "\",style=\"filled\"];" + lsep;
        }


        // Places
        for (Place place : net.getPlaces()) {
            id = checkId(place, idMapping, id);
            resultString += idMapping.get(place) + " [shape=\"circle\",label=\"\"];" + lsep;
        }

        // Edges
        for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : net.getEdges()) {
            id = checkId(edge.getSource(), idMapping, id);
            id = checkId(edge.getTarget(), idMapping, id);

            String edgeString = idMapping.get(edge.getSource()) + " -> " + idMapping.get(edge.getTarget());
            resultString += edgeString + lsep;
        }

        resultString += "}";

        return resultString;
    }

    private static int checkId(PetrinetNode node, Map<PetrinetNode, String> idMapping, int currentCounter) {
        if (!idMapping.containsKey(node)) {
            idMapping.put(node, String.valueOf("id" + (currentCounter++)));
        }
        return currentCounter;
    }
}
