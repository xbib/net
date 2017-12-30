package org.xbib.net.template.vars.values;

import java.util.List;
import java.util.Map;

/**
 */
public abstract class VariableValue {

    private final ValueType type;

    VariableValue(ValueType type) {
        this.type = type;
    }

    /**
     * Get the type for this value.
     *
     * @return the value type
     */
    public ValueType getType() {
        return type;
    }

    /**
     * Get a simple string for this value.
     * Only valid for string values.
     *
     * @return the string
     * @throws IllegalArgumentException value is not a string value
     */
    public String getScalarValue() {
        throw new IllegalArgumentException("not a scalar");
    }

    /**
     * Get a list for this value.
     * Only valid for list values.
     *
     * @return the list
     * @throws IllegalArgumentException value is not a list value
     */
    public List<String> getListValue() {
        throw new IllegalArgumentException("not a list");
    }

    /**
     * Get a map for this value.
     * Only valid for map values.
     *
     * @return the map
     * @throws IllegalArgumentException value is not a map value
     */
    public Map<String, String> getMapValue() {
        throw new IllegalArgumentException("not a map");
    }

    /**
     * Tell whether this value is empty.
     * For strings, this tells whether the string itself is empty. For lists
     * and maps, this tells whether the list or map have no elements/entries.
     *
     * @return true if the value is empty
     */
    public abstract boolean isEmpty();
}
