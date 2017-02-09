package org.processmining.plugins.stochasticpetrinet.prediction.timeseries;

import java.util.Map;
import java.util.TreeMap;

/**
 * A {@link TreeMap} limited in upper size...
 *
 * @param <K>
 * @param <V>
 * @author Andreas Rogge-Solti
 */
public class LimitedTreeMap<K, V> extends TreeMap<K, V> {
    /**
     *
     */
    private static final long serialVersionUID = -9013105211924976229L;

    private int maxSize;

    public LimitedTreeMap(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public V put(K key, V value) {
        V v = super.put(key, value);
        if (size() > maxSize) {
            removeFirst();
        }
        return v;
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        super.putAll(map);
        while (size() > maxSize) {
            removeFirst();
        }
    }

    /**
     * Removes the first element of the tree map
     */
    private void removeFirst() {
        K oldKey = this.keySet().iterator().next();
        remove(oldKey);
    }


}
