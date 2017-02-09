package org.processmining.plugins.stochasticpetrinet.measures.entropy;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.stochasticpetrinet.measures.AbstractionLevel;

import java.util.Arrays;
import java.util.Map;

/**
 * The outcome of an abstraction of a trace
 * <p>
 * Currently, there is no need for more than an int-encoded representation of an event.
 * We can represent each abstraction level as an ordered integer array.
 * <p>
 * Let's assume we encode the events A,B,C,D with 1,2,3,4 respectively.
 * <p>
 * The set abstraction is represented as an ordered array of the encoded integers. {B,D,A} -> [1,2,4]
 * The multiset abstraction is also an ordered array but with multiple. {B²,D,A³} -> [1,1,1,2,2,4]
 * And the list abstraction is just kept. <B,A,D,C,C,A> -> [2,1,4,3,3,1]
 * <p>
 * Comparison becomes easy this way, as we don't need to search but can fast and simple array-comparison.
 *
 * @author Andreas Rogge-Solti
 * @see AbstractionLevel
 */
public class Outcome implements Comparable<Outcome> {

    private int[] representation;

    public Outcome(int... args) {
        this.representation = args.clone();
    }

    public Outcome(XTrace trace, AbstractionLevel level, XEventClasses eventClasses, Map<XEventClass, Integer> encoding) {
        int[] rawEncoding = getRawEncoding(trace, eventClasses, encoding);
        this.representation = level.abstractFrom(rawEncoding);
    }

    private int[] getRawEncoding(XTrace trace, XEventClasses eventClasses, Map<XEventClass, Integer> encoding) {
        int[] encodedTrace = new int[trace.size()];
        int i = 0;
        for (XEvent e : trace) {
            encodedTrace[i++] = encoding.get(eventClasses.getClassOf(e));
        }
        return encodedTrace;
    }

    public int hashCode() {
        return Arrays.hashCode(representation);
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Outcome other = (Outcome) obj;
        if (!Arrays.equals(representation, other.representation))
            return false;
        return true;
    }

    public int compareTo(Outcome o) {
        // order by size first
        if (this.representation.length != o.representation.length) {
            return this.representation.length - o.representation.length;
        }
        // order by elements
        for (int i = 0; i < representation.length; i++) {
            if (representation[i] != o.representation[i]) {
                return representation[i] - o.representation[i];
            }
        }
        return 0;
    }
}
