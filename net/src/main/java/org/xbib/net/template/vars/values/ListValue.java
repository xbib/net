package org.xbib.net.template.vars.values;

import java.util.ArrayList;
import java.util.List;

/**
 * List value.
 */
public class ListValue extends VariableValue {

    private final List<String> list;

    private ListValue(Builder builder) {
        super(ValueType.ARRAY);
        list = builder.list;
    }

    /**
     * Create a new list value builder.
     *
     * @return a builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a list value out of an existing iterable (list, set, other).
     *
     * This calls {@link Builder#addAll(Iterable)} internally.
     *
     * @param iterable the iterable
     * @param <T>      the type of iterable elements
     * @return a new list value
     */
    public static <T> VariableValue copyOf(Iterable<T> iterable) {
        return new Builder().addAll(iterable).build();
    }

    @Override
    public List<String> getListValue() {
        return list;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public String toString() {
        return list.toString();
    }

    /**
     * Builder class for a {@link ListValue}.
     */
    public static class Builder {

        private final List<String> list = new ArrayList<>();

        Builder() {
        }

        /**
         * Add a series of elements to this list.
         *
         * @param first first element
         * @param other other elements, if any
         * @return this
         * @throws NullPointerException one argument at least is null
         */
        public Builder add(Object first, Object... other) {
            list.add(first.toString());
            for (Object o : other) {
                list.add(o.toString());
            }
            return this;
        }

        /**
         * Add elements from an iterable.
         *
         * @param iterable the iterable
         * @param <T>  type of elements in the iterable
         * @return this
         */
        public <T> Builder addAll(Iterable<T> iterable) {
            for (T t : iterable) {
                list.add(t.toString());
            }
            return this;
        }

        /**
         * Build the value.
         *
         * @return the list value as a {@link VariableValue}
         */
        public VariableValue build() {
            return new ListValue(this);
        }
    }
}
