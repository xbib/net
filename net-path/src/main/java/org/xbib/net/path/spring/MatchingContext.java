package org.xbib.net.path.spring;

import org.xbib.net.path.spring.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates context when attempting a match. Includes some fixed state like the
 * candidate currently being considered for a match but also some accumulators for
 * extracted variables.
 */
public class MatchingContext {

    private final PathPattern pathPattern;

    public final PathContainer candidate;

    public final List<PathContainer.Element> pathElements;

    public final int pathLength;

    private Map<String, String> extractedUriVariables;

    private Map<String, MultiValueMap<String, String>> extractedMatrixVariables;

    public boolean extractingVariables;

    public boolean determineRemainingPath = false;

    // if determineRemaining is true, this is set to the position in
    // the candidate where the pattern finished matching - i.e. it
    // points to the remaining path that wasn't consumed
    public int remainingPathIndex;

    public MatchingContext(PathPattern pathPattern, PathContainer pathContainer, boolean extractVariables) {
        this.pathPattern = pathPattern;
        this.candidate = pathContainer;
        this.pathElements = pathContainer.elements();
        this.pathLength = this.pathElements.size();
        this.extractingVariables = extractVariables;
    }

    public void setMatchAllowExtraPath() {
        this.determineRemainingPath = true;
    }

    public boolean isMatchOptionalTrailingSeparator() {
        return pathPattern.matchOptionalTrailingSeparator;
    }

    public void set(String key, String value, MultiValueMap<String, String> parameters) {
        if (this.extractedUriVariables == null) {
            this.extractedUriVariables = new HashMap<>();
        }
        this.extractedUriVariables.put(key, value);
        if (!parameters.isEmpty()) {
            if (this.extractedMatrixVariables == null) {
                this.extractedMatrixVariables = new HashMap<>();
            }
            this.extractedMatrixVariables.put(key, parameters);
        }
    }

    public PathMatchInfo getPathMatchResult() {
        if (this.extractedUriVariables == null) {
            return PathMatchInfo.EMPTY;
        } else {
            return new PathMatchInfo(this.extractedUriVariables, this.extractedMatrixVariables);
        }
    }

    /**
     * Return if element at specified index is a separator.
     *
     * @param pathIndex possible index of a separator
     * @return {@code true} if element is a separator
     */
    public boolean isSeparator(int pathIndex) {
        return this.pathElements.get(pathIndex) instanceof PathContainer.Separator;
    }

    /**
     * Return the decoded value of the specified element.
     *
     * @param pathIndex path element index
     * @return the decoded value
     */
    public String pathElementValue(int pathIndex) {
        PathContainer.Element element = (pathIndex < this.pathLength) ? this.pathElements.get(pathIndex) : null;
        if (element instanceof PathContainer.PathSegment) {
            PathContainer.PathSegment pathSegment = (PathContainer.PathSegment) element;
            return pathSegment.valueToMatch();
        }
        return "";
    }
}
