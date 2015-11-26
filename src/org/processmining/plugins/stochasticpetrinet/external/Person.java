package org.processmining.plugins.stochasticpetrinet.external;

public class Person extends Resource {

	protected int capacity;
	
	public Person(String name) {
		this(name, 1);
	}
	public Person(String name, int capacity){
		super(name);
		this.capacity = capacity;
	}

	public int getCapacity(){
		return this.capacity;
	}
	
	public String toString(){
		return getName();
	}
}
