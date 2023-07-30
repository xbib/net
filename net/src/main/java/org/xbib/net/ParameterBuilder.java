package org.xbib.net;

import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.xbib.datastructures.common.ImmutableList;
import org.xbib.datastructures.common.MultiMap;
import org.xbib.datastructures.common.Pair;
import org.xbib.datastructures.common.PairValidator;

public class ParameterBuilder implements PairValidator {

    @SuppressWarnings("unchecked")
    private static final Pair<String, Object>[] EMPTY_PAIR = (Pair<String, Object>[]) Array.newInstance(Pair.class, 0);

    private static final char AMPERSAND_CHAR = '&';

    private static final char EQUAL_CHAR = '=';

    private static final Integer MAX_PARAMS_IN_QUERY_STRING = 1024;

    private final List<Pair<String, Object>> list;

    protected Parameter.Domain domain;

    final Map<Parameter.Domain, Parameter> parameterMap;

    private Charset charset;

    private PercentEncoder percentEncoder;

    private PercentDecoder percentDecoder;

    private int limit;

    private PairValidator pairValidator;

    private ParameterValidator parameterValidator;

    private boolean enableLowerCaseNames;

    private boolean enablePercentEncoding;

    private boolean enableQueryStringPercentEncoding;

    private boolean enablePercentDecoding;

    private boolean enableSort;

    private boolean enableDuplicates;

    private boolean enableQueryString;

    ParameterBuilder() {
        this.list = new ArrayList<>();
        this.parameterMap = new HashMap<>();
        this.domain = Parameter.Domain.UNDEFINED;
        this.limit = 0;
    }

    ParameterBuilder(ParameterBuilder builder) {
        this.list = builder.list;
        this.parameterMap = builder.parameterMap;
        this.domain = builder.domain;
        this.limit = builder.limit;
        this.charset = builder.charset;
        this.percentDecoder = builder.percentDecoder;
        this.percentEncoder = builder.percentEncoder;
        this.enableLowerCaseNames = builder.enableLowerCaseNames;
        this.enablePercentDecoding = builder.enablePercentDecoding;
        this.enablePercentEncoding = builder.enablePercentEncoding;
        this.enableQueryString = builder.enableQueryString;
        this.enableQueryStringPercentEncoding = builder.enableQueryStringPercentEncoding;
        this.enableSort = builder.enableSort;
        this.enableDuplicates = builder.enableDuplicates;
        this.pairValidator = builder.pairValidator;
        this.parameterValidator = builder.parameterValidator;
    }

    public ParameterBuilder lowercase() {
        this.enableLowerCaseNames = true;
        return this;
    }

