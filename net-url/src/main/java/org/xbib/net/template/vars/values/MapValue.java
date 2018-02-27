package org.xbib.net.template.vars.values;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map value.
 */
public class MapValue extends VariableValue {

    private final Map<String, String> map;

    private MapValue(Builder builder) {
        super(ValueType.MAP);
        map = builder.map;
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
     * Convenience method to build a variable value from an existing {@link Map}.
     *
     * @param map the map
     * @param <T> the type of values in this map
     * @return a new map value as a {@link VariableValue}
     * @throws NullPointerException map is null, or one of its keys or values
     *                              is null
     */
    public static <T> VariableValue copyOf(Map<String, T> map) {
        return builder().putAll(map).build();
    }

    @Override
    public Map<String, String> getMapValue() {
        return map;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Builder class for a {@link MapValue}.
     */
    public static class Builder {

        private final Map<String, String> map = new LinkedHashMap<>();

        Builder() {
        }

        /**
         * Add one key/value pair to the map.
         *
         * @param key   the key
         * @param value the value
         * @param <T>   the type of the value
         * @return this
         * @throws NullPointerException the key or value is null
         */
        public <T> Builder put(String key, T value) {
            map.put(key, value.toString());
            return this;
        }

        /**
         * Inject a map of key/value pairs.
         *
         * @param map the map
         * @param <T> the type of this map's values
         * @return this
         * @throws NullPointerException map is null, or one of its keys or
         *                              values is null
         */
        public <T> Builder putAll(Map<String, T> map) {
            for (Map.Entry<String, T> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * Build the value.
         *
         * @return the map value as a {@link VariableValue}
         */
        public VariableValue build() {
            return new MapValue(this);
        }
    }
}
