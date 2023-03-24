package org.xbib.net.path.structure;

import org.xbib.net.Parameter;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.PathNormalizer;
import org.xbib.net.util.CharMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathMatcher extends Path {

    private static final Pattern GLOB_PATTERN = Pattern.compile("\\?|\\*\\*|\\*|\\{((?:\\{[^/]+?}|[^/{}]|\\\\[{}])+?)}");

    private static final Pattern MATCH_ALL_PATTERN = Pattern.compile("(.*)");

    private static final CharMatcher RESERVED_URL_CHARS = CharMatcher.anyOf(":/?#[]{}");

    private static final String DEFAULT_PATTERN = "(.*)";

    private static final String WILDCARD = "*";

    private final boolean fullMatch;

    private List<PathSegment> analyzedSegments;

    private final ParameterBuilder parameterBuilder;

    public PathMatcher() {
        this(null, "/", true, true, true,
                Parameter.builder().domain(Parameter.Domain.PATH).enableSort());
    }

    public PathMatcher(String pathSpec,
                       String pathSeparator,
                       boolean trimTokens,
                       boolean caseSensitive,
                       boolean fullMatch,
                       ParameterBuilder parameterBuilder) {
        super.init(pathSpec, pathSeparator, trimTokens, caseSensitive);
        this.analyzedSegments = analyzeTokens();
        this.fullMatch = fullMatch;
        this.parameterBuilder = parameterBuilder;
    }

    public static PathMatcher of(String pathSpec) {
        return new PathMatcher(pathSpec, "/", true, true, true,
                Parameter.builder().domain(Parameter.Domain.PATH).enableSort());
    }

    public void setPathSeparator(String pathSeparator) {
        this.pathSeparator = pathSeparator;
    }

    public Parameter getParameter() {
        return parameterBuilder.build();
    }

    public List<PathSegment> getAnalyzedSegments() {
        return analyzedSegments;
    }

    public boolean match(String pathSpec, String path) {
        super.init(pathSpec, pathSeparator, trimTokens, caseSensitive);
        this.analyzedSegments = analyzeTokens();
        return match(path);
    }

    public boolean match(String path) {
        String normalizedPath = PathNormalizer.normalize(path);
        if (normalizedPath.startsWith(pathSeparator) != pathSpec.startsWith(pathSeparator)) {
            return false;
        }
        List<PathSegment> analyzedSegments = getAnalyzedSegments();
        List<PathSegment> pathSegments = tokenize(normalizedPath);
        int patternStart = 0;
        int patternEnd = analyzedSegments.size() - 1;
        int pathStart = 0;
        int pathEnd = pathSegments.size() - 1;
        while (patternStart <= patternEnd && pathStart <= pathEnd) {
            PathSegment pathSegment = analyzedSegments.get(patternStart);
            if (pathSegment.isCatchAll()) {
                break;
            }
            boolean b = matchAndExtractVariables(pathSegment, pathSegments.get(pathStart));
            if (!b) {
                return false;
            }
            patternStart++;
            pathStart++;
        }
        if (pathStart > pathEnd) {
            if (patternStart > patternEnd) {
                return pathSpec.endsWith(pathSeparator) == normalizedPath.endsWith(pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (patternStart == patternEnd
                    && analyzedSegments.get(patternStart).getString().equals(WILDCARD)
                    && normalizedPath.endsWith(pathSeparator)) {
                return true;
            }
            for (int i = patternStart; i <= patternEnd; i++) {
                if (!analyzedSegments.get(i).isCatchAll()) {
                    return false;
                }
            }
            return true;
        } else if (patternStart > patternEnd) {
            return false;
        } else if (!fullMatch && analyzedSegments.get(patternStart).isCatchAll()) {
            return true;
        }
        while (patternStart <= patternEnd && pathStart <= pathEnd) {
            PathSegment pathSegment = analyzedSegments.get(patternEnd);
            if (pathSegment.isCatchAll()) {
                break;
            }
            boolean b = matchAndExtractVariables(pathSegment, pathSegments.get(pathEnd));
            if (!b) {
                return false;
            }
            patternEnd--;
            pathEnd--;
        }
        if (pathStart > pathEnd) {
            for (int i = patternStart; i <= patternEnd; i++) {
                if (!analyzedSegments.get(i).isCatchAll()) {
                    return false;
                }
            }
            return true;
        }
        while (patternStart != patternEnd && pathStart <= pathEnd) {
            int patternIndexTemp = -1;
            for (int i = patternStart + 1; i <= patternEnd; i++) {
                if (analyzedSegments.get(i).isCatchAll()) {
                    patternIndexTemp = i;
                    break;
                }
            }
            if (patternIndexTemp == patternStart + 1) {
                patternStart++;
                continue;
            }
            int patternLength = patternIndexTemp - patternStart - 1;
            int strLength = pathEnd - pathStart + 1;
            int foundIndex = -1;
            boolean loop = true;
            while (loop) {
                for (int i = 0; i <= strLength - patternLength; i++) {
                    for (int j = 0; j < patternLength; j++) {
                        PathSegment segment = pathSegments.get(pathStart + i + j);
                        PathSegment pathSegment = analyzedSegments.get(patternStart + j + 1);
                        boolean b = matchAndExtractVariables(pathSegment, segment);
                        if (b) {
                            loop = false;
                            break;
                        }
                    }
                    if (loop) {
                        foundIndex = pathStart + i;
                    } else {
                        break;
                    }
                }
            }
            if (foundIndex == -1) {
                return false;
            }
            patternStart = patternIndexTemp;
            pathStart = foundIndex + patternLength;
        }
        for (int i = patternStart; i <= patternEnd; i++) {
            if (!analyzedSegments.get(i).isCatchAll()) {
                return false;
            }
        }
        return true;
    }

    private boolean matchAndExtractVariables(PathSegment patternSegment, PathSegment pathSegment) {
        if (patternSegment.getPattern() == null) {
            return true;
        }
        Matcher matcher = patternSegment.getPattern().matcher(pathSegment.getString());
        if (!matcher.matches()) {
            return false;
        } else {
            if (patternSegment.getParameterNames() == null) {
                return true;
            }
            if (patternSegment.getParameterNames().size() != matcher.groupCount()) {
                throw new IllegalArgumentException("The number of capturing groups in the pattern segment " +
                        patternSegment.getString() + " does not match the number of URI template variables it defines, " +
                        "which can occur if capturing groups are used in a URI template regex. " +
                        "Use non-capturing groups instead.");
            }
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = patternSegment.getParameterNames().get(i - 1);
                String value = matcher.group(i);
                parameterBuilder.add(name, value);
            }
            return true;
        }
    }

    private List<PathSegment> analyzeTokens() {
        List<PathSegment> pathSegments = new ArrayList<>();
        for (PathSegment pathSegment : segments) {
            String token = pathSegment.getString();
            List<String> parameterNames = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            Matcher matcher = GLOB_PATTERN.matcher(token);
            int start = 0;
            boolean isPattern = false;
            boolean isCatchAll = false;
            while (matcher.find()) {
                sb.append(quote(token, start, matcher.start()));
                String match = matcher.group();
                if ("?".equals(match)) {
                    sb.append('.');
                    isPattern = true;
                } else if ("*".equals(match)) {
                    sb.append(".*");
                    isPattern = true;
                } else if (CATCH_ALL.equals(match)) {
                    isCatchAll = true;
                } else if (match.startsWith("{") && match.endsWith("}")) {
                    int colonIdx = match.indexOf(':');
                    if (colonIdx == -1) {
                        sb.append(DEFAULT_PATTERN);
                        parameterNames.add(matcher.group(1));
                    } else {
                        String parameterPattern = match.substring(colonIdx + 1, match.length() - 1);
                        sb.append('(').append(parameterPattern).append(')');
                        parameterNames.add(match.substring(1, colonIdx));
                    }
                } else {
                    if (RESERVED_URL_CHARS.matchesAnyOf(match)) {
                        throw new IllegalArgumentException("found reserved chars in " + match);
                    }
                }
                start = matcher.end();
            }
            sb.append(quote(token, start, token.length()));
            if (isPattern) {
                pathSegment.setPattern(caseSensitive ? Pattern.compile(sb.toString()) :
                        Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE));
            }
            if (!parameterNames.isEmpty()) {
                pathSegment.setParameterNames(parameterNames);
                pathSegment.setPattern(MATCH_ALL_PATTERN);
            }
            if (isCatchAll) {
                pathSegment.setCatchAll(isCatchAll);
            }
            pathSegments.add(pathSegment);
        }
        return pathSegments;
    }

    private static String quote(String s, int start, int end) {
        return start == end ? "" : Pattern.quote(s.substring(start, end));
    }
}
