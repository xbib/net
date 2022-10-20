package org.xbib.net.path;

import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.PathNormalizer;

public class PathDecoder {

    private final String path;

    private final String query;

    private final ParameterBuilder params;

    public PathDecoder(String pathAndQuery) {
        this(pathAndQuery, null);
    }

    public PathDecoder(String pathAndQuery, String queryString) {
        int pos = pathAndQuery.indexOf('?');
        String path = pos > 0 ? pathAndQuery.substring(0, pos) : pathAndQuery;
        this.query = pos > 0 ? pathAndQuery.substring(pos + 1) : null;
        this.path = PathNormalizer.normalize(path);
        this.params = Parameter.builder().enablePercentDeccoding();
        if (query != null) {
            parse(query);
        }
        if (queryString != null) {
            parse(queryString);
        }
    }

    public void parse(String queryString) {
        this.params.add(queryString);
    }

    public String path() {
        return path;
    }

    public String query() {
        return query;
    }

    public Parameter getParameter() {
        return params.build();
    }
}
