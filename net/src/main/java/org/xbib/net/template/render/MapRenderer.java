package org.xbib.net.template.render;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Map renderer.
 */
public  class MapRenderer extends MultiValueRenderer {

    public MapRenderer(ExpressionType type) {
        super(type);
    }

    @Override
    protected List<String> renderNamedExploded(String varname, VariableValue value) {
        List<String> ret = new ArrayList<>();
        value.getMapValue().forEach((k, v) -> ret.add(pctEncode(k) + (v.isEmpty() ? ifEmpty : '=' + pctEncode(v))));
        return ret;
    }

    @Override
    protected List<String> renderUnnamedExploded(VariableValue value) {
        List<String> ret = new ArrayList<>();
        value.getMapValue().forEach((k, v) -> ret.add(pctEncode(k) + '=' + pctEncode(v)));
        return ret;
    }

    @Override
    protected List<String> renderNamedNormal(String varname, VariableValue value) {
        StringBuilder sb = new StringBuilder(varname);
        if (value.isEmpty()) {
            return Collections.singletonList(sb.append(ifEmpty).toString());
        }
        sb.append('=');
        List<String> elements = mapAsList(value).stream().map(this::pctEncode).collect(Collectors.toList());
        return Collections.singletonList(sb.toString() + String.join(",", elements));
    }

    @Override
    protected List<String> renderUnnamedNormal(VariableValue value) {
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> elements = mapAsList(value).stream().map(this::pctEncode).collect(Collectors.toList());
        return Collections.singletonList(String.join(",", elements));
    }

    private static List<String> mapAsList(VariableValue value) {
        List<String> ret = new ArrayList<>();
        value.getMapValue().forEach((k, v) -> {
            ret.add(k);
            ret.add(v);
        });
        return ret;
    }
}