    public ParameterBuilder charset(Charset charset) {
        this.charset = charset;
        this.percentEncoder = PercentEncoders.getQueryParamEncoder(charset);
        this.percentDecoder = new PercentDecoder(charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE));
        return this;
    }

    public ParameterBuilder percentEncoder(PercentEncoder percentEncoder) {
        this.percentEncoder = percentEncoder;
        return this;
    }

    public ParameterBuilder percentDecode(PercentDecoder percentDecoder) {
        this.percentDecoder = percentDecoder;
        return this;
    }

    public ParameterBuilder enablePercentEncoding() {
        this.enablePercentEncoding = true;
        charset(StandardCharsets.UTF_8);
        return this;
    }

    public ParameterBuilder enablePercentDecoding() {
        this.enablePercentDecoding = true;
        charset(StandardCharsets.UTF_8);
        return this;
    }

    public ParameterBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public ParameterBuilder pairValidator(PairValidator validator) {
        this.pairValidator = validator;
        return this;
    }

    public ParameterBuilder parameterValidator(ParameterValidator validator) {
        this.parameterValidator = validator;
        return this;
    }

    public ParameterBuilder domain(Parameter.Domain domain) {
        this.domain = domain;
        return this;
    }

    public ParameterBuilder enableSort() {
        this.enableSort = true;
        return this;
    }

    public ParameterBuilder enableDuplicates() {
        this.enableDuplicates = true;
        return this;
    }

    public ParameterBuilder enableQueryString(boolean enableQueryStringPercentEncoding) {
        this.enableQueryString = true;
        this.enableQueryStringPercentEncoding = enableQueryStringPercentEncoding;
        charset(StandardCharsets.UTF_8);
        return this;
    }

    public ParameterBuilder add(Map<String, Object> map) {
        map.forEach(this::add);
        return this;
    }

    public ParameterBuilder add(MultiMap<String, Object> map) {
        map.asMap().forEach(this::add);
        return this;
    }

    public ParameterBuilder add(Parameter parameter) {
        if (parameterMap.containsKey(parameter.getDomain())) {
            throw new IllegalArgumentException("unable to add domain " + parameter.getDomain());
        }
        parameterMap.putIfAbsent(parameter.getDomain(), parameter);
        return this;
    }

    public ParameterBuilder addPercentEncodedBody(String body) {
        if (body == null) {
            return this;
        }
        if (percentDecoder == null) {
            charset(StandardCharsets.UTF_8);
        }
        // watch out for "plus" encoding, replace it with a space character
        String s = body.replace('+', ' ');
        while (s != null) {
            Pair<String, Object> pairs = indexOf(AMPERSAND_CHAR, s);
            Pair<String, Object> pair = indexOf(EQUAL_CHAR, pairs.getKey());
            if (pair.getKey() != null && !pair.getKey().isEmpty()) {
                try {
                    add(percentDecoder.decode(pair.getKey()),
                            pair.getValue() instanceof CharSequence ? percentDecoder.decode((CharSequence) pair.getValue()) : pair.getValue());
                } catch (MalformedInputException | UnmappableCharacterException e) {
                    throw new UncheckedIOException(e);
                }
            }
            s = pairs.getValue() !=null ? pairs.getValue().toString() : null;
        }
        return this;
    }

    public ParameterBuilder add(Pair<String, Object> pair) {
        add(pair.getKey(), pair.getValue());
        return this;
    }

    public ParameterBuilder add(String name, Object value) {
        if (limit > 0 && list.size() >= limit) {
            throw new IllegalArgumentException("parameter limit " + limit + " exceeded");
        }
        Pair<String, Object> pair = apply(Pair.of(name, value));
        if (pair != null) {
            if (enableDuplicates || !list.contains(pair)) {
                list.add(pair);
            }
        }
        return this;
    }

    public ParameterBuilder add(String percentEncodedQueryString) {
        try {
            decodeQueryString(percentEncodedQueryString);
        } catch (MalformedInputException | UnmappableCharacterException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public Parameter build() {
        if (enableSort) {
            list.sort(Comparator.comparing(Pair::getKey));
        }
        String queryString = null;
        if (enableQueryString) {
            try {
                queryString = encodeQueryString();
            } catch (MalformedInputException | UnmappableCharacterException e) {
                throw new UncheckedIOException(e);
            }
        }
        Parameter parameter = new Parameter(this, ImmutableList.of(list, EMPTY_PAIR), queryString);
        if (parameterValidator != null) {
            return parameterValidator.apply(parameter);
        } else {
            return parameter;
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public Pair<String, Object> apply(Pair<String, Object> pair) {
        Object theValue = pair.getValue();
        if (pairValidator != null)  {
            return pairValidator.apply(pair);
        }
        String theName = pair.getKey();
        if (enableLowerCaseNames) {
            theName = theName.toLowerCase(Locale.ROOT);
        }
        if (charset != null && !charset.equals(Charset.defaultCharset())) {
            theName = new String(theName.getBytes(charset));
        }
        if (enablePercentEncoding) {
            try {
                theName = percentEncoder.encode(theName);
                if (theValue instanceof CharSequence) {
                    theValue = percentEncoder.encode((CharSequence) theValue);
                }
            } catch (MalformedInputException | UnmappableCharacterException e) {
                // never thrown
                throw new UncheckedIOException(e);
            }
        }
        if (enablePercentDecoding) {
            try {
                theName = percentDecoder.decode(theName);
                if (theValue instanceof CharSequence) {
                    theValue = percentDecoder.decode((CharSequence) theValue);
                }
            } catch (MalformedInputException | UnmappableCharacterException e) {
                // never thrown
                throw new UncheckedIOException(e);
            }
        }
        return Pair.of(theName, theValue);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<Pair<String, Object>> iterator() {
        return list.iterator();
    }

    private String encodeQueryString()
            throws MalformedInputException, UnmappableCharacterException {
        Iterator<Pair<String, Object>> it = list.iterator();
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            Pair<String, Object> p = it.next();
            String k = (enableQueryStringPercentEncoding ? percentEncoder.encode(p.getKey()) : p.getKey());
            String v = p.getValue() != null ?
                    (enableQueryStringPercentEncoding && p.getValue() instanceof CharSequence ?
                            percentEncoder.encode((CharSequence) p.getValue()) : (p.getValue() != null ? p.getValue().toString() : "")) : "";
            sb.append(k).append(EQUAL_CHAR).append(v);
            if (it.hasNext()) {
                sb.append(AMPERSAND_CHAR);
            }
        }
        return sb.toString();
    }

    private void decodeQueryString(String query)
            throws MalformedInputException, UnmappableCharacterException {
        if (query == null || query.isEmpty()) {
            return;
        }
        String name = null;
        int count = 0;
        int pos = 0;
        int i;
        char c;
        for (i = 0; i < query.length(); i++) {
            c = query.charAt(i);
            if (c == '=' && name == null) {
                if (pos != i) {
                    name = query.substring(pos, i).replaceAll("\\+", "%20");
                    name = percentDecoder.decode(name);
                }
                pos = i + 1;
            } else if (c == '&' || c == ';') {
                if (name == null && pos != i) {
                    if (++count > MAX_PARAMS_IN_QUERY_STRING) {
                        return;
                    }
                    String s = query.substring(pos, i).replaceAll("\\+", "%20");
                    add(percentDecoder.decode(s), "");
                } else if (name != null) {
                    if (++count > MAX_PARAMS_IN_QUERY_STRING) {
                        return;
                    }
                    String value = query.substring(pos, i).replaceAll("\\+", "%20");
                    add(name, percentDecoder.decode(value));
                    name = null;
                }
                pos = i + 1;
            }
        }
        if (pos != i) {
            if (name == null) {
                add(percentDecoder.decode(query.substring(pos, i)), "");
            } else {
                String value = query.substring(pos, i).replaceAll("\\+", "%20");
                add(name, percentDecoder.decode(value));
            }
        } else if (name != null) {
            add(name, "");
        }
    }

    private static Pair<String, Object> indexOf(char ch, String input) {
        int i = input.indexOf(ch);
        String k = i >= 0 ? input.substring(0, i) : input;
        Object v = i >= 0 ? input.substring(i + 1) : null;
        return Pair.of(k, v);
    }
}
