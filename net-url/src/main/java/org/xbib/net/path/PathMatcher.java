package org.xbib.net.path;

import org.xbib.net.QueryParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Path matcher.
 */
public class PathMatcher {

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private final Map<String, List<String>> tokenizedPatternCache =
            Collections.synchronizedMap(new LRUCache<>(1024));

    private final Map<String, PathStringMatcher> stringMatcherCache =
            Collections.synchronizedMap(new LRUCache<>(1024));

    private String pathSeparator;

    private PathSeparatorPatternCache pathSeparatorPatternCache;

    private boolean caseSensitive = true;

    private boolean trimTokens = true;

    public PathMatcher() {
        this(DEFAULT_PATH_SEPARATOR);
    }

    public PathMatcher(String pathSeparator) {
        this.pathSeparator = pathSeparator;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public void setTrimTokens(boolean trimTokens) {
        this.trimTokens = trimTokens;
    }

    public QueryParameters extractUriTemplateVariables(String pattern, String path) {
        QueryParameters queryParameters = new QueryParameters();
        if (!doMatch(pattern, path, true, queryParameters)) {
            throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        }
        return queryParameters;
    }

    public Map<String, PathStringMatcher> stringMatcherCache() {
        return stringMatcherCache;
    }

    public boolean match(String pattern, String path) {
        return doMatch(pattern, path, true, null);
    }

    public boolean matchStart(String pattern, String path) {
        return doMatch(pattern, path, false, null);
    }

    public String extractPathWithinPattern(String pattern, String path) {
        List<String> patternParts = tokenize(pattern, pathSeparator, trimTokens);
        List<String> pathParts = tokenize(path, pathSeparator, trimTokens);
        StringBuilder sb = new StringBuilder();
        boolean pathStarted = false;
        for (int segment = 0; segment < patternParts.size(); segment++) {
            String patternPart = patternParts.get(segment);
            if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
                while (segment < pathParts.size()) {
                    if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
                        sb.append(pathSeparator);
                    }
                    sb.append(pathParts.get(segment));
                    pathStarted = true;
                    segment++;
                }
            }
        }
        return sb.toString();
    }

    public String combine(String pattern1, String pattern2) {
        if (hasNotText(pattern1) && hasNotText(pattern2)) {
            return "";
        }
        if (hasNotText(pattern1)) {
            return pattern2;
        }
        if (hasNotText(pattern2)) {
            return pattern1;
        }
        boolean pattern1ContainsUriVar = pattern1.indexOf('{') != -1;
        if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
            return pattern2;
        }
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
            return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
        }
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
            return concat(pattern1, pattern2);
        }
        int starDotPos1 = pattern1.indexOf("*.");
        if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
            return concat(pattern1, pattern2);
        }
        String ext1 = pattern1.substring(starDotPos1 + 1);
        int dotPos2 = pattern2.indexOf('.');
        String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
        String ext2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
        boolean ext1All = (ext1.equals(".*") || ext1.equals(""));
        boolean ext2All = (ext2.equals(".*") || ext2.equals(""));
        if (!ext1All && !ext2All) {
            throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
        }
        String ext = ext1All ? ext2 : ext1;
        return file2 + ext;
    }

    public Comparator<String> getPatternComparator(String path) {
        return new PathPatternComparator(path);
    }

    private static boolean hasNotText(CharSequence str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String concat(String path1, String path2) {
        boolean path1EndsWithSeparator = path1.endsWith(this.pathSeparator);
        boolean path2StartsWithSeparator = path2.startsWith(this.pathSeparator);
        if (path1EndsWithSeparator && path2StartsWithSeparator) {
            return path1 + path2.substring(1);
        } else if (path1EndsWithSeparator || path2StartsWithSeparator) {
            return path1 + path2;
        } else {
            return path1 + this.pathSeparator + path2;
        }
    }

    private boolean doMatch(String pattern, String path, boolean fullMatch, QueryParameters queryParameters) {
        if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        }
        List<String> patternElements = tokenizePattern(pattern);
        List<String> pathElements = tokenizePath(path);
        int pattIdxStart = 0;
        int pattIdxEnd = patternElements.size() - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathElements.size() - 1;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = patternElements.get(pattIdxStart);
            if ("**".equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattDir, pathElements.get(pathIdxStart), queryParameters)) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            if (pattIdxStart > pattIdxEnd) {
                return pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd
                    && patternElements.get(pattIdxStart).equals("*")
                    && path.endsWith(this.pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!patternElements.get(i).equals("**")) {
                    return false;
                }
            }
            return true;
        } else if (pattIdxStart > pattIdxEnd) {
            return false;
        } else if (!fullMatch && "**".equals(patternElements.get(pattIdxStart))) {
            return true;
        }
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = patternElements.get(pattIdxEnd);
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattDir, pathElements.get(pathIdxEnd), queryParameters)) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!patternElements.get(i).equals("**")) {
                    return false;
                }
            }
            return true;
        }
        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (patternElements.get(i).equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                pattIdxStart++;
                continue;
            }
            int patLength = patIdxTmp - pattIdxStart - 1;
            int strLength = pathIdxEnd - pathIdxStart + 1;
            int foundIdx = -1;
            boolean strLoop = true;
            while (strLoop) {
                for (int i = 0; i <= strLength - patLength; i++) {
                    for (int j = 0; j < patLength; j++) {
                        String subPat = patternElements.get(pattIdxStart + j + 1);
                        String subStr = pathElements.get(pathIdxStart + i + j);
                        if (matchStrings(subPat, subStr, queryParameters)) {
                            strLoop = false;
                            break;
                        }
                    }
                    if (strLoop) {
                        foundIdx = pathIdxStart + i;
                    } else {
                        break;
                    }
                }
            }
            if (foundIdx == -1) {
                return false;
            }
            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }
        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!patternElements.get(i).equals("**")) {
                return false;
            }
        }
        return true;
    }

    private List<String> tokenizePattern(String pattern) {
        return tokenizedPatternCache.computeIfAbsent(pattern, this::tokenizePath);
    }

    public List<String> tokenizePath(String path) {
        return tokenize(path, pathSeparator, trimTokens);
    }

    private static List<String> tokenize(String str, String delimiters, boolean trimTokens) {
        List<String> tokens = new ArrayList<>();
        if (str == null) {
            return tokens;
        }
        StringTokenizer st = new StringTokenizer(str, delimiters);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean matchStrings(String pattern, String str, QueryParameters queryParameters) {
        return getStringMatcher(pattern).matchStrings(str, queryParameters);
    }

    private PathStringMatcher getStringMatcher(String pattern) {
        return stringMatcherCache.computeIfAbsent(pattern, p -> new PathStringMatcher(p, this.caseSensitive));
    }

    /**
     * A simple LRU cache, based on a {@link LinkedHashMap}.
     *
     * @param <K> the key type parameter
     * @param <V> the vale type parameter
     */
    @SuppressWarnings("serial")
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {

        private final int cacheSize;

        LRUCache(int cacheSize) {
            super(16, 0.75f, true);
            this.cacheSize = cacheSize;
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() >= cacheSize;
        }
    }
}
