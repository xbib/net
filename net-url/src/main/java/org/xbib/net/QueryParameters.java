package org.xbib.net;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class QueryParameters extends ArrayList<QueryParameters.Pair<String, String>> {

    private static final long serialVersionUID = 1195469379836789386L;

    private final int max;

    public QueryParameters() {
        this(1024);
    }

    public QueryParameters(int max) {
        this.max = max;
    }

    public List<String> get(String key) {
        return stream()
                .filter(p -> key.equals(p.getFirst()))
                .map(Pair::getSecond)
                .collect(Collectors.toList());
    }

    public QueryParameters add(String name, String value) {
        add(new Pair<>(name, value));
        return this;
    }

    @Override
    public boolean add(QueryParameters.Pair<String, String> element) {
        return size() < max && super.add(element);
    }

    /**
     * A pair of query parameters.
     * @param <K> the key type parameter
     * @param <V> the value type parameter
     */
    public static class Pair<K, V> {
        private final K first;
        private final V second;

        public Pair(K first, V second) {
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
}
