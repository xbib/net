package org.xbib.net.path.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;

/**
 * Path matcher. The methods of this class are threadsafe.
 *
 * This is based on org.springframework.util.AntPathMatcher
 */
public class PathMatcher {

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private String pathSeparator;

    private String endsOnWildCard;

    private String endsOnDoubleWildCard;

    private boolean caseSensitive;

    private boolean trimTokens;

    public PathMatcher() {
        this(DEFAULT_PATH_SEPARATOR);
    }

    public PathMatcher(String pathSeparator) {
        setPathSeparator(pathSeparator);
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR;
        this.endsOnWildCard = this.pathSeparator + "*";
        this.endsOnDoubleWildCard = this.pathSeparator + "**";
        this.caseSensitive = true;
        this.trimTokens = true;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public void setTrimTokens(boolean trimTokens) {
        this.trimTokens = trimTokens;
    }

    public Parameter extractUriTemplateVariables(String pattern, String path) {
        ParameterBuilder queryParameters = Parameter.builder();
        if (!doMatch(pattern, path, true, queryParameters)) {
            throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        }
        return queryParameters.build();
    }

    public PathComparator getPatternComparator(String path) {
        return new PathComparator(path);
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
        if (pattern1.endsWith(endsOnWildCard)) {
            return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
        }
        if (pattern1.endsWith(endsOnDoubleWildCard)) {
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
        boolean path1EndsWithSeparator = path1.endsWith(pathSeparator);
        boolean path2StartsWithSeparator = path2.startsWith(pathSeparator);
        if (path1EndsWithSeparator && path2StartsWithSeparator) {
            return path1 + path2.substring(1);
        } else if (path1EndsWithSeparator || path2StartsWithSeparator) {
            return path1 + path2;
        } else {
            return path1 + pathSeparator + path2;
        }
    }

    private boolean doMatch(String pattern, String path, boolean fullMatch, ParameterBuilder queryParameters) {
        if (path.startsWith(pathSeparator) != pattern.startsWith(pathSeparator)) {
            return false;
        }
        List<String> patternElements = tokenize(pattern, pathSeparator, trimTokens);
        List<String> pathElements = tokenize(path, pathSeparator, trimTokens);
        int pattIdxStart = 0;
        int pattIdxEnd = patternElements.size() - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathElements.size() - 1;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String patternString = patternElements.get(pattIdxStart);
            if ("**".equals(patternString)) {
                break;
            }
            if (!matchStrings(patternString, pathElements.get(pathIdxStart), queryParameters)) {
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

    public List<String> tokenize(String string) {
        return tokenize(string, pathSeparator, trimTokens);
    }

    private static List<String> tokenize(String string, String delimiters, boolean trimTokens) {
        List<String> tokens = new ArrayList<>();
        if (string == null) {
            return tokens;
        }
        StringTokenizer st = new StringTokenizer(string, delimiters);
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

    private boolean matchStrings(String patternString, String str, ParameterBuilder queryParameters) {
        return new PathStringMatcher(patternString, caseSensitive).match(str, queryParameters);
    }
}
