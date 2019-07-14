package org.xbib.net.http.util;

import java.util.SortedSet;
import java.util.TreeMap;

/**
 * Tree Map with a limited number of elements.
 *
 * @param <K> key
 * @param <V> value
 */
@SuppressWarnings("serial")
public class LimitedTreeMap<K, V> extends TreeMap<K, SortedSet<V>> {

    private final int limit;

    public LimitedTreeMap() {
        this(1024);
    }

    public LimitedTreeMap(int limit) {
        this.limit = limit;
    }

    @Override
    public SortedSet<V> put(K key, SortedSet<V> value) {
        if (size() < limit) {
            return super.put(key, value);
        }
        return null;
    }
}
