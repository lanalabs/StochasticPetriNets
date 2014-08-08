package org.processmining.tests.plugins.stochasticnet.data.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.tests.plugins.stochasticnet.data.IbmCallToProcess;
import org.processmining.tests.plugins.stochasticnet.data.IbmCallToService;
import org.processmining.tests.plugins.stochasticnet.data.IbmCallToTask;
import org.processmining.tests.plugins.stochasticnet.data.IbmConnection;
import org.processmining.tests.plugins.stochasticnet.data.IbmDecision;
import org.processmining.tests.plugins.stochasticnet.data.IbmEndNode;
import org.processmining.tests.plugins.stochasticnet.data.IbmFork;
import org.processmining.tests.plugins.stochasticnet.data.IbmHumanTask;
import org.processmining.tests.plugins.stochasticnet.data.IbmInput;
import org.processmining.tests.plugins.stochasticnet.data.IbmJoin;
import org.processmining.tests.plugins.stochasticnet.data.IbmMerge;
import org.processmining.tests.plugins.stochasticnet.data.IbmOutput;
import org.processmining.tests.plugins.stochasticnet.data.IbmOutputBranch;
import org.processmining.tests.plugins.stochasticnet.data.IbmProcess;
import org.processmining.tests.plugins.stochasticnet.data.IbmStartNode;
import org.processmining.tests.plugins.stochasticnet.data.IbmTask;

public class IbmToStochasticNetConverter {
	public static final String SEPARATOR = "|";

	public static StochasticNet convertFromIbmProcess(IbmProcess process){
		return convertFromIbmProcess(process, false);
	}
	
