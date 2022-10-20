package org.xbib.net.path.spring;

import org.xbib.net.path.spring.util.MultiValueMap;

import java.util.Map;

/**
 * Holder for the result of a match on the start of a pattern.
 * Provides access to the remaining path not matched to the pattern as well
 * as any variables bound in that first part that was matched.
 */
public class PathRemainingMatchInfo {

    private final PathContainer pathMatched;

    private final PathContainer pathRemaining;

    private final PathMatchInfo pathMatchInfo;

    PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining) {
        this(pathMatched, pathRemaining, PathMatchInfo.EMPTY);
    }

    PathRemainingMatchInfo(PathContainer pathMatched, PathContainer pathRemaining,
                           PathMatchInfo pathMatchInfo) {
        this.pathRemaining = pathRemaining;
        this.pathMatched = pathMatched;
        this.pathMatchInfo = pathMatchInfo;
    }

    /**
     * Return the part of a path that was matched by a pattern.
     */
    public PathContainer getPathMatched() {
        return this.pathMatched;
    }

    /**
     * Return the part of a path that was not matched by a pattern.
     */
    public PathContainer getPathRemaining() {
        return this.pathRemaining;
    }

    /**
     * Return variables that were bound in the part of the path that was
     * successfully matched or an empty map.
     */
    public Map<String, String> getUriVariables() {
        return this.pathMatchInfo.getUriVariables();
    }

    /**
     * Return the path parameters for each bound variable.
     */
    public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
        return this.pathMatchInfo.getMatrixVariables();
    }
}
