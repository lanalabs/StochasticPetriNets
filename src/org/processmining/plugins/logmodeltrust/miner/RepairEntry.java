package org.processmining.plugins.logmodeltrust.miner;

import java.util.Set;

public class RepairEntry implements Comparable<RepairEntry>{

	private boolean add = false;
	
	private String eventType;
	
	private int weight;
	
	private Set<int[]> positionsInAlignment;
	
	public RepairEntry(String eventType, boolean add, int weight, Set<int[]> set){
		this.eventType = eventType;
		this.add = add;
		this.weight = weight;
		this.positionsInAlignment = set;
	}

	public boolean isAdd() {
		return add;
	}

	public String getEventType() {
		return eventType;
	}

	public int getWeight() {
		return weight;
	}

	public int compareTo(RepairEntry o) {
		 return Integer.compare(weight, o.weight);
	}

	public Set<int[]> getPositionsInAlignment() {
		return positionsInAlignment;
	}
	
}
