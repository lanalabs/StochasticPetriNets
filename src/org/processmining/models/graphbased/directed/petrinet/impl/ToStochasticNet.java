package org.processmining.models.graphbased.directed.petrinet.impl;

import java.util.Map;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.distribution.BernsteinExponentialApproximation;

public class ToStochasticNet {
	
	public static final DistributionType[] SUPPORTED_CONVERSION_TYPES = new DistributionType[]{DistributionType.NORMAL, DistributionType.BERNSTEIN_EXPOLYNOMIAL};

	@PluginVariant(variantLabel = "From any Marked Petrinet", requiredParameterLabels = { 0, 1 })
	public static Object[] fromPetrinet(PluginContext context, PetrinetGraph net, Marking marking)
			throws ConnectionCannotBeObtained {
		if (marking != null) {
			// Check for connection
			context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, net, marking);
		}
		Object[] transformed = fromPetriNetExternal(context, net, marking);
		context.getFutureResult(0).setLabel(net.getLabel());
		context.getFutureResult(1).setLabel("Initial Marking of " + net.getLabel());
		return transformed;
	}
	
	public static Object[] fromPetriNetExternal(PluginContext context, PetrinetGraph net, Marking marking){
		StochasticNetImpl newNet = new StochasticNetImpl(net.getLabel());
		Map<DirectedGraphElement, DirectedGraphElement> mapping = newNet.cloneFrom(net);

		Marking newMarking = ToResetInhibitorNet.cloneMarking(marking, mapping);
		
		if (context != null){
			context.addConnection(new InitialMarkingConnection(newNet, newMarking));
		}
		

		return new Object[] { newNet, newMarking };
	}
	public static Object[] fromStochasticNet(PluginContext context, StochasticNet net, Marking marking){
		StochasticNetImpl newNet = new StochasticNetImpl(net.getLabel());
		newNet.setExecutionPolicy(net.getExecutionPolicy());
		newNet.setTimeUnit(net.getTimeUnit());
		Map<DirectedGraphElement, DirectedGraphElement> mapping = newNet.cloneFrom(net);

		Marking newMarking = ToResetInhibitorNet.cloneMarking(marking, mapping);
		
		for (Transition t : net.getTransitions()){
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				TimedTransition target = (TimedTransition) mapping.get(t);
				target.setDistributionType(tt.getDistributionType());
				target.setDistributionParameters((tt.getDistributionParameters()!=null?tt.getDistributionParameters().clone():null));
				target.setPriority(tt.getPriority());
				target.setWeight(tt.getWeight());
				target.setTrainingData(tt.getTrainingData());
				target.initDistribution(0);
			}
		}
		
		if (context != null){
			context.addConnection(new InitialMarkingConnection(newNet, newMarking));
		}
		

		return new Object[] { newNet, newMarking };
	}
	
	public static Object[] convertStochasticNetToType(PluginContext context, StochasticNet net, Marking marking, DistributionType type){
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
					case IMMEDIATE :
					case DETERMINISTIC :
						target.setDistributionType(tt.getDistributionType());
						target.setDistributionParameters((tt.getDistributionParameters() != null ? tt.getDistributionParameters().clone() : null));
						target.setPriority(tt.getPriority());
						target.setWeight(tt.getWeight());
						target.setTrainingData(tt.getTrainingData());
						target.initDistribution(0);
						break;
					default :
						switch (type) {
							case BERNSTEIN_EXPOLYNOMIAL :
								target.setDistributionType(type);
								BernsteinExponentialApproximation bea = new BernsteinExponentialApproximation(
										tt.getDistribution(), 0.0, tt.getDistribution().getSupportUpperBound());
								target.setDistributionParameters(bea.getParameters());
								target.setPriority(tt.getPriority());
								target.setWeight(tt.getWeight());
								target.setTrainingData(tt.getTrainingData());
								target.setDistribution(bea);
								break;
							case NORMAL :
								target.setDistributionType(type);
								target.setDistributionParameters(tt.getDistribution().getNumericalMean(),
										Math.sqrt(tt.getDistribution().getNumericalVariance()));
								target.setPriority(tt.getPriority());
								target.setWeight(tt.getWeight());
								target.setTrainingData(tt.getTrainingData());
								target.initDistribution(0);
								break;
							default :
								throw new IllegalArgumentException("Type " + type + " not supported yet for conversion");
						}
						break;
				}
			}
		}
		
		if (context != null){
			context.addConnection(new InitialMarkingConnection(newNet, newMarking));
		}
		
		return new Object[] { newNet, newMarking };
	}
}
