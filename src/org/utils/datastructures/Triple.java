package org.utils.datastructures;

public class Triple<K, L, M> {
    private K first;
    private L second;
    private M third;

    public Triple(K first, L second, M third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public K getFirst() {
        return first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    public L getSecond() {
        return second;
    }

    public void setSecond(L second) {
        this.second = second;
    }

    public M getThird() {
        return third;
    }

    public void setThird(M third) {
        this.third = third;
    }
}
