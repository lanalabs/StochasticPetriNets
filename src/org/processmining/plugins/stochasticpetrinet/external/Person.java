package org.processmining.plugins.stochasticpetrinet.external;

public class Person extends Resource {

    public static final String CASE_PREFIX = "patient#";

    protected int capacity;

    public Person(String name) {
        this(name, 1);
    }

    public Person(String name, int capacity) {
        super(name);
        this.capacity = capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public String toString() {
        return getName();
    }

    public static boolean isCaseResourceName(String name) {
        return name.startsWith(CASE_PREFIX);
    }
}
