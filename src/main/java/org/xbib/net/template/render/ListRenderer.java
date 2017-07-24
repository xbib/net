package org.xbib.net.template.render;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class ListRenderer extends MultiValueRenderer {

    public ListRenderer(ExpressionType type) {
        super(type);
    }

    @Override
    protected List<String> renderNamedExploded(String varname, VariableValue value) {
        return value.getListValue().stream().map(element ->
                element.isEmpty() ? varname + ifEmpty : varname + '=' + pctEncode(element)
        ).collect(Collectors.toList());
    }

    @Override
    protected List<String> renderUnnamedExploded(VariableValue value) {
        return value.getListValue().stream().map(this::pctEncode).collect(Collectors.toList());
    }

    @Override
    protected List<String> renderNamedNormal(String varname, VariableValue value) {
        StringBuilder sb = new StringBuilder(varname);
        if (value.isEmpty()) {
            return Collections.singletonList(sb.append(ifEmpty).toString());
        }
        sb.append('=');
        List<String> elements = value.getListValue().stream().map(this::pctEncode).collect(Collectors.toList());
        return Collections.singletonList(sb.toString() + String.join(",", elements));
    }

    @Override
    protected List<String> renderUnnamedNormal(VariableValue value) {
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> elements = value.getListValue().stream().map(this::pctEncode).collect(Collectors.toList());
        return Collections.singletonList(String.join(",", elements));
    }
}
