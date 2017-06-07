package org.processmining.plugins.stochasticpetrinet.measures;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Abstracts from sequences and only retains the set of elements in the sequence, disregards permutations and also frequencies.
 */
public class SetAbstractionLevel extends AbstractionLevel {

    public String getName() {
        return "Set abstraction";
    }

    public int[] abstractFrom(int[] rawEncoding) {
        Set<Integer> intSet = new HashSet<>();
        for (int i : rawEncoding) {
            intSet.add(i);
        }
        int[] result = new int[intSet.size()];
        Iterator<Integer> iter = intSet.iterator();
        int index = 0;
        while (iter.hasNext()) {
            int i = iter.next();
            result[index++] = i;
        }
        Arrays.sort(result);
        return result;
    }

    @Override
    public double getLevel() {
        return 2;
    }
}
