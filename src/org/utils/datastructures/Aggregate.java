package org.utils.datastructures;

import java.util.*;

public class Aggregate {

    public static <K, V> Map<K, List<V>> group(Collection<V> items, Function<V, K> groupFunction) {
        Map<K, List<V>> groupedItems = new HashMap<>();

        for (V item : items) {
            K key = groupFunction.apply(item);

            List<V> itemGroup = groupedItems.get(key);
            if (itemGroup == null) {
                itemGroup = new ArrayList<>();
                groupedItems.put(key, itemGroup);
            }

            itemGroup.add(item);
        }

        return groupedItems;
    }

    public static <K, V> Map<K, Integer> sum(Collection<V> items, Function<V, K> groupFunction,
                                             Function<V, Integer> intGetter) {
        Map<K, Integer> sums = new TreeMap<>();

        for (V item : items) {
            K key = groupFunction.apply(item);
            Integer sum = sums.get(key);

            sums.put(key, sum != null ? sum + intGetter.apply(item) : intGetter.apply(item));
        }

        return sums;
    }

    public static <K, V> Map<K, Double> avg(Collection<V> items, Function<V, K> groupFunction,
                                            Function<V, Double> doubleGetter) {
        Map<K, Double> avgs = new TreeMap<>();
        Map<K, Integer> counts = new TreeMap<>();

        for (V item : items) {
            K key = groupFunction.apply(item);
            Integer n = counts.containsKey(key) ? counts.get(key) : 0;
            Double avg = avgs.get(key);

            avgs.put(key, avg != null ? (avg * n + doubleGetter.apply(item)) / (n + 1) : doubleGetter.apply(item));
            counts.put(key, n + 1);
        }

        return avgs;
    }

    public static <K, V> Map<K, V> max(Collection<V> items, Function<V, K> groupFunction, Comparator<V> comparator) {
        Map<K, V> maximums = new HashMap<>();

        for (V item : items) {
            K key = groupFunction.apply(item);
            V maximum = maximums.get(key);

            if (maximum == null || comparator.compare(maximum, item) < 0) {
                maximums.put(key, item);
            }
        }

        return maximums;
    }

    public static interface Function<T, R> {
        public R apply(T value);
    }
}
