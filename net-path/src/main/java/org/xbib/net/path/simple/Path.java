package org.xbib.net.path.simple;

import java.util.Objects;
import java.util.regex.Pattern;

public class Path {

    static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?}");

    static final String CATCH_ALL = "**";

    private final String pattern;

    private int uriVars;

    private int singleWildcards;

    private int doubleWildcards;

    private boolean catchAllPattern;

    private boolean prefixPattern;

    private Integer length;

    public Path(String pattern) {
        this.pattern = pattern;
        if (this.pattern != null) {
            initCounters();
            this.catchAllPattern = this.pattern.equals(CATCH_ALL);
            this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith(CATCH_ALL);
        }
        if (this.uriVars == 0) {
            this.length = this.pattern != null ? this.pattern.length() : 0;
        }
    }

    public static Path of(String string) {
        return new Path(string);
    }

    public String getPattern() {
        return pattern;
    }

    public int getUriVars() {
        return uriVars;
    }

    public int getSingleWildcards() {
        return singleWildcards;
    }

    public int getDoubleWildcards() {
        return doubleWildcards;
    }

    public boolean isLeastSpecific() {
        return this.pattern == null || this.catchAllPattern;
    }

    public boolean isPrefixPattern() {
        return this.prefixPattern;
    }

    public int getTotalCount() {
        return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
    }

    public int getLength() {
        if (length == null) {
            length = VARIABLE_PATTERN.matcher(pattern).replaceAll("#").length();
        }
        return length;
    }

    public boolean isCatchAllPattern() {
        return catchAllPattern;
    }

    public boolean isWildCard() {
        return singleWildcards > 0 || doubleWildcards > 0;
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
        return uriVars == that.uriVars && singleWildcards == that.singleWildcards && doubleWildcards == that.doubleWildcards && catchAllPattern == that.catchAllPattern && prefixPattern == that.prefixPattern && Objects.equals(pattern, that.pattern) && Objects.equals(length, that.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, uriVars, singleWildcards, doubleWildcards, catchAllPattern, prefixPattern, length);
    }

    private void initCounters() {
        int pos = 0;
        int len = this.pattern.length();
        while (pos < len) {
            if (this.pattern.charAt(pos) == '{') {
                this.uriVars++;
                pos++;
            } else if (this.pattern.charAt(pos) == '*') {
                if (pos + 1 < len && this.pattern.charAt(pos + 1) == '*') {
                    this.doubleWildcards++;
                    pos += 2;
                } else if (pos > 0 && !(this.pattern.charAt(pos - 1) == '.' && this.pattern.charAt(pos) == '*')) {
                    this.singleWildcards++;
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
