package org.processmining.plugins.logmodeltrust.mover;

import java.util.UUID;

public class EditOperation implements Comparable<EditOperation>{

	private boolean reverse = false;
	
	public enum Op{
		INSERT, DELETE, RENAME, KEEP;
	}
	
	private Op operation;
	
	private UUID origNode;
	private UUID newNode;
	
	private String origName, newName;
	
	private double cost;

	public EditOperation(Op operation, UUID origNodeId, String origName, UUID newNodeId, String newName,  double cost){
		this(operation,origNodeId, origName, newNodeId, newName, cost, false);
	}

	public EditOperation(Op operation, UUID origNodeId, String origName, UUID newNodeId, String newName,  double cost, boolean reverse){
		this.operation = operation;
		this.origNode = origNodeId;
		this.origName = origName;
		this.newNode = newNodeId;
		this.newName = newName;
		this.cost = cost;
		this.reverse = reverse;
	}
	
	public int compareTo(EditOperation other) {
		return Double.compare(cost, other.cost);
	}

	public Op getOperation() {
		return operation;
	}

	public UUID getOrigNode() {
		return origNode;
	}

	public UUID getNewNode() {
		return newNode;
	}

	public double getCost() {
		return cost;
	}
	
	public String getOrigName() {
		return origName;
	}

	public String getNewName() {
		return newName;
	}

	public String toString() {
		String origString = origName == null? "null" : origName;
		String targetString = newNode == null? "null" : newName;
		return operation+(isReverse()?"-":"")+" "+origString+"->"+targetString+" "+cost;
	}

	public boolean isReverse() {
		return reverse;
	}
	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}
}
