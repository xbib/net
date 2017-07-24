package org.xbib.net.path;

import org.xbib.net.PercentDecoder;
import org.xbib.net.QueryParameters;

import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;

/**
 *
 */
public class PathDecoder {

    private static final Integer MAX_PARAM_COUNT = 1000;

    private PercentDecoder decoder;

    private String path;

    private String query;

    private QueryParameters params;

    public PathDecoder(String pathAndQuery) throws MalformedInputException, UnmappableCharacterException {
        this(pathAndQuery, StandardCharsets.UTF_8);
    }

    public PathDecoder(String pathAndQuery, Charset charset)
            throws MalformedInputException, UnmappableCharacterException {
        this(pathAndQuery, null, charset);
    }

    public PathDecoder(String pathAndQuery, String queryString, Charset charset)
            throws MalformedInputException, UnmappableCharacterException {
        this.decoder = new PercentDecoder(charset.newDecoder());
        int pos = pathAndQuery.indexOf('?');
        String path = pos > 0 ? pathAndQuery.substring(0, pos) : pathAndQuery;
        this.query = pos > 0 ? pathAndQuery.substring(pos + 1) : null;
        this.path = PathNormalizer.normalize(path);
        this.params = new QueryParameters();
        if (query != null) {
            parse(query);
        }
        if (queryString != null) {
            parse(queryString);
        }
    }

    public void parse(String queryString)
            throws MalformedInputException, UnmappableCharacterException {
        this.params.addAll(decodeQueryString(decoder, queryString));
    }

    public String path() {
        return path;
    }

    public String query() {
        return query;
    }

    public String decodedQuery() throws MalformedInputException, UnmappableCharacterException {
        return decoder.decode(query);
    }

    public QueryParameters params() {
        return params;
    }

    private static QueryParameters decodeQueryString(PercentDecoder decoder, String query)
            throws MalformedInputException, UnmappableCharacterException {
        QueryParameters params = new QueryParameters();
        if (query == null || query.isEmpty()) {
            return params;
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
                    name = decoder.decode(name);
                }
                pos = i + 1;
            } else if (c == '&' || c == ';') {
                if (name == null && pos != i) {
                    if (++count > MAX_PARAM_COUNT) {
                        return params;
                    }
                    String s = query.substring(pos, i).replaceAll("\\+", "%20");
                    params.add(decoder.decode(s), "");
                } else if (name != null) {
                    if (++count > MAX_PARAM_COUNT) {
                        return params;
                    }
                    String value = query.substring(pos, i).replaceAll("\\+", "%20");
                    params.add(name, decoder.decode(value));
                    name = null;
                }
                pos = i + 1;
            }
        }
        if (pos != i) {
            if (name == null) {
                params.add(decoder.decode(query.substring(pos, i)), "");
            } else {
                String value = query.substring(pos, i).replaceAll("\\+", "%20");
                params.add(name, decoder.decode(value));
            }
        } else if (name != null) {
            params.add(name, "");
        }
        return params;
    }
}
