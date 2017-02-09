package org.utils.datastructures;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A data structure that is limited in size and otherwise works as a queue
 * (but can also access the latest/youngest element at the tail)
 *
 * @param <K>
 * @author Andreas Rogge-Solti
 */
public class LimitedQueue<K> extends ArrayList<K> {
    private static final long serialVersionUID = -4751296587645896037L;

    /**
     * The maximum size of the queue
     */
    private int maxSize;

    public LimitedQueue(int size) {
        this.maxSize = size;
    }

    public boolean add(K k) {
        boolean r = super.add(k);
        limitSize();
        return r;
    }

    public boolean addAll(Collection<? extends K> c) {
        boolean changed = super.addAll(c);
        changed |= limitSize();
        return changed;
    }

    /**
     * @return boolean indicating whether the list changed
     */
    protected boolean limitSize() {
        if (size() > maxSize) {
            removeRange(0, size() - maxSize - 1);
            return true;
        }
        return false;
    }

    public boolean addAll(int index, Collection<? extends K> c) {
        boolean changed = super.addAll(index, c);
        changed |= limitSize();
        return changed;
    }

    /**
     * Provides access to the element inserted as last (the youngest element in the queue)
     *
     * @return K
     */
    public K getLast() {
        return get(size() - 1);
    }

    /**
     * Takes a peek at the element, which is the oldest one in the queue without removing it.
     *
     * @return
     */
    public K peek() {
        return get(0);
    }

    /**
     * Removes the oldest element in the list (FIFO)
     *
     * @return K the oldest element that is also removed
     */
    public K poll() {
        return remove(0);
    }
}