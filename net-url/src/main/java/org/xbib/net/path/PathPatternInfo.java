package org.xbib.net.path;

import java.util.regex.Pattern;

/**
 */
class PathPatternInfo {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");

    private final String pattern;

    private int uriVars;

    private int singleWildcards;

    private int doubleWildcards;

    private boolean catchAllPattern;

    private boolean prefixPattern;

    private Integer length;

    PathPatternInfo(String pattern) {
        this.pattern = pattern;
        if (this.pattern != null) {
            initCounters();
            this.catchAllPattern = this.pattern.equals("/**");
            this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
        }
        if (this.uriVars == 0) {
            this.length = this.pattern != null ? this.pattern.length() : 0;
        }
    }

    private void initCounters() {
        int pos = 0;
        while (pos < this.pattern.length()) {
            if (this.pattern.charAt(pos) == '{') {
                this.uriVars++;
                pos++;
            } else if (this.pattern.charAt(pos) == '*') {
                if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
                    this.doubleWildcards++;
                    pos += 2;
                } else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
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

    int getUriVars() {
        return uriVars;
    }

    int getSingleWildcards() {
        return singleWildcards;
    }

    int getDoubleWildcards() {
        return doubleWildcards;
    }

    boolean isLeastSpecific() {
        return this.pattern == null || this.catchAllPattern;
    }

    boolean isPrefixPattern() {
        return this.prefixPattern;
    }

    int getTotalCount() {
        return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
    }

    int getLength() {
        if (this.length == null) {
            this.length = VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length();
        }
        return length;
    }
}
