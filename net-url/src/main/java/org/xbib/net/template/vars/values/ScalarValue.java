package org.xbib.net.template.vars.values;

/**
 * Scalar value.
 */
public class ScalarValue extends VariableValue {

    private final String value;

    public ScalarValue(Object value) {
        super(ValueType.SCALAR);
        this.value = (String) value;
    }

    @Override
    public String getScalarValue() {
        return value;
    }

    @Override
    public boolean isEmpty() {
        return value.isEmpty();
    }

    @Override
    public String toString() {
        return value;
    }
}
