package org.xbib.net.template.vars.specs;

/**
 * A varspec without modifier (for instance, {@code foo} in {@code {foo}}.
 */
public class SimpleVariable extends VariableSpec {

    public SimpleVariable(String name) {
        super(VariableSpecType.SIMPLE, name);
    }

    @Override
    public boolean isExploded() {
        return false;
    }

    @Override
    public int getPrefixLength() {
        return -1;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleVariable other = (SimpleVariable) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name + " (simple)";
    }
}
