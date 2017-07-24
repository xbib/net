package org.xbib.net.internal;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple LRU cache, based on a {@link LinkedHashMap}.
 *
 * @param <K> the key type parameter
 * @param <V> the vale type parameter
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = -2795566703268944901L;

    private final int cacheSize;

    public LRUCache(int cacheSize) {
        super(16, 0.75f, true);
        this.cacheSize = cacheSize;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= cacheSize;
    }
}
