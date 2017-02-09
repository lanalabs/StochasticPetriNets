package org.utils.datastructures.measure;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class JaccardSimilarity {

    public static <E extends Comparable<E>> double getSimilarity(E[] x, E[] y) {
        return getSimilarity(Arrays.asList(x), Arrays.asList(y));
    }

    public static <E extends Comparable<E>> double getSimilarity(Collection<E> x, Collection<E> y) {
        if (x.size() == 0 || y.size() == 0) {
            return 0.0;
        }

        Set<E> unionXY = new HashSet<E>(x);
        unionXY.addAll(y);

        Set<E> intersectionXY = new HashSet<E>(x);
        intersectionXY.retainAll(y);

        return (double) intersectionXY.size() / (double) unionXY.size();

    }
}
