package org.processmining.plugins.stochasticpetrinet.measures;

public abstract class AbstractionLevel implements Comparable<AbstractionLevel> {

    public abstract String getName();

    public abstract int[] abstractFrom(int[] rawEncoding);

    public abstract double getLevel();

    public final int compareTo(AbstractionLevel level){
        return new Double(getLevel()).compareTo(level.getLevel());
    }
}
