package org.processmining.models.semantics.petrinet.impl;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NormalizedMarkingCache {

    private static NormalizedMarkingCache instance;

    private LruCache<String, List<Integer>[]> cache;

    public static NormalizedMarkingCache getInstance() {
        if (instance == null) {
            instance = new NormalizedMarkingCache();
        }
        return instance;
    }

    private NormalizedMarkingCache() {
        cache = new LruCache<>(10);
    }

    public List<Integer>[] getMarking(String key) {
        List<Integer>[] result;
        if (!cache.containsKey(key)) {
            result = fromKey(key);
            cache.put(key, result);
        } else {
            result = cache.get(key);
        }
        return result;
    }

//	public static CachedNormalizedMarking getMarking(ArrayList<Integer>[] marking){
//		String key = getKey(marking);
//		CachedNormalizedMarking value = null;
//		if (!cache.containsKey(key)){
//			value = new CachedNormalizedMarking();
//			value.normalizedMarking = marking;
//			cache.put(key, value); 
//		} else {
//			value = cache.get(key);
//		}
//		return value;
//	}


    public void clearCache() {
        cache.clear();
    }

    public String getKey(List<Integer>[] marking) {
        long before = System.nanoTime();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < marking.length; i++) {
            if (i > 0) buf.append(";");
            if (marking[i] != null) {
                for (int j = 0; j < marking[i].size(); j++) {
                    if (j > 0) buf.append(",");
                    buf.append(marking[i].get(j));
                }
            }
        }
        long before2 = System.nanoTime();
        String key = buf.toString().intern();
        long timeTakenToInternalize = System.nanoTime() - before2;
        cache.put(key, marking);
        long timeTaken = System.nanoTime() - before;
        System.out.println(timeTaken + ", " + timeTakenToInternalize);
        return key;
    }

    private static List<Integer>[] fromKey(String key) {
        String[] parts = key.split(";", 1000000);
        List<Integer>[] result = new LinkedList[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] tokens = parts[i].split(",", 1000);
            List<Integer> placeTokens = new LinkedList<>();
            for (int j = 0; j < tokens.length; j++) {
                if (!tokens[j].isEmpty()) {
                    placeTokens.add(Integer.valueOf(tokens[j]));
                }
            }
            if (!placeTokens.isEmpty()) {
                result[i] = placeTokens;
            }
        }
        return result;
    }


//	public int hashCode() {
//		return getKey(normalizedMarking).hashCode();
//	}

//
//	public boolean equals(Object obj) {
//		if (obj == null) return false;
//		
//		if (obj instanceof NormalizedMarkingCache){
//			return obj.toString().equals(toString());
//		} else {
//			return false;
//		}
//	}
//
//	public String toString() {
//		return getKey(normalizedMarking);
//	}

    private class LruCache<A, B> extends LinkedHashMap<A, B> {
        private final int maxEntries;

        public LruCache(final int maxEntries) {
            super(maxEntries + 1, 1.0f, true);
            this.maxEntries = maxEntries;
        }

        /**
         * Returns <tt>true</tt> if this <code>LruCache</code> has more entries than the maximum specified when it was
         * created.
         * <p>
         * <p>
         * This method <em>does not</em> modify the underlying <code>Map</code>; it relies on the implementation of
         * <code>LinkedHashMap</code> to do that, but that behavior is documented in the JavaDoc for
         * <code>LinkedHashMap</code>.
         * </p>
         *
         * @param eldest the <code>Entry</code> in question; this implementation doesn't care what it is, since the
         *               implementation is only dependent on the size of the cache
         * @return <tt>true</tt> if the oldest
         * @see java.util.LinkedHashMap#removeEldestEntry(Map.Entry)
         */
        @Override
        protected boolean removeEldestEntry(final Map.Entry<A, B> eldest) {
            return super.size() > maxEntries;
        }
    }

}
