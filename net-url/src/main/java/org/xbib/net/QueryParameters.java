package org.xbib.net;

import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query parameter list, of limited size. Default is 1024 pairs.
 */
@SuppressWarnings("serial")
public class QueryParameters extends ArrayList<QueryParameters.Pair<String, String>> {

    private static final char AMPERSAND_CHAR = '&';

    private static final char EQUAL_CHAR = '=';

    private final PercentDecoder percentDecoder;

    private final int max;

    public QueryParameters() {
        this(1024);
    }

    public QueryParameters(int max) {
        this.max = max;
        this.percentDecoder = new PercentDecoder();
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

    public QueryParameters addPercentEncodedBody(String body) throws MalformedInputException, UnmappableCharacterException {
        String s = body;
        while (s != null) {
            Pair<String, String> pairs = indexOf(AMPERSAND_CHAR, s);
            Pair<String, String> pair = indexOf(EQUAL_CHAR, pairs.getFirst());
            if (!isNullOrEmpty(pair.getFirst())) {
                add(percentDecoder.decode(pair.getFirst()),
                        percentDecoder.decode(pair.getSecond()));
            }
            s = pairs.getSecond();
        }
        return this;
    }

    /**
     * Returns true if the parameter string is neither null nor empty.
     */
    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static Pair<String, String> indexOf(char ch, String input) {
        int i = input.indexOf(ch);
        String k = i >= 0 ? input.substring(0, i) : input;
        String v = i >= 0 ? input.substring(i + 1) : null;
        return new Pair<>(k, v);
    }

    /**
     * A pair of query parameters.
     * @param <K> the key type parameter
     * @param <V> the value type parameter
     */
    public static class Pair<K, V> {
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
}
