package org.xbib.net.template.vars.specs;

/**
 * Exploded variable.
 */
public class ExplodedVariable extends VariableSpec {

    public ExplodedVariable(String name) {
        super(VariableSpecType.EXPLODED, name);
    }

    @Override
    public boolean isExploded() {
        return true;
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
        ExplodedVariable other = (ExplodedVariable) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name + " (exploded)";
    }
}
