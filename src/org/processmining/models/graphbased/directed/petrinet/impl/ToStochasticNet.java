package org.processmining.models.graphbased.directed.petrinet.impl;

import java.util.Map;

import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;

public class ToStochasticNet {

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
				target.initDistribution(0);
			}
		}
		
		if (context != null){
			context.addConnection(new InitialMarkingConnection(newNet, newMarking));
		}
		

		return new Object[] { newNet, newMarking };
	}
}
