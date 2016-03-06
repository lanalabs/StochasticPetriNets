package org.processmining.plugins.logmodeltrust.mover;

import java.util.HashMap;
import java.util.Map;

import treedist.EditScore;
import treedist.LabeledTree;

public class BehaviorScore implements EditScore {

	Map<Integer, Integer> tree1Children, tree2Children;
	private int maxSize;
	
	LabeledTree t1, t2;
	
	public BehaviorScore(LabeledTree t1, LabeledTree t2){
		tree1Children = computeChildren(t1);
		tree2Children = computeChildren(t2);
		this.t1 = t1;
		this.t2 = t2;
		maxSize = Math.max(t1.size(), t2.size());
	}
	
	private Map<Integer, Integer> computeChildren(LabeledTree t1) {
		Map<Integer, Integer> nodeToChildCount = new HashMap<>();
		for (int i = 0; i < t1.size(); i++){
			increaseParentCounts(t1, nodeToChildCount, i);
		}
		return nodeToChildCount;
	}

	private void increaseParentCounts(LabeledTree t1, Map<Integer, Integer> nodeToChildCount, int i) {
		int parent = t1.getParent(i);
		if (parent >= 0){
			// add 1 to all the parents
			if (!nodeToChildCount.containsKey(parent)){
				nodeToChildCount.put(parent, 1);
			} else {
				nodeToChildCount.put(parent, nodeToChildCount.get(parent)+1);
			}
			increaseParentCounts(t1, nodeToChildCount, parent);
		}
	}
	
	private double getScore(Integer nodeId, Map<Integer, Integer> childrenCount){
		if (childrenCount.containsKey(nodeId)){
			return childrenCount.get(nodeId);
		}
		return 0.5;
	}

	/**
	 * 
	 */
	public double replace(int node1, int node2) {
		if (t1.getLabel(node1) == t2.getLabel(node2)) {
			return 0;
		} else {
			return (4+getScore(node1, tree1Children))/maxSize;
		}
	}

	/**
	 * Score for deleting a node from tree 1
	 */
	public double delete(int node1) {
		return (2+getScore(node1, tree1Children))/maxSize;
	}

	/**
	 * Score for inserting a node in tree 1 
	 */
	public double insert(int node2) {
		return (3+getScore(node2, tree2Children))/maxSize;
	}

}
