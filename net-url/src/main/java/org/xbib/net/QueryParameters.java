package org.xbib.net;

import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query parameter list, of limited size. Default is 1024 pairs.
 */
@SuppressWarnings("serial")
public class QueryParameters extends ArrayList<Pair<String, String>> {

    private static final char AMPERSAND_CHAR = '&';

    private static final char EQUAL_CHAR = '=';

    private transient final PercentDecoder percentDecoder;

    private final int max;

    public QueryParameters() {
        this(1024);
    }

    public QueryParameters(int max) {
        this(StandardCharsets.UTF_8, max);
    }

    public QueryParameters(Charset charset) {
        this(charset, 1024);
    }

    public QueryParameters(Charset charset, int max) {
        this(new PercentDecoder(charset.newDecoder()
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .onMalformedInput(CodingErrorAction.REPLACE)), max);
    }


    public QueryParameters(PercentDecoder percentDecoder) {
        this(percentDecoder, 1024);
    }

    public QueryParameters(PercentDecoder percentDecoder, int max) {
        this.percentDecoder = percentDecoder;
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
    public boolean add(Pair<String, String> element) {
        return size() < max && super.add(element);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof QueryParameters && super.equals(o);
    }

    public QueryParameters addPercentEncodedBody(String body) {
        String s = body;
        while (s != null) {
            Pair<String, String> pairs = indexOf(AMPERSAND_CHAR, s);
            Pair<String, String> pair = indexOf(EQUAL_CHAR, pairs.getFirst());
            if (!isNullOrEmpty(pair.getFirst())) {
                try {
                    add(percentDecoder.decode(pair.getFirst()),
                            percentDecoder.decode(pair.getSecond()));
                } catch (MalformedInputException e) {
                    // never thrown
                } catch (UnmappableCharacterException e) {
                   // never thrown
                }
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
}
