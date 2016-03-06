package org.processmining.plugins.logmodeltrust.mover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.processmining.plugins.logmodeltrust.converter.ProcessTreeConverter;
import org.processmining.plugins.logmodeltrust.mover.EditOperation.Op;
import org.processmining.processtree.Block;
import org.processmining.processtree.Block.And;
import org.processmining.processtree.Block.Def;
import org.processmining.processtree.Block.DefLoop;
import org.processmining.processtree.Block.Or;
import org.processmining.processtree.Block.PlaceHolder;
import org.processmining.processtree.Block.Seq;
import org.processmining.processtree.Block.Xor;
import org.processmining.processtree.Block.XorLoop;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Event.Message;
import org.processmining.processtree.Event.TimeOut;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.Task.Automatic;
import org.processmining.processtree.Task.Manual;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractEvent;
import org.processmining.processtree.impl.AbstractTask;

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
	
	private BiMap<Node, Integer> origNodes;
	private BiMap<Node, Integer> targetNodes;
	
	private BiMap<Integer, Node> fromOrigToNewNodes;
	private BiMap<Integer, Node> fromTargetToNewNodes;
	
	private LabeledTree origLblTree;
	private LabeledTree targetLblTree;
	private Mapping map;
	private double distance;
	private BehaviorScore score;
	
	private List<EditOperation> editOperations; 
	
	public TreeMover(ProcessTree orig, ProcessTree target){
		super();
			
		this.origTree = orig;
		this.targetTree = target;
		init();
		
	}

	/**
	 * Precomputes the tree edit distance
	 */
	private void init() {
		this.converter = new ProcessTreeConverter();
		
		this.origLblTree = converter.getLabeledTree(origTree);
		this.targetLblTree = converter.getLabeledTree(targetTree);
		
		this.origNodes = getMappingOfNodes(origTree);
		this.targetNodes = getMappingOfNodes(targetTree);
		
		this.map = new Mapping(this.origLblTree, this.targetLblTree);
		this.score = new BehaviorScore(this.origLblTree, this.targetLblTree);
		TreeEditDistance dist = new TreeEditDistance(this.score);
		distance = dist.calc(this.origLblTree, this.targetLblTree, map);
		
//		TreeDistanceTest.debugTrace(converter, origLblTree, targetLblTree, map);
		
		// add edit operations from the mapping, until we reach a point close to the trust level
		editOperations = getEditOperations();
		Collections.shuffle(editOperations);
	}

	private BiMap<Node,Integer> getMappingOfNodes(ProcessTree tree) {
		BiMap<Node,Integer> mapping = HashBiMap.create();
		for (Node node : tree.getNodes()){
			mapping.put(node, converter.getNodeMapping().get(node));
		}
		return mapping;
	}
	
	public ProcessTree getProcessTreeBasedOnTrust(double trustLevel){
		if (trustLevel < 0 || trustLevel > 1){
			throw new IllegalArgumentException("trust level must be between 1 (total trust) and 0 (no trust at all)");
		}
		ProcessTree resultTree = TreeUtils.getClone(this.origTree);
		this.fromOrigToNewNodes = HashBiMap.create();
		this.fromTargetToNewNodes = HashBiMap.create();
		for (Node n : resultTree.getNodes()){
			fromOrigToNewNodes.put(converter.getNodeMapping().get(n), n);
		}
		for (int[] repl : map.getAllReplacement()){
			if (fromOrigToNewNodes.containsKey(repl[0])){
				fromTargetToNewNodes.put(repl[1], fromOrigToNewNodes.get(repl[0]));
			} else {
				System.out.println("Debug this! - all original replacement nodes should be already in the map.");
			}
		}
		double targetDistance = this.distance * (1-trustLevel);
				
		double currentDistance = 0;
		Iterator<EditOperation> iter = editOperations.iterator();
		while (currentDistance < targetDistance && iter.hasNext()){
			currentDistance += apply(iter.next(), resultTree); 
		}
		return resultTree;
	}
	
	public double apply(EditOperation op, ProcessTree tree){
//		System.out.println("Applying: "+op);
		switch (op.getOperation()){
			case INSERT:
				int nodeIdInTarget = targetNodes.get(op.getNewNode());
				Node newNode = op.getNewNode();
				// find next existing parent under which we can place the new node:
				int newParentInTargetTree = findNextMappedParent(nodeIdInTarget, targetLblTree, fromTargetToNewNodes);
				Node parentNode = fromTargetToNewNodes.get(newParentInTargetTree);
				
				// we need to find all children that will go below the new node and are currently present in the tree
				if (parentNode != null && parentNode instanceof Block){ // has parent
					Block parent = (Block) parentNode;
					
					int lastChild = 0;
					List<Edge> edges = parent.getOutgoingEdges();
					List<Edge> edgesToRelocate = new ArrayList<>();
					for (Edge outgoing : edges){
						Node child = outgoing.getTarget();
						Integer childIdInTarget =  fromTargetToNewNodes.inverse().get(child);
						if (childIdInTarget == null){
							// not added yet!
						} else {
							if (isAncestor(childIdInTarget, nodeIdInTarget, targetLblTree)){
								lastChild++;
								edgesToRelocate.add(outgoing);
							} else if (edgesToRelocate.isEmpty() && isRightOf(childIdInTarget, nodeIdInTarget, targetLblTree)){
								lastChild++;
							}
						}
					}
					// add child at position of last child that will be removed or at the end
					// TODO: change this to use sibling information of the new node!
					Node newNodeToInsert = getNewNode(newNode, null, tree);
					parent.addChildAt(newNodeToInsert, lastChild);
					fromTargetToNewNodes.put(nodeIdInTarget, newNodeToInsert);
					
					for (Edge edgeToRelocate : edgesToRelocate){
						Node target = edgeToRelocate.getTarget();
						parent.removeOutgoingEdge(edgeToRelocate);
						
						Block newBlock = (Block) newNodeToInsert;
						newBlock.addChild(target);
					}
				}
				break;
			case DELETE:
				int nodeIdInOrig = origNodes.get(op.getOrigNode());
				Node origNode = fromOrigToNewNodes.get(nodeIdInOrig);
				// find next existing parent under which we can place the new node:
				int newParentInOrigTree = findNextMappedParent(nodeIdInOrig, origLblTree, fromOrigToNewNodes);
				Node newParentNode = fromOrigToNewNodes.get(newParentInOrigTree);
				
				int currentSiblingPosition = getPositionInParent(origNode, newParentNode);
				
				List<Edge> edgesToRelocate = new ArrayList<>();
				// we need to find all children that are below the old node and are currently present in the tree
				if (origNode instanceof Block){ // might have children
					Block origBlock = (Block) origNode;
					
					edgesToRelocate = origBlock.getOutgoingEdges();
				}
				
				// add children at sibling position of current node that will be removed
				if (newParentNode != null){
					Block newParentBlock = (Block) newParentNode;
					for (Edge e : edgesToRelocate){
						newParentBlock.addChildAt(e.getTarget(), currentSiblingPosition++);
					}
					Edge toRemove = null;
					for (Edge e : newParentBlock.getOutgoingEdges()){
						if (e.getTarget().equals(origNode)){
							toRemove = e;
						}
					}
					newParentBlock.removeOutgoingEdge(toRemove);
					tree.removeNode(origNode);
					fromOrigToNewNodes.remove(nodeIdInOrig);
				}
				break;
			case RENAME:
				int nodeIdToRename = origNodes.get(op.getOrigNode());
				int nodeIdToRenameTo = targetNodes.get(op.getNewNode());
				Node nodeWithNewName = op.getNewNode();
				
				Node newNodeToRename = fromOrigToNewNodes.get(nodeIdToRename);
				Node np = null;
				if (nodeWithNewName.getClass().equals(newNodeToRename.getClass())){
					// same class: just relabel the node
					newNodeToRename.setName(nodeWithNewName.getName());
					np = newNodeToRename;
				} else {
					// different class: need to create a clone with the new type:
					np = getNewNode(nodeWithNewName, op.getOrigNode(), tree);
					
					tree.addNode(np);
					tree.removeNode(op.getOrigNode());
					op.getOrigNode().setProcessTree(null);
					np.setProcessTree(tree);
				}
				fromOrigToNewNodes.put(nodeIdToRename, np);
				fromTargetToNewNodes.put(nodeIdToRenameTo, np);
				break;
			default:
				break;
				
		}
		return op.getCost();
	}


	private Node getNewNode(Node newNode, Node origNode, ProcessTree tree) {
		Node np = null;
		if(newNode instanceof Seq){
			np = new AbstractBlock.Seq((Seq)newNode);
		}
		if(newNode instanceof And){
			np = new AbstractBlock.And((And)newNode);
		}
		if(newNode instanceof Xor){
			np = new AbstractBlock.Xor((Xor)newNode);
		}
		if(newNode instanceof Def){
			np = new AbstractBlock.Def((Def)newNode);
		}
		if(newNode instanceof Or){
			np = new AbstractBlock.Or((Or)newNode);
		}
		if(newNode instanceof XorLoop){
			np = new AbstractBlock.XorLoop((XorLoop)newNode);
		}
		if(newNode instanceof DefLoop){
			np = new AbstractBlock.DefLoop((DefLoop)newNode);
		}
		if(newNode instanceof PlaceHolder){
			np = new AbstractBlock.PlaceHolder((PlaceHolder)newNode);
		}
		if(newNode instanceof Message){
			np = new AbstractEvent.Message((Message)newNode);
		}
		if(newNode instanceof TimeOut){
			np = new AbstractEvent.TimeOut((TimeOut)newNode);
		}
		if (np != null && origNode != null & np instanceof Block && origNode instanceof Block){
			// transfer all children of the original node:
			Block origBlock = (Block) origNode;
			Block newBlock = (Block) np;
			for (Node child : origBlock.getChildren()){
				newBlock.addChild(child);
			}
			Collection<Block> parents = origBlock.getParents();
			for (Block parent : parents){
				newBlock.addParent(parent);
			}
		}
		
		if(newNode instanceof Manual){
			np = new AbstractTask.Manual((Manual)newNode);
		}
		if(newNode instanceof Automatic){
			np = new AbstractTask.Automatic((Automatic)newNode);
		}
		np.setProcessTree(tree);
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
			EditOperation insertOp = new EditOperation(Op.INSERT, null, targetNodes.inverse().get(insertedNode), score.insert(insertedNode));
			editOps.add(insertOp);
		}
		
		for (Integer deletedNode : map.getAllDeletion()){
			EditOperation deleteOp = new EditOperation(Op.DELETE, origNodes.inverse().get(deletedNode), null, score.delete(deletedNode));
			editOps.add(deleteOp);
		}
		
		for (int[] replacement : map.getAllReplacement()){
			double cost = score.replace(replacement[0], replacement[1]);
			if (cost > 0){
				EditOperation replaceNode = new EditOperation(Op.RENAME, origNodes.inverse().get(replacement[0]), targetNodes.inverse().get(replacement[1]), cost);
				editOps.add(replaceNode);
			} else {
				// nothing changed here
			}
		}
		return editOps;
	}	
}
