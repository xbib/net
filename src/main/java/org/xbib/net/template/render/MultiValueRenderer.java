package org.xbib.net.template.render;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.List;

/**
 *
 */
abstract class MultiValueRenderer extends ValueRenderer {

    MultiValueRenderer(ExpressionType type) {
        super(type);
    }

    @Override
    public List<String> render(VariableSpec varspec, VariableValue value) {
        if (varspec.getPrefixLength() != -1) {
            throw new IllegalArgumentException("incompatible var spec value");
        }
        String varname = varspec.getName();
        return named ?
                (varspec.isExploded() ? renderNamedExploded(varname, value) : renderNamedNormal(varname, value)) :
                (varspec.isExploded() ? renderUnnamedExploded(value) : renderUnnamedNormal(value));
    }

    protected abstract List<String> renderNamedExploded(String varname, VariableValue value);

    protected abstract List<String> renderUnnamedExploded(VariableValue value);

    /**
     * Rendering method for named expressions and non exploded varspecs.
     *
     * @param varname name of the variable (used in lists)
     * @param value   value of the variable
     * @return list of rendered elements
     */
    protected abstract List<String> renderNamedNormal(String varname, VariableValue value);

    /**
     * Rendering method for non named expressions and non exploded varspecs.
     *
     * @param value value of the variable
     * @return list of rendered elements
     */
    protected abstract List<String> renderUnnamedNormal(VariableValue value);
}
