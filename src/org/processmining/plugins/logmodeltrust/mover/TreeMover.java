package org.processmining.plugins.logmodeltrust.mover;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jbpt.bp.BehaviouralProfile;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.logmodeltrust.converter.ProcessTreeConverter;
import org.processmining.plugins.logmodeltrust.heuristic.bp.BehaviourJaccardSimilarity;
import org.processmining.plugins.logmodeltrust.heuristic.bp.ProcessTreeBPCreator;
import org.processmining.plugins.logmodeltrust.heuristic.processtree.NodeWrapper;
import org.processmining.plugins.logmodeltrust.mover.EditOperation.Op;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractTask;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import treedist.LabeledTree;
import treedist.Mapping;
import treedist.TreeEditDistance;

/**
 * Allows to move between two process trees along the path of a tree edit distance.
 * The edit operations are forming a spectrum between the two models.
 * 
 * @author Andreas R-Solti
 *
 */
public class TreeMover {
	
	private ProcessTreeConverter converter;
	
	private ProcessTree origTree;
	private ProcessTree targetTree;
	
	private ProcessTree resultTree;
	
	private LabeledTree origLblTree;
	private LabeledTree targetLblTree;
	
	private Map<UUID, String> names;
	
	/** maps from the orig nodes' ids to their integer position in the target labeled tree */ 
	private BiMap<UUID, Integer> origNodeIds;
	/** maps from the target nodes' ids to their integer position in the target labeled tree */ 
	private BiMap<UUID, Integer> targetNodeIds;
	
	/** maps from the original tree representation (LabeledTree) to the new nodes in the moving tree*/
	private BiMap<Integer, Node> fromOrigToNewNodes;
	/** maps from the target tree representation (LabeledTree) to the new nodes in the moving tree*/
	private BiMap<Integer, Node> fromTargetToNewNodes;
	
//	private JPanel graphPanel;

	private Mapping map;
	private double distance;
	private BehaviorScore score;
	
	private BehaviouralProfile<ProcessTree, NodeWrapper> targetProfile;
	
	private List<EditOperation> editOperations; 
	
	public TreeMover(ProcessTree orig, ProcessTree target){
		super();
		
		this.names = new HashMap<>();
			
		this.origTree = orig;
		this.targetTree = target;
		
//		this.graphPanel = new JPanel();
//		JFrame frame = new JFrame("debug window");
//		graphPanel.setPreferredSize(new Dimension(800,1000));
//		frame.getContentPane().add(graphPanel);
//		frame.pack();
//		frame.setVisible(true);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		init();
	}

	/**
	 * Precomputes the tree edit distance
	 */
	private void init() {
		this.converter = new ProcessTreeConverter();
		
		this.origLblTree = converter.getLabeledTree(origTree);
		this.targetLblTree = converter.getLabeledTree(targetTree);
		
		this.targetProfile = ProcessTreeBPCreator.getInstance().deriveBehaviouralProfile(targetTree);
		
		this.origNodeIds = getMappingOfNodes(origTree);
		this.targetNodeIds = getMappingOfNodes(targetTree);
		
		this.map = new Mapping(this.origLblTree, this.targetLblTree);
		this.score = new BehaviorScore(this.origLblTree, this.targetLblTree);
		TreeEditDistance dist = new TreeEditDistance(this.score);
		distance = dist.calc(this.origLblTree, this.targetLblTree, map);
		
//		TreeDistanceTest.debugTrace(converter, origLblTree, targetLblTree, map);
	}

	private BiMap<UUID,Integer> getMappingOfNodes(ProcessTree tree) {
		BiMap<UUID,Integer> mapping = HashBiMap.create();
		for (Node node : tree.getNodes()){
			String nodeName = node.getName();
			nodeName = nodeName.trim().isEmpty()? node.getClass().getSimpleName(): nodeName;
			mapping.put(node.getID(), converter.getNodeMapping().get(node));
			names.put(node.getID(), nodeName);
		}
		return mapping;
	}
	
