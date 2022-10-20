package org.xbib.net.template.vars;

import org.xbib.net.template.vars.values.ListValue;
import org.xbib.net.template.vars.values.MapValue;
import org.xbib.net.template.vars.values.ScalarValue;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Variables.
 */
public class Variables {

    private final Map<String, VariableValue> vars;

    private Variables(Builder builder) {
        this.vars = builder.vars;
    }

    /**
     * Create a new builder for this class.
     *
     * @return a {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the value associated with a variable name.
     *
     * @param varname the variable name
     * @return the value, or {@code null} if there is no matching value
     */
    public VariableValue get(String varname) {
        return vars.get(varname);
    }

    @Override
    public String toString() {
        return vars.toString();
    }

    /**
     * A Builder for variables.
     */
    public static class Builder {

        private final Map<String, VariableValue> vars =  new LinkedHashMap<>();

        Builder() {
        }

        /**
         * Associate a map, list, or object to a variable name.
         *
         * @param varname the variable name
         * @param value the value, as a {@link VariableValue}
         * @return this
         */
        @SuppressWarnings("unchecked")
        public Builder add(String varname, Object value) {
            if (value instanceof VariableValue) {
                addValue(varname, (VariableValue) value);
            } else if (value instanceof Map) {
                addValue(varname, (Map) value);
            } else if (value instanceof List) {
                addValue(varname, (List) value);
            } else {
                addValue(varname, new ScalarValue(value));
            }
            return this;
        }

        /**
         * Associate a value to a variable name.
         *
         * @param varname the variable name
         * @param value the value, as a {@link VariableValue}
         * @return this
         */
        private Builder addValue(String varname, VariableValue value) {
            vars.put(varname, value);
            return this;
        }

        /**
         * Shortcut method to associate a name with a list value.
         * Any {@link Iterable} can be used (thereby including all collections:
         * sets, lists, etc). Note that it is your responsibility that objects in
         * this iterable implement {@link Object#toString()} correctly.
         *
         * @param varname the variable name
         * @param iterable the iterable
         * @return this
         */
        private Builder addValue(String varname, Iterable<Object> iterable) {
            return add(varname, ListValue.copyOf(iterable));
        }

        /**
         * Method to associate a variable name to a map value.
         * Values of the map can be of any type. You should ensure that they
         * implement {@link Object#toString()} correctly.
         *
         * @param varname the variable name
         * @param map the map
         * @return this
         */
        private Builder addValue(String varname, Map<String, ?> map) {
            return add(varname, MapValue.copyOf(map));
        }

        /**
         * Add all variable definitions from another variable map.
         * @param other the other variable map to copy definitions from
         * @return this
         * @throws NullPointerException other variable map is null
         */
        public Builder add(Variables other) {
            vars.putAll(other.vars);
            return this;
        }

        public Variables build() {
            return new Variables(this);
        }
    }
}
