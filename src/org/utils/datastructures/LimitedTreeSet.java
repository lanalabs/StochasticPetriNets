package org.utils.datastructures;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;


/**
 * A {@link TreeSet} with an upper bound on size (if an element will be inserted, the lowest value will be removed)
 *
 * @author Andreas Rogge-Solti
 */
public class LimitedTreeSet<E> extends TreeSet<E> {
    private static final long serialVersionUID = -4346366005764682662L;

    private int maxSize;

    public LimitedTreeSet(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public boolean add(E e) {
        boolean added = super.add(e);
        if (size() > maxSize) {
            // remove first element:
            Iterator<E> iter = this.iterator();
            iter.next();
            iter.remove();
        }
        return added;
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean added = super.addAll(c);
        Iterator<E> iter = this.iterator();
        while (size() > maxSize) {
            // remove first element(s) to keep highes ones:
            iter.next();
            iter.remove();
        }
        return added;
    }

}
