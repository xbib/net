package org.xbib.net.template.expression;

/**
 */
public enum ExpressionType {
    /*
     * Simple character expansion.
     */
    SIMPLE("", ',', false, ""),
    /*
     * Reserved character expansion.
     */
    RESERVED("", ',', false, ""),
    /*
     * Name labels expansion.
     */
    NAME_LABELS(".", '.', false, ""),
    /*
     * Path segments expansion.
     */
    PATH_SEGMENTS("/", '/', false, ""),
    /*
     * Path parameters expansion.
     */
    PATH_PARAMETERS(";", ';', true, ""),
    /*
     * Query string expansion.
     */
    QUERY_STRING("?", '&', true, "="),
    /*
     * Query string continuation expansion.
     */
    QUERY_CONT("&", '&', true, "="),
    /*
     * Fragment expansion.
     */
    FRAGMENT("#", ',', false, "");

    /**
     * Prefix string of expansion (requires at least one expanded token).
     */
    private final String prefix;

    /**
     * Separator if several tokens are present.
     */
    private final char separator;

    /**
     * Whether the variable (string, list) or key (map) name should be included
     * if no explode modifier is found.
     */
    private final boolean named;

    /**
     * String to append to a name if the matching value is empty (empty string,
     * empty list element, empty map value).
     */
    private final String ifEmpty;

    ExpressionType(String prefix, char separator, boolean named, String ifEmpty) {
        this.prefix = prefix;
        this.separator = separator;
        this.named = named;
        this.ifEmpty = ifEmpty;
    }

    /**
     * Get the prefix string for this expansion type.
     *
     * @return the prefix string
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get the separator between token expansion elements.
     *
     * @return the separator
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Tell whether the variable name should be used in expansion.
     *
     * @return true if this is the case
     */
    public boolean isNamed() {
        return named;
    }

    /**
     * Get the substitution string for empty values.
     *
     * @return the substitution string
     */
    public String getIfEmpty() {
        return ifEmpty;
    }
}