	public static StochasticNet convertFromIbmProcess(IbmProcess process, boolean expandSubProcesses){
		StochasticNet net = new StochasticNetImpl(process.getName());
		
		Map<String, Object> pnElementByName = new HashMap<>();
		
		Map<String, IbmOutputBranch> outputBranches = new HashMap<String, IbmOutputBranch>();
		Set<String> forks = new HashSet<String>();
		
		Set<Place> startPlaces = new HashSet<Place>();
		Set<Place> endPlaces = new HashSet<Place>();
		
		// add processes
		for (IbmCallToProcess callProcess : process.getFlowContent().getCallsToProcess()){
			Transition t = addDefaultTimedTransition(net,getProcessName(callProcess.getName()));
			pnElementByName.put(callProcess.getName(), t);
		}
		// add collapsed sub-processes (don't care about the contents for now
		for (IbmProcess subprocess : process.getFlowContent().getProcesses()){
			if (expandSubProcesses){
				StochasticNet newNet = convertFromIbmProcess(subprocess, false);
				Pair<Place,Place> inputAndOutput = addNet(net, newNet);
				pnElementByName.put(subprocess.getName(), inputAndOutput);
			} else {
				Transition t = addDefaultTimedTransition(net,getProcessName(subprocess.getName()));
				pnElementByName.put(subprocess.getName(), t);
			}
		}
		
		// add call tasks
		for (IbmCallToTask callTask : process.getFlowContent().getCallsToTask()){
			Transition t = addDefaultTimedTransition(net,getTaskName(callTask.getName()));
			pnElementByName.put(callTask.getName(), t);
		}
		// add tasks
		for (IbmTask task : process.getFlowContent().getTasks()){
			Transition t = addDefaultTimedTransition(net,getTaskName(task.getName()));
			pnElementByName.put(task.getName(), t);
		}
		// add human tasks
		for (IbmHumanTask task : process.getFlowContent().getHumanTasks()){
			Transition t = addDefaultTimedTransition(net,getTaskName(task.getName()));
			pnElementByName.put(task.getName(), t);
		}
		// add services
		for (IbmCallToService callService : process.getFlowContent().getCallsToService()){
			Transition t = addDefaultTimedTransition(net,getServiceName(callService.getName()));
			pnElementByName.put(callService.getName(), t);
		}
		
		
		// add start nodes
		for (IbmStartNode startNode : process.getFlowContent().getStartNodes()){
			Place startPlace = net.addPlace(getStartNodeName(startNode.getName()));
			pnElementByName.put(startNode.getName(), startPlace);
			startPlaces.add(startPlace);
//			Transition startTransition = net.addImmediateTransition(startNode.getName());
//			pnElementByName.put(startNode.getName(), startTransition);
//			
//			Place transitionPlace = net.addPlace(getPlaceName(startNode.getName()));
//			net.addArc(startPlace, startTransition);
//			net.addArc(startTransition, transitionPlace);
		}
		// add final nodes
		List<IbmEndNode> endIbmNodes = new ArrayList<>(process.getFlowContent().getEndNodes());
		endIbmNodes.addAll(process.getFlowContent().getStopNodes());
		for (IbmEndNode endNode : endIbmNodes){
			Place endPlace = net.addPlace(getEndNodeName(endNode.getName()));
			pnElementByName.put(endNode.getName(), endPlace);
			endPlaces.add(endPlace);
		}
		// add splitting gateways
		for (IbmDecision decision : process.getFlowContent().getDecisions()){
			if (decision.isInclusive()){
				// and split
				System.out.println("todo: handle inclusive or split!");
				throw new IllegalArgumentException("Can't transform models with inclusive OR-splits!");
			}
			Place decisionPlace = net.addPlace(getDecisionName(decision.getName()));
			pnElementByName.put(decision.getName(), decisionPlace);
			
			// create for each arc a transition
			for (IbmOutputBranch outBranch : decision.getOutputBranches()){
				if (outBranch.getOutputs().size() >= 1){
					if (outBranch.getProbability() != null){
						for (IbmOutput output : outBranch.getOutputs()){
							outputBranches.put(decision.getName()+SEPARATOR+output.getName(), outBranch);
						}
					}
				}
//				Transition outTransition = null;
//				if (pnElementByName.containsKey(outBranch.getName())){
//					outTransition = (Transition) pnElementByName.get(outBranch.getName()); 
//				}
//				if (outTransition == null){
//					if (outBranch.getProbability() != null){
//						outTransition = net.addImmediateTransition(outBranch.getName(), Double.valueOf(outBranch.getProbability()));
//					} else {
//						outTransition = net.addImmediateTransition(outBranch.getName(), 100.0/decision.getOutputBranches().size());
//					}
//					pnElementByName.put(outBranch.getName(), outTransition);
//					// add output place for the transition:
//					Place transitionPlace = net.addPlace(getPlaceName(outBranch.getName())); 
//					net.addArc(outTransition, transitionPlace);
//				}
//				net.addArc(decisionPlace, outTransition);
			}
		}
		
		for (IbmMerge merge : process.getFlowContent().getMerges()){
			// assume non-inclusive merges!
			// just join input places to one.
			Place mergePlace = net.addPlace(getMergeName(merge.getName()));
			pnElementByName.put(merge.getName(), mergePlace);
			
//			for (IbmInputBranch inputBranch : merge.getInputBranches()){
//				Transition joinTransition = net.addImmediateTransition("merge_"+inputBranch.getName());
//				pnElementByName.put(joinTransition.getLabel(), joinTransition);
//				net.addArc(joinTransition, mergePlace);
//			}
		}
		
		for (IbmFork fork : process.getFlowContent().getForks()){
			Transition forkTransition = net.addImmediateTransition(fork.getName());
			forkTransition.setInvisible(true);
			pnElementByName.put(fork.getName(), forkTransition);
//			for (IbmOutputBranch outputBranch : fork.getOutputBranches()){
//				Place forkPlace = net.addPlace(outputBranch.getName());
//				pnElementByName.put(outputBranch.getName(), forkPlace);
//				net.addArc(forkTransition, forkPlace);
//			}
			forks.add(fork.getName());
		}
		
		for (IbmJoin join : process.getFlowContent().getJoins()){
			Transition joinTransition = net.addImmediateTransition(join.getName());
			joinTransition.setInvisible(true);
			pnElementByName.put(join.getName(), joinTransition);
//			for (IbmInputBranch inputBranch : join.getInputBranches()){
//				// connect later...
//			}
		}
		Set<String> isConnected = new HashSet<String>();
		Map<IbmOutputBranch,Transition> mappedBranches = new HashMap<IbmOutputBranch, Transition>();
		
		// TODO: add a final transition for the process output!
		
		for (IbmConnection conn : process.getFlowContent().getConnections()){
			String sourceNode = conn.getSourceNode();
			String targetNode = conn.getTargetNode();
			
			if (sourceNode != null && targetNode != null && !isConnected.contains(sourceNode+targetNode)){
				// find transitions and combine them
				if (pnElementByName.containsKey(sourceNode) && pnElementByName.containsKey(targetNode)){
					PetrinetNode sNode = resolveSource(pnElementByName.get(sourceNode));
					PetrinetNode tNode = resolveTarget(pnElementByName.get(targetNode)); 
					if (sNode instanceof Transition && tNode instanceof Transition){
						if (forks.contains(sourceNode)){
							Place forkPlace = net.addPlace(sourceNode+SEPARATOR+targetNode);
							net.addArc((Transition) sNode, forkPlace);
							net.addArc(forkPlace, (Transition) tNode);
						} else {
							net.addArc(getNextPlace(sNode, net), (Transition) tNode);
						}
						isConnected.add(sourceNode+targetNode);
					} else if (!(sNode instanceof Transition || tNode instanceof Transition)){ // both are either places or subnets
						// add join transition:
						if (conn.getSourceContactPoint()!=null && outputBranches.containsKey(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint())){
							IbmOutputBranch outputBranch = outputBranches.get(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint());
							if (mappedBranches.containsKey(outputBranch)){
								net.addArc(mappedBranches.get(outputBranch), (Place) tNode);
							} else {
								Transition tChoice = net.addImmediateTransition(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint(), Double.valueOf(outputBranch.getProbability()));
								tChoice.setInvisible(true);
								mappedBranches.put(outputBranch, tChoice);
								net.addArc((Place)sNode, tChoice);
								net.addArc(tChoice, (Place)tNode);
							}
						} else {
							Transition arcTransition = net.addImmediateTransition(((Place)sNode).getLabel()+"_"+((Place)tNode).getLabel());
							arcTransition.setInvisible(true);
							net.addArc((Place) sNode, arcTransition);
							net.addArc(arcTransition, (Place) tNode);
						}
						isConnected.add(sourceNode+targetNode);
					} else if (sNode instanceof Place){
						if (conn.getSourceContactPoint()!=null && outputBranches.containsKey(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint())){
							IbmOutputBranch outputBranch = outputBranches.get(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint());
							if (mappedBranches.containsKey(outputBranch)){
								Place pChoice = net.addPlace(getPlaceName(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint()));
								net.addArc(mappedBranches.get(outputBranch), pChoice);
								net.addArc(pChoice, (Transition) tNode);
							} else {
								Transition tChoice = net.addImmediateTransition(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint(), Double.valueOf(outputBranch.getProbability()));
								tChoice.setInvisible(true);
								mappedBranches.put(outputBranch, tChoice);
								net.addArc((Place)sNode, tChoice);
								Place pChoice = net.addPlace(getPlaceName(conn.getSourceNode()+SEPARATOR+conn.getSourceContactPoint()));
								net.addArc(tChoice, pChoice);
								net.addArc(pChoice, (Transition) tNode);
								isConnected.add(sourceNode+targetNode);
							}
						} else {
							net.addArc((Place)sNode, (Transition)tNode);
						}
					} else {
						net.addArc((Transition)sNode, (Place)tNode);
					}
//					net.addArc(getNextPlace(pnElementByName.get(sourceNode), net), (Transition) pnElementByName.get(targetNode));
				}
			}
		}
		// add connections for inputs for nodes that are not connected yet (initial inputs)
		for (IbmConnection conn : process.getFlowContent().getConnections()){
			String sourceRef = conn.getSourceNode();
			String targetRef = conn.getTargetNode();
			
			String sourceContactPoint = conn.getSourceContactPoint();
			String targetContactPoint = conn.getTargetContactPoint();
			
			// check inputs
			if (sourceRef == null && targetRef != null && sourceContactPoint != null && isInput(sourceContactPoint,process.getInputs())){
				if (pnElementByName.containsKey(targetRef)){
					Object targetNode = resolveTarget(pnElementByName.get(targetRef));
					if (targetNode instanceof Transition){
						if (net.getInEdges((Transition)targetNode).isEmpty()){
//							// create start node
//							Place startNode = net.addPlace("input_start");
////							startNodes.add(startNode);
//							net.addArc(startNodes.iterator().next(), (Transition) targetNode);
//						}
							if (startPlaces.isEmpty()){
								// create start node
								Place startNode = net.addPlace("input_start");
								startPlaces.add(startNode);
								net.addArc(startPlaces.iterator().next(), (Transition) targetNode);
							} else {
								// connect start node with Transition
								net.addArc(startPlaces.iterator().next(), (Transition) targetNode);
							}
						}
					}
				}
			}
			
			// check outputs
			if (targetRef == null && sourceRef != null && targetContactPoint != null && isOutput(targetContactPoint,process.getOutputs())){
				if (pnElementByName.containsKey(sourceRef)){
					Object sourceNode = resolveSource(pnElementByName.get(sourceRef));
					if (sourceNode instanceof Transition) { // ignore places (they are fine)
						if (net.getOutEdges((Transition)sourceNode).isEmpty()){
							// create end data place
							Place endNode = net.addPlace("output_end");
//							endNodes.add(endNode);
							net.addArc((Transition) sourceNode, endNode);
						}
//						if (endNodes.isEmpty()){
//							// create end place
//							Place endNode = net.addPlace("output_end");
//							endNodes.add(endNode);
//							net.addArc((Transition) sourceNode, endNodes.iterator().next());
//						} else {
//							net.addArc((Transition) sourceNode, endNodes.iterator().next());
//						}
					}
				}
			}
		}
		
		if (startPlaces.size() > 1){
			// make start nodes alternative
			Place newStartPlace = net.addPlace("artificial_start");
			for (Place startPlace : startPlaces){
				Transition tChooseThisStart = net.addImmediateTransition("choose_"+startPlace.getLabel(), 1);
				tChooseThisStart.setInvisible(true);
				net.addArc(newStartPlace, tChooseThisStart);
				net.addArc(tChooseThisStart, startPlace);
			}
		}
		
		return net;
	}
	
