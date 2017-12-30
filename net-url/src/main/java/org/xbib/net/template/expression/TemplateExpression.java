package org.xbib.net.template.expression;

import org.xbib.net.template.render.ValueRenderer;
import org.xbib.net.template.vars.Variables;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TemplateExpression implements URITemplateExpression {

    private final ExpressionType expressionType;

    private final List<VariableSpec> variableSpecs;

    public TemplateExpression(ExpressionType expressionType, List<VariableSpec> variableSpecs) {
        this.expressionType = expressionType;
        this.variableSpecs = variableSpecs;
        if (expressionType == null) {
            throw new IllegalArgumentException("expression type must not be null");
        }
        if (variableSpecs == null) {
            throw new IllegalArgumentException("variables must not be null");
        }
    }

    @Override
    public String expand(Variables vars)  {
        List<String> expansions = new ArrayList<>();
        VariableValue value;
        ValueRenderer renderer;
        for (VariableSpec varspec : variableSpecs) {
            value = vars.get(varspec.getName());
            if (value == null) {
                continue;
            }
            renderer = value.getType().selectRenderer(expressionType);
            List<String> list = renderer.render(varspec, value);
            if (list != null) {
                expansions.addAll(list);
            }
        }
        if (expansions.isEmpty()) {
            return "";
        }
        return expressionType.getPrefix() + String.join(Character.toString(expressionType.getSeparator()), expansions);
    }

    @Override
    public int hashCode() {
        return 31 * expressionType.hashCode() + variableSpecs.hashCode();
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
        TemplateExpression other = (TemplateExpression) obj;
        return expressionType == other.expressionType && variableSpecs.equals(other.variableSpecs);
    }
}
