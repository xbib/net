package org.xbib.net.path;

import org.xbib.net.QueryParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
class PathStringMatcher {

    private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

    private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

    private final List<String> variableNames = new ArrayList<>();

    private final Pattern pattern;

    PathStringMatcher(String pattern, boolean caseSensitive) {
        StringBuilder patternBuilder = new StringBuilder();
        Matcher matcher = GLOB_PATTERN.matcher(pattern);
        int end = 0;
        while (matcher.find()) {
            patternBuilder.append(quote(pattern, end, matcher.start()));
            String match = matcher.group();
            if ("?".equals(match)) {
                patternBuilder.append('.');
            } else if ("*".equals(match)) {
                patternBuilder.append(".*");
            } else if (match.startsWith("{") && match.endsWith("}")) {
                int colonIdx = match.indexOf(':');
                if (colonIdx == -1) {
                    patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
                    this.variableNames.add(matcher.group(1));
                } else {
                    String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                    patternBuilder.append('(').append(variablePattern).append(')');
                    String variableName = match.substring(1, colonIdx);
                    this.variableNames.add(variableName);
                }
            }
            end = matcher.end();
        }
        patternBuilder.append(quote(pattern, end, pattern.length()));
        this.pattern = caseSensitive ? Pattern.compile(patternBuilder.toString()) :
                Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    private static String quote(String s, int start, int end) {
        if (start == end) {
            return "";
        }
        return Pattern.quote(s.substring(start, end));
    }

    boolean matchStrings(String str, QueryParameters queryParameters) {
        Matcher matcher = this.pattern.matcher(str);
        if (matcher.matches()) {
            if (queryParameters != null) {
                if (this.variableNames.size() != matcher.groupCount()) {
                    throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                            this.pattern + " does not match the number of URI template variables it defines, " +
                            "which can occur if capturing groups are used in a URI template regex. " +
                            "Use non-capturing groups instead.");
                }
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String name = this.variableNames.get(i - 1);
                    String value = matcher.group(i);
                    queryParameters.add(name, value);
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