	public ProcessTree getProcessTreeBasedOnTrust(double trustLevel){
		if (trustLevel < 0 || trustLevel > 1){
			throw new IllegalArgumentException("trust level must be between 1 (total trust) and 0 (no trust at all)");
		}
		
		this.resultTree = TreeUtils.getClone(this.origTree);
		
		this.fromOrigToNewNodes = HashBiMap.create();
		this.fromTargetToNewNodes = HashBiMap.create();
		for (Node n : resultTree.getNodes()){
			fromOrigToNewNodes.put(converter.getNodeMapping().get(n), n);
		}
		for (int[] repl : map.getAllReplacement()){
			if (fromOrigToNewNodes.containsKey(repl[0])){
				fromTargetToNewNodes.put(repl[1], fromOrigToNewNodes.get(repl[0]));
				System.out.println("mapped: "+names.get(fromOrigToNewNodes.get(repl[0]).getID())+" - "+names.get(targetNodeIds.inverse().get(repl[1])));
			} else {
				System.out.println("Debug this! - all original replacement nodes should be already in the map.");
			}
		}
		
		// add edit operations from the mapping, until we reach a point close to the trust level
		editOperations = getEditOperations();
//		editOperations.add(0, editOperations.remove(4));

		double targetDistance = this.distance * (1-trustLevel);
				
		double currentDistance = 0;
		
		while (!editOperations.isEmpty() && currentDistance < targetDistance){
			// check all possible next moves and apply the best one
			Pair<EditOperation, Double> bestEditOperation = null;
			for (EditOperation oper : editOperations){
				try {
					// peek forward to see, how good the next tree would be.
					List<Edge> edges = new ArrayList<>(resultTree.getEdges());
					Collections.sort(edges, new Comparator<Edge>() {
						public int compare(Edge o1, Edge o2) {
							return o1.toString().compareTo(o2.toString());
						}
					});
					int size = edges.size();
					apply(oper, resultTree);
					BehaviouralProfile<ProcessTree, NodeWrapper> potentialProfile = ProcessTreeBPCreator.getInstance().deriveBehaviouralProfile(resultTree);
					unapply(oper, resultTree);
					List<Edge> newEdges = new ArrayList<>(resultTree.getEdges());
					Collections.sort(newEdges, new Comparator<Edge>() {
						public int compare(Edge o1, Edge o2) {
							return o1.toString().compareTo(o2.toString());
						}
					});
					if (resultTree.getEdges().size()!=size){
						System.out.println("before oper "+oper+" :"+edges.size());
						System.out.println("after oper "+oper+" :"+resultTree.getEdges().size());
						System.out.println(Joiner.on("\n ").join(edges));
						System.out.println("");
						System.out.println(Joiner.on("\n ").join(newEdges));
					}
					
					Pair<EditOperation, Double> thisOperation = new Pair<>(oper, getSimilarityOfProfiles(targetProfile, potentialProfile));
					if (bestEditOperation == null || bestEditOperation.getSecond() < thisOperation.getSecond()){
						bestEditOperation = thisOperation;
					}
				} catch (IllegalArgumentException e){
					// ignore moves that violate the behavioral profile computation
					// for example: adding an already existing node, before removing the old one.
				}
			}
			currentDistance += apply(bestEditOperation.getFirst(), resultTree);
//			visualizeTree(resultTree, targetTree);
			editOperations.remove(bestEditOperation.getFirst());
		}		
		
		return resultTree;
	}


	private Double getSimilarityOfProfiles(BehaviouralProfile<ProcessTree, NodeWrapper> profileA,
			BehaviouralProfile<ProcessTree, NodeWrapper> profileB) {
		BehaviourJaccardSimilarity<ProcessTree, NodeWrapper> sim = new BehaviourJaccardSimilarity<>(profileA, profileB);
		return sim.getSimilarity();
	}
	
	public double unapply(EditOperation op, ProcessTree tree){
		op.setReverse(!op.isReverse());
		double val = apply(op, tree);
		op.setReverse(!op.isReverse());
		return val;
	}

	public double apply(EditOperation op, ProcessTree tree){
		System.out.println("Applying: "+op);
//		visualizeTree(tree, targetTree);
		switch (op.getOperation()){
			case INSERT:
				if (!op.isReverse()){
					insertNodeIntoTree(op.getNewNode(), false, tree);
				} else {
					// reverse insert = delete
					deleteNodeFromTree(op.getNewNode(), true, tree);
				}
				break;
			case DELETE:
				if (!op.isReverse()){
					deleteNodeFromTree(op.getOrigNode(), false, tree);
				} else {
					insertNodeIntoTree(op.getOrigNode(), true, tree);
				}
				break;
			case RENAME:
				if (!op.isReverse()){
					renameNodesInTree(op.getOrigNode(), op.getNewNode(), false, tree);
				} else {
					renameNodesInTree(op.getNewNode(), op.getOrigNode(), true, tree);
				}
				break;
			default:
				break;
				
		}
//		visualizeTree(tree, targetTree);
		return op.getCost();
	}

