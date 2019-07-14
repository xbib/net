package org.xbib.net;

/**
 * A pair of parameters.
 * @param <K> the key type parameter
 * @param <V> the value type parameter
 */
public class Pair<K, V> {

    private final K first;

    private final V second;

    Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return first + ":" + second;
    }
}
