package org.xbib.net.template.vars.specs;

/**
 * A variable specifier.
 *
 * A template expression can have one or more variable specifiers. For
 * instance, in {@code {+path:3,var}}, variable specifiers are {@code path:3}
 * and {@code var}.
 *
 * This class records the name of this specifier and its modifier, if any.
 */
public abstract class VariableSpec {

    protected final String name;

    private final VariableSpecType type;

    protected VariableSpec(VariableSpecType type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Get the modifier type for this var spec.
     *
     * @return the modifier type
     */
    public final VariableSpecType getType() {
        return type;
    }

    /**
     * Get the name for this var spec.
     *
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Tell whether this varspec has an explode modifier.
     *
     * @return true if an explode modifier is present
     */
    public abstract boolean isExploded();

    /**
     * Return the prefix length for this varspec.
     *
     * Returns -1 if no prefix length is specified. Recall: valid values are
     * integers between 0 and 10000.
     *
     * @return the prefix length, or -1 if no prefix modidifer
     */
    public abstract int getPrefixLength();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract String toString();
}