	/**
	 * This corresponds to a replacement mapping. 
	 * We find the original node in the tree, and replace it with the new node.
	 * 
	 * All connections are passed to the new node.
	 * 
	 * @param origNode
	 * @param newNode
	 * @param reverse
	 * @param tree
	 */
	protected void renameNodesInTree(UUID origNodeId, UUID newNodeId, boolean reverse, ProcessTree tree) {
		BiMap<UUID, Integer> fromNodeIds = reverse?targetNodeIds:origNodeIds;
		BiMap<UUID, Integer> toNodeIds = reverse?origNodeIds:targetNodeIds;
		BiMap<Integer, Node> fromNodeToNewNodes = reverse?fromTargetToNewNodes:fromOrigToNewNodes;
		BiMap<Integer, Node> toNodeToNewNodes = reverse?fromOrigToNewNodes:fromTargetToNewNodes;
		
		
		int nodeIdToRename = fromNodeIds.get(origNodeId);
		int nodeIdToRenameTo = toNodeIds.get(newNodeId);

		Node nodeToRename = fromNodeToNewNodes.get(nodeIdToRename);
		Node newNode = reverse ? origTree.getNode(newNodeId) : targetTree.getNode(newNodeId);

		
		Node np = getNewNode(newNode, nodeToRename, tree);
		
		if (np != null && nodeToRename != null && np instanceof Block && nodeToRename instanceof Block){
			// transfer all connection of the original node to the new node:
			Block origBlock = (Block) nodeToRename;
			Block newBlock = (Block) np;
			int index = 0;
			for (Node child : origBlock.getChildren()){
				newBlock.addChild(child);
				tree.removeEdge(origBlock.getOutgoingEdges().get(index++));
			}
			
		}
		Collection<Block> parents = nodeToRename.getParents();
		for (Block parent : parents){
			np.addParent(parent);
			Collection<Edge> edgesToRemove = new ArrayList<>();
			for (Edge outgoing : parent.getOutgoingEdges()){
				if (outgoing.getTarget().equals(nodeToRename)){
					edgesToRemove.add(outgoing);
				}
			}
			for (Edge toRemove : edgesToRemove){
				parent.removeOutgoingEdge(toRemove);
			}
		}
		for (Edge e : nodeToRename.getIncomingEdges()){
			tree.removeEdge(e);
		}
		tree.removeNode(nodeToRename);
	
		
//			toRemove.setProcessTree(null);
		
		fromNodeToNewNodes.put(nodeIdToRename, np);
		toNodeToNewNodes.put(nodeIdToRenameTo, np);
	}

	/**
	 * For deleting a node, we need to:
	 * - know the parent of the node to be deleted.
	 * - know the children of the node to be deleted.
	 * - delete the node and connect the children to the parent of the deleted node (in the correct position!)
	 * 
	 * @param uuid
	 * @param reverse
	 * @param tree
	 */
	protected void deleteNodeFromTree(UUID uuid, boolean reverse, ProcessTree tree) {
		BiMap<UUID, Integer> nodeIds = reverse ? targetNodeIds : origNodeIds ;
		BiMap<Integer, Node> fromNodeToNewNodes = reverse?fromTargetToNewNodes:fromOrigToNewNodes;
		
		Integer nodeIdInOrig = nodeIds.get(uuid);
		Node origNode = fromNodeToNewNodes.get(nodeIdInOrig);
		
		if (origNode == null){
			System.out.println("Debug me!");
		}
		Node parentNode = origNode.getParents().iterator().next();
		if (origNode.getParents().size()>1){
			System.out.println("Debug me!");
		}
		
		int currentSiblingPosition = getPositionInParent(origNode, parentNode);
		
		List<Edge> edgesToRelocate = new ArrayList<>();
		// we need to find all children that are below the old node and are currently present in the tree
		if (origNode instanceof Block){ // might have children
			Block origBlock = (Block) origNode;
			edgesToRelocate = origBlock.getOutgoingEdges();
			
			if (parentNode != null){
				Block newParentBlock = (Block) parentNode;
				for (Edge e : edgesToRelocate){
					newParentBlock.addChildAt(e.getTarget(), currentSiblingPosition++);
					tree.removeEdge(e);
					e.getTarget().removeIncomingEdge(e);
				}
			}
		}
		
		// add children at sibling position of current node that will be removed
		if (parentNode != null){
			Block newParentBlock = (Block) parentNode;
			
			Edge toRemove = null;
			for (Edge e : newParentBlock.getOutgoingEdges()){
				if (e.getTarget().getID().equals(origNode.getID())){
					toRemove = e;
				}
			}
			newParentBlock.removeOutgoingEdge(toRemove);
			tree.removeEdge(toRemove);
			tree.removeNode(origNode);
			fromNodeToNewNodes.remove(nodeIdInOrig);
		} else {
			System.out.println("Debug me!");
		}
	}

