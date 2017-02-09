package org.utils.datastructures;

public class ComparablePair<K extends Comparable<K>, V> implements Comparable<ComparablePair<K, V>> {

    private K first;
    private V second;

    public ComparablePair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public int compareTo(ComparablePair<K, V> other) {
        if (other == null) {
            return 1;
        }
        return first.compareTo(other.first);
    }

    public K getFirst() {
        return first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    public V getSecond() {
        return second;
    }

    public void setSecond(V second) {
        this.second = second;
    }

}
