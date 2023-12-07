package org.xbib.net.path;

import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.PathNormalizer;

import java.nio.charset.Charset;

public class PathDecoder {

    private final String path;

    private final String query;

    private final Charset charset;

    private final ParameterBuilder params;

    public PathDecoder(String pathAndQuery, Charset charset) {
        this(pathAndQuery, null, charset);
    }

    public PathDecoder(String pathAndQuery, String queryString, Charset charset) {
        this.charset = charset;
        int pos = pathAndQuery.indexOf('?');
        String path = pos > 0 ? pathAndQuery.substring(0, pos) : pathAndQuery;
        this.query = pos > 0 ? pathAndQuery.substring(pos + 1) : null;
        this.path = PathNormalizer.normalize(path);
        this.params = Parameter.builder().domain(Parameter.Domain.PATH).enablePercentDecoding();
        if (query != null) {
            this.params.add(query, charset);
        }
        if (queryString != null) {
            this.params.add(queryString, charset);
        }
    }

    public void parse(String queryString) {
        this.params.add(queryString, charset);
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