	/**
	 * When inserting a node, it is important to find a mapped parent of it.
	 * We need to insert the node under that parent.
	 * Then, we check all children of that parent, whether they should be descendants of the new node.
	 * These will be put below the new node and their edges to the parent node are cut.
	 *  
	 * @param uuid
	 * @param reverse
	 * @param tree
	 */
	protected void insertNodeIntoTree(UUID uuid, boolean reverse, ProcessTree tree) {
		BiMap<UUID, Integer> nodeIds = reverse ? origNodeIds : targetNodeIds;
		BiMap<Integer, Node> nodeToNewNodes = reverse?fromOrigToNewNodes:fromTargetToNewNodes;
		ProcessTree sourceTree = reverse? origTree : targetTree; 
		
		Integer nodeIdInTree = nodeIds.get(uuid);
		Node newNode = sourceTree.getNode(uuid);
		
		LabeledTree labeledTree = reverse ? origLblTree : targetLblTree;
		
		// find next existing parent under which we can place the new node:
		int newParentInTree = findNextMappedParent(nodeIdInTree, labeledTree, nodeToNewNodes);
		Node parentNode = nodeToNewNodes.get(newParentInTree);
		
		// we need to find all children that will go below the new node and are currently present in the tree
		if (parentNode != null && parentNode instanceof Block){ // has parent
			Block parentBlock = (Block) parentNode;
			
			int lastChild = 0;
			List<Edge> edges = parentBlock.getOutgoingEdges();
			List<Edge> edgesToRelocate = new ArrayList<>();
			for (Edge outgoing : edges){
				Node child = outgoing.getTarget();
				Integer childIdInTarget =  nodeToNewNodes.inverse().get(child);
				if (childIdInTarget == null){
					// not added yet! or belongs to other tree (we don't care)
				} else {
					if (isAncestor(childIdInTarget, nodeIdInTree, labeledTree)){
						lastChild++;
						edgesToRelocate.add(outgoing);
					} else if (edgesToRelocate.isEmpty() && isRightOf(childIdInTarget, nodeIdInTree, labeledTree)){
						lastChild++;
					}
				}
			}
			// add child at position of last child that will be removed or at the end
			// TODO: change this to use sibling information of the new node!
			Node newNodeToInsert = getNewNode(newNode, null, tree);
			parentBlock.addChildAt(newNodeToInsert, lastChild);
			int size = nodeToNewNodes.size();
			nodeToNewNodes.put(nodeIdInTree, newNodeToInsert);
			System.out.println("added "+newNodeToInsert.getName()+" to nodes. Size from " + size+" to " + nodeToNewNodes.size());
			for (Edge edgeToRelocate : edgesToRelocate){
				System.out.println("Relocating edge: "+edgesToRelocate+" to new ");
				Node target = edgeToRelocate.getTarget();
				parentBlock.removeOutgoingEdge(edgeToRelocate);
				target.removeIncomingEdge(edgeToRelocate);
				tree.removeEdge(edgeToRelocate);
				Block newBlock = (Block) newNodeToInsert;
				newBlock.addChild(target);
			}
		} else {
			System.out.println("Debug me!");
		}
	}


