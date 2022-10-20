package org.xbib.net.template.render;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.List;

/**
 * Null renderer.
 */
public class NullRenderer extends ValueRenderer {

    public NullRenderer(ExpressionType type) {
        super(type);
    }

    @Override
    public List<String> render(VariableSpec varspec, VariableValue value) {
        return null;
    }
}
