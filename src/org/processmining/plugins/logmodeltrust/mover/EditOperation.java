package org.processmining.plugins.logmodeltrust.mover;

import org.processmining.processtree.Node;

public class EditOperation implements Comparable<EditOperation>{

	public enum Op{
		INSERT, DELETE, RENAME, KEEP;
	}
	
	private Op operation;
	
	private Node origNode;
	private Node newNode;
	
	private double cost;
	
	public EditOperation(Op operation, Node origNode, Node newNode, double cost){
		this.operation = operation;
		this.origNode = origNode;
		this.newNode = newNode;
		this.cost = cost;
	}
	
	public int compareTo(EditOperation other) {
		return Double.compare(cost, other.cost);
	}

	public Op getOperation() {
		return operation;
	}

	public Node getOrigNode() {
		return origNode;
	}

	public Node getNewNode() {
		return newNode;
	}

	public double getCost() {
		return cost;
	}
	
	public String toString() {
		String origString = origNode == null? "null" : (origNode.getName().isEmpty() ? origNode.getClass().getSimpleName() : origNode.getName());
		String targetString = newNode == null? "null" : (newNode.getName().isEmpty() ? newNode.getClass().getSimpleName() : newNode.getName());
		return operation+" "+origString+"->"+targetString+" "+cost;
	}

}
