package org.xbib.net.path.structure;

import org.xbib.net.PathNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Path {

    protected static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{[^/]+?}");

    protected static final String CATCH_ALL = "**";

    protected String pathSpec;

    protected String pathSeparator;

    protected boolean trimTokens;

    protected boolean caseSensitive;

    protected List<PathSegment> segments;

    private int parameterCount;

    private int singleWildcards;

    private int doubleWildcards;

    private boolean catchAllPattern;

    private boolean prefixPattern;

    private Integer length;

    public Path() {
    }

    public Path(String pathSpec) {
        this(pathSpec, "/", false, false);
    }

    public Path(String pathSpec, String pathSeparator, boolean trimTokens, boolean caseSensitive) {
        init(pathSpec, pathSeparator, trimTokens, caseSensitive);
    }

    public static Path of(String pathSpec) {
        return new Path(pathSpec);
    }

    public void init(String pathSpec, String pathSeparator, boolean trimTokens, boolean caseSensitive) {
        this.pathSpec = PathNormalizer.normalize(pathSpec);
        this.pathSeparator = pathSeparator;
        this.trimTokens = trimTokens;
        this.caseSensitive = caseSensitive;
        this.segments = tokenize(this.pathSpec);
        if (pathSpec != null) {
            initCounters();
            this.catchAllPattern = pathSpec.equals(CATCH_ALL);
            this.prefixPattern = !catchAllPattern && pathSpec.endsWith(CATCH_ALL);
        }
        if (this.parameterCount == 0) {
            this.length = pathSpec != null ? pathSpec.length() : 0;
        }
    }

    public String getPathSpec() {
        return pathSpec;
    }

    public String getPathSeparator() {
        return pathSeparator;
    }

    public boolean isTrimTokens() {
        return trimTokens;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public int getSingleWildcards() {
        return singleWildcards;
    }

    public int getDoubleWildcards() {
        return doubleWildcards;
    }

    public boolean isLeastSpecific() {
        return pathSpec == null || catchAllPattern;
    }

    public boolean isPrefixPattern() {
        return prefixPattern;
    }

    public int getTotalCount() {
        return parameterCount + singleWildcards + (2 * doubleWildcards);
    }

    public boolean isCatchAllPattern() {
        return catchAllPattern;
    }

    public boolean isWildCard() {
        return singleWildcards > 0 || doubleWildcards > 0;
    }

    public int getLength() {
        if (length == null) {
            length = PARAMETER_PATTERN.matcher(pathSpec).replaceAll("#").length();
        }
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Path that = (Path) o;
        return parameterCount == that.parameterCount && singleWildcards == that.singleWildcards && doubleWildcards == that.doubleWildcards && catchAllPattern == that.catchAllPattern && prefixPattern == that.prefixPattern && Objects.equals(pathSpec, that.pathSpec) && Objects.equals(length, that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathSpec, parameterCount, singleWildcards, doubleWildcards, catchAllPattern, prefixPattern, length);
    }

    public List<PathSegment> tokenize(String string) {
        return tokenize(string, pathSeparator, trimTokens, caseSensitive);
    }

    public static List<PathSegment> tokenize(String string, String pathSeparator, boolean trimTokens, boolean caseSensitive) {
        List<PathSegment> pathSegments = new ArrayList<>();
        if (string == null) {
            return pathSegments;
        }
        StringTokenizer st = new StringTokenizer(string, pathSeparator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (token.length() > 0) {
                if (!caseSensitive) {
                    token = token.toLowerCase(Locale.ROOT);
                }
                PathSegment pathSegment = new PathSegment();
                pathSegment.setString(token);
                pathSegments.add(pathSegment);
            }
        }
        return pathSegments;
    }

    private void initCounters() {
        singleWildcards = 0;
        doubleWildcards = 0;
        int pos = 0;
        while (pos < pathSpec.length()) {
            char ch = pathSpec.charAt(pos);
            if (ch == '{') {
                parameterCount++;
                pos++;
            } else if (ch == '*') {
                if (pos + 1 < pathSpec.length() && pathSpec.charAt(pos + 1) == '*') {
                    doubleWildcards++;
                    pos += 2;
                } else if (pos > 0 && !(pathSpec.charAt(pos - 1) == '.' && pathSpec.charAt(pos) == '*')) {
                    singleWildcards++;
                    pos++;
                } else {
                    pos++;
                }
            } else {
                pos++;
            }
        }
    }
}
