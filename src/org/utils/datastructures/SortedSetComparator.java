package org.utils.datastructures;

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public class SortedSetComparator<E extends Comparable<E>> implements Comparator<SortedSet<E>> {

    @Override
    public int compare(SortedSet<E> first, SortedSet<E> second) {
        Iterator<E> otherRecords = second.iterator();
        for (E thisRecord : first) {
            // Shorter sets sort first.
            if (!otherRecords.hasNext())
                return 1;
            int comparison = thisRecord.compareTo(otherRecords.next());
            if (comparison != 0)
                return comparison;
        }
        // Shorter sets sort first
        if (otherRecords.hasNext())
            return -1;
        else
            return 0;
    }
}