	private static Pair<Place, Place> addNet(StochasticNet root, StochasticNet newNetToAdd) {
		// add transitions, places, and connections
		Map<PetrinetNode, PetrinetNode> mapping = new HashMap<>();
		for (Transition t : newNetToAdd.getTransitions()){
			Transition newT;
			if (t instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) t;
				newT = root.addTimedTransition(tt.getLabel(), tt.getWeight(), tt.getDistributionType(), tt.getTrainingData(), tt.getDistributionParameters());
			} else {
				newT = root.addTransition(t.getLabel());
			}
			newT.setInvisible(t.isInvisible());
			mapping.put(t, newT);
		}
		for (Place p : newNetToAdd.getPlaces()){
			Place newP = root.addPlace(p.getLabel());
			mapping.put(p, newP);
		}
		for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : newNetToAdd.getEdges()){
			PetrinetNode sourceNode = mapping.get(edge.getSource());
			PetrinetNode targetNode = mapping.get(edge.getTarget());
			if (sourceNode instanceof Place) {
				root.addArc((Place)sourceNode, (Transition)targetNode);	
			} else {
				root.addArc((Transition)sourceNode, (Place)targetNode);
			}
		}
		Place input = (Place) mapping.get(StochasticNetUtils.getInitialMarking(null, newNetToAdd).iterator().next());
		Place output = (Place) mapping.get(StochasticNetUtils.getFinalMarking(null, newNetToAdd).iterator().next());
		return new Pair<Place, Place>(input, output);
	}

	private static PetrinetNode resolveTarget(Object object) {
		if (object instanceof PetrinetNode){
			return (PetrinetNode) object;
		}
		if (object instanceof Pair){
			// return the initial place
			return (PetrinetNode) ((Pair) object).getFirst();
		}
		return null;
	}

	private static PetrinetNode resolveSource(Object object) {
		if (object instanceof PetrinetNode){
			return (PetrinetNode) object;
		}
		if (object instanceof Pair){
			// return the final place
			return (PetrinetNode) ((Pair) object).getSecond();
		}
		return null;
	}

	private static boolean isOutput(String targetContactPoint, List<IbmOutput> outputs) {
		for (IbmOutput output : outputs){
			if (output.getName().equals(targetContactPoint)){
				return true;
			}
		}
		return false;
	}

	private static boolean isInput(String sourceContactPoint, List<IbmInput> inputs) {	
		for (IbmInput input : inputs){
			if (input.getName().equals(sourceContactPoint)){
				return true;
			}
		}
		return false;
	}

	private static Transition addDefaultTimedTransition(StochasticNet net, String processName) {
		return net.addTimedTransition(processName, DistributionType.NORMAL, 100,10);
	}

	private static String getPlaceName(String name) {
		return name+"_place";
	}
	private static String getProcessName(String name) {
		return name+"_process";
	}
	private static String getTaskName(String name) {
		return name+"_task";
	}
	private static String getServiceName(String name) {
		return name+"_service";
	}
	private static String getStartNodeName(String name) {
		return name+"_start";
	}
	private static String getEndNodeName(String name) {
		return name+"_end";
	}
	private static String getDecisionName(String name) {
		return name+"_decision";
	}
	private static String getMergeName(String name) {
		return name+"_merge";
	}
	
	
	private static Place getNextPlace(Object element, Petrinet net){
		if (element instanceof Place){
			return (Place) element;
		} else if(element instanceof Transition){
			Place place = net.addPlace(getPlaceName(((Transition)element).getLabel()));
			net.addArc((Transition) element, place);
			return place;
		} else {
			StochasticNet subNet = (StochasticNet) element;
			return StochasticNetUtils.getFinalMarking(null, subNet).iterator().next();
		}
//		
//		Collection<PetrinetEdge<? extends PetrinetNode,? extends PetrinetNode>> edges = net.getOutEdges(element);
//		if (edges.size() > 1){
//			System.out.println("Probable modeling error: More than one outputEdge at "+element.getLabel()+"!!");
//			//throw new RuntimeException("More than one outputEdge at "+element.getLabel()+"!!");
//			//return the edge that leads to the natural place of the transition.
//			for (PetrinetEdge<? extends PetrinetNode,? extends PetrinetNode> edge : edges){
//				if (edge.getTarget().getLabel().equals(getPlaceName(element.getLabel()))){
//					return (Place) edge.getTarget();
//				}
//			}
//			return (Place) edges.iterator().next().getTarget(); 
//		} else if (edges.size() == 0){
//			Place place = net.addPlace(getPlaceName(element.getLabel()));
//			net.addArc((Transition) element, place);
//			return place;
//		}
//		
//		// if our current element is not a place, the next one is - because of the bipartite property of Petri nets. 
//		return (Place) edges.iterator().next().getTarget();
	}
}
