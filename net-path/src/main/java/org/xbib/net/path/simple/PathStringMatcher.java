package org.xbib.net.path.simple;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xbib.net.ParameterBuilder;

public class PathStringMatcher {

    private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?}|[^/{}]|\\\\[{}])+?)}");

    private static final String DEFAULT_VARIABLE_PATTERN = "(.*)";

    private final String patternString;

    private final List<String> variableNames = new ArrayList<>();

    private final Pattern pattern;

    public PathStringMatcher(String patternString, boolean caseSensitive) {
        this.patternString = patternString;
        StringBuilder sb = new StringBuilder();
        Matcher matcher = GLOB_PATTERN.matcher(patternString);
        int start = 0;
        while (matcher.find()) {
            sb.append(quote(patternString, start, matcher.start()));
            String match = matcher.group();
            if ("?".equals(match)) {
                sb.append('.');
            } else if ("*".equals(match)) {
                sb.append(".*");
            } else if (match.startsWith("{") && match.endsWith("}")) {
                int colonIdx = match.indexOf(':');
                if (colonIdx == -1) {
                    sb.append(DEFAULT_VARIABLE_PATTERN);
                    this.variableNames.add(matcher.group(1));
                } else {
                    String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                    sb.append('(').append(variablePattern).append(')');
                    String variableName = match.substring(1, colonIdx);
                    this.variableNames.add(variableName);
                }
            }
            start = matcher.end();
        }
        sb.append(quote(patternString, start, patternString.length()));
        this.pattern = caseSensitive ? Pattern.compile(sb.toString()) :
                Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }

    public String getPatternString() {
        return patternString;
    }

    public List<String> getVariableNames() {
        return variableNames;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean match(String string, ParameterBuilder queryParameters) {
        Matcher matcher = pattern.matcher(string);
        if (matcher.matches()) {
            if (queryParameters != null) {
                if (variableNames.size() != matcher.groupCount()) {
                    throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                            pattern + " does not match the number of URI template variables it defines, " +
                            "which can occur if capturing groups are used in a URI template regex. " +
                            "Use non-capturing groups instead.");
                }
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String name = variableNames.get(i - 1);
                    String value = matcher.group(i);
                    queryParameters.add(name, value);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static String quote(String s, int start, int end) {
        return start == end ? "" : Pattern.quote(s.substring(start, end));
    }
}
