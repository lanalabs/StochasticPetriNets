package org.processmining.plugins.stochasticpetrinet.measures;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Abstracting from permutations, but still keeping count of executions
 */
public class MultiSetAbstractionLevel extends AbstractionLevel {

    public String getName() {
        return "Multi set abstraction";
    }

    public int[] abstractFrom(int[] rawEncoding) {
        Bag<Integer> intBag = new HashBag<>();
        for (int i : rawEncoding) {
            intBag.add(i);
        }
        int[] result = new int[intBag.size()];
        Iterator<Integer> iter = intBag.iterator();
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
        return 1;
    }

}
