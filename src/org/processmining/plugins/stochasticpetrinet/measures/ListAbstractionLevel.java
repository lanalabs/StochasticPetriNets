package org.processmining.plugins.stochasticpetrinet.measures;

/**
 * Not really abstracting.
 */
public class ListAbstractionLevel extends AbstractionLevel {
    public String getName() {
        return "List abstraction";
    }

    public int[] abstractFrom(int[] rawEncoding) {
        // nothing to do
        return rawEncoding.clone();
    }

    @Override
    public double getLevel() {
        return 0;
    }
}