	private Node getNewNode(Node newNode, Node origNode, ProcessTree tree) {
		Node np = null;
		try {
			if(newNode instanceof AbstractTask.Manual){
				np = new AbstractTask.Manual(newNode.getID(), newNode.getName());
			} else if(newNode instanceof AbstractTask.Automatic){
				np = new AbstractTask.Automatic(newNode.getID(), newNode.getName());
			} else {
				np = newNode.getClass().getConstructor(UUID.class, String.class).newInstance(newNode.getID(), newNode.getName());
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		tree.addNode(np);
		return np;
	}

	private boolean isRightOf(Integer childIdInTarget, int nodeIdInTarget, LabeledTree tree) {
		Collection<Integer> rightSiblings = getRightSiblings(childIdInTarget, tree);
		return rightSiblings.contains(nodeIdInTarget);
	}

	private Collection<Integer> getRightSiblings(Integer childIdInTarget, LabeledTree tree) {
		int node = childIdInTarget;
		int sibling = tree.getNextSibling(node);
		Collection<Integer> siblings = new ArrayList<>();
		while (sibling != -1){
			siblings.add(sibling);
			node = sibling;
			sibling = tree.getNextSibling(node);
		}
		return siblings;
	}

	private int getPositionInParent(Node origNode, Node newParentNode) {
		if (newParentNode instanceof Block){
			Block parent = (Block) newParentNode;
			List<Node> children = parent.getChildren();
			for (int i = 0; i < children.size(); i++){
				Node child = children.get(i);
				if (origNode.equals(child)){
					return i;
				}
			}
		}
		System.out.println("Debug me! - Why is it not a child?");
		return 0;
	}

	/**
	 * Checks whether "child in target" is a descendant of "node in target".
	 * @param targetLblTree
	 * @param childId
	 * @param parentId
	 * @return
	 */
	private boolean isAncestor(Integer childId, int parentId, LabeledTree tree) {
		Collection<Integer> ancestors = getAncestors(childId, tree);
		return ancestors.contains(parentId);
	}

	private Collection<Integer> getAncestors(Integer childIdInTarget, LabeledTree targetLblTree2) {
		if (childIdInTarget == null){
			System.out.println("debug this.");
		}
		int child = childIdInTarget;
		int parent = targetLblTree2.getParent(childIdInTarget);
		Collection<Integer> parents = new ArrayList<>();
		while (parent != -1){
			parents.add(parent);
			child = parent;
			parent = targetLblTree2.getParent(child);
		}
		return parents;
	}

	private int findNextMappedParent(int nodeIdInTarget, LabeledTree tree, BiMap<Integer, Node> fromToNewMapping) {
		boolean found = false;
		int nodeId = nodeIdInTarget;
		while (!found && tree.getParent(nodeId) >= -1){
			nodeId = tree.getParent(nodeId);
			if (fromToNewMapping.containsKey(nodeId)){
				found = true;
			}
		}
		if (!found){
			return -1;
		}
		return nodeId;
	}

	/**
	 * Converts a mapping into a list of edit operations {@link EditOperation}.
	 * Assumes that {@link #map} is already computed. 
	 * @return a list of edit operations that transform the original tree to the target tree.
	 */
	private List<EditOperation> getEditOperations() {
		List<EditOperation> editOps = new ArrayList<>();
		
		for (Integer insertedNode : map.getAllInsertion()){
			EditOperation insertOp = new EditOperation(Op.INSERT, null, null, targetNodeIds.inverse().get(insertedNode), names.get(targetNodeIds.inverse().get(insertedNode)), score.insert(insertedNode));
			editOps.add(insertOp);
		}
		
		for (Integer deletedNode : map.getAllDeletion()){
			EditOperation deleteOp = new EditOperation(Op.DELETE, origNodeIds.inverse().get(deletedNode), names.get(origNodeIds.inverse().get(deletedNode)), null, null, score.delete(deletedNode));
			editOps.add(deleteOp);
		}
		
		for (int[] replacement : map.getAllReplacement()){
			double cost = score.replace(replacement[0], replacement[1]);
			if (cost > 0){
				EditOperation replaceNode = new EditOperation(Op.RENAME, origNodeIds.inverse().get(replacement[0]), names.get(origNodeIds.inverse().get(replacement[0])), targetNodeIds.inverse().get(replacement[1]), names.get(targetNodeIds.inverse().get(replacement[1])), cost);
				editOps.add(replaceNode);
			} else {
				// nothing changed here
			}
		}
		return editOps;
	}	
	
	private void visualizeTree(ProcessTree... trees){
//		graphPanel.removeAll();
//		graphPanel.setLayout(new GridLayout(trees.length, 1));
//		
//		for (ProcessTree tree : trees){
//			TreeLayoutBuilder builder = new TreeLayoutBuilder(tree);
//			JGraph graph = builder.getJGraph();
//			graphPanel.add(new JScrollPane(graph));
//		}
//		
//		graphPanel.revalidate();
//		graphPanel.repaint();
	}
}
