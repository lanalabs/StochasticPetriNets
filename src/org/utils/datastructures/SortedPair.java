package org.utils.datastructures;

import org.apache.commons.math3.util.Pair;

public class SortedPair<K extends Comparable<K>, V> extends Pair<K, V> implements Comparable<SortedPair<K, V>> {

    public SortedPair(K k, V v) {
        super(k, v);
    }

    public int compareTo(SortedPair<K, V> o) {
        return getFirst().compareTo(o.getFirst());
    }
}
