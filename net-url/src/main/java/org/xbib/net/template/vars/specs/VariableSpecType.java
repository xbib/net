package org.xbib.net.template.vars.specs;

/**
 * Enumeration of a variable modifier type.
 */
public enum VariableSpecType {
    /**
     * No modifier.
     */
    SIMPLE,
    /**
     * Prefix modifier ({@code :xxx} where {@code xxx} is an integer).
     * Only makes sense for string values.
     */
    PREFIX,
    /**
     * Explode modifier ({@code *}).
     * Only makes sense for list and map values.
     */
    EXPLODED
}
