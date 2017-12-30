package org.xbib.net.template.render;

import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.values.VariableValue;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class StringRenderer extends ValueRenderer {

    public StringRenderer(ExpressionType type) {
        super(type);
    }

    @Override
    public List<String> render(VariableSpec varspec, VariableValue value) {
        return Collections.singletonList(doRender(varspec, value.getScalarValue()));
    }

    private String doRender(VariableSpec varspec, String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        if (named) {
            sb.append(varspec.getName());
            if (value.isEmpty()) {
                return sb.append(ifEmpty).toString();
            }
            sb.append('=');
        }
        int prefixLen = varspec.getPrefixLength();
        if (prefixLen == -1) {
            return sb.append(pctEncode(value)).toString();
        }
        int len = value.codePointCount(0, value.length());
        return len <= prefixLen ?
                sb.append(pctEncode(value)).toString() :
                sb.append(pctEncode(nFirstChars(value, prefixLen))).toString();
    }

    private static String nFirstChars(String s, int n) {
        int realIndex = n;
        while (s.codePointCount(0, realIndex) != n) {
            realIndex++;
        }
        return s.substring(0, realIndex);
    }
}
