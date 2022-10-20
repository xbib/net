package org.xbib.net.path.spring;

import org.xbib.net.path.spring.util.MultiValueMap;

import java.util.Collections;
import java.util.Map;

/**
 * Holder for URI variables and path parameters (matrix variables) extracted
 * based on the pattern for a given matched path.
 */
public class PathMatchInfo {

    static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());

    private final Map<String, String> uriVariables;

    private final Map<String, MultiValueMap<String, String>> matrixVariables;

    public PathMatchInfo(Map<String, String> uriVars, Map<String, MultiValueMap<String, String>> matrixVars) {
        this.uriVariables = Collections.unmodifiableMap(uriVars);
        this.matrixVariables = (matrixVars != null ?
                Collections.unmodifiableMap(matrixVars) : Collections.emptyMap());
    }

    /**
     * Return the extracted URI variables.
     */
    public Map<String, String> getUriVariables() {
        return this.uriVariables;
    }

    /**
     * Return maps of matrix variables per path segment, keyed off by URI
     * variable name.
     */
    public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
        return this.matrixVariables;
    }

    @Override
    public String toString() {
        return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " +
                "matrixVariables=" + this.matrixVariables + "]";
    }
}
