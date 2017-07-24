package org.xbib.net.template.parse;

import org.xbib.net.matcher.CharMatcher;
import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.expression.TemplateExpression;
import org.xbib.net.template.expression.URITemplateExpression;
import org.xbib.net.template.vars.specs.VariableSpec;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class ExpressionParser implements TemplateParser {

    private static final Map<Character, ExpressionType> EXPRESSION_TYPE_MAP;
    static {
        EXPRESSION_TYPE_MAP = new HashMap<>();
        EXPRESSION_TYPE_MAP.put('+', ExpressionType.RESERVED);
        EXPRESSION_TYPE_MAP.put('#', ExpressionType.FRAGMENT);
        EXPRESSION_TYPE_MAP.put('.', ExpressionType.NAME_LABELS);
        EXPRESSION_TYPE_MAP.put('/', ExpressionType.PATH_SEGMENTS);
        EXPRESSION_TYPE_MAP.put(';', ExpressionType.PATH_PARAMETERS);
        EXPRESSION_TYPE_MAP.put('?', ExpressionType.QUERY_STRING);
        EXPRESSION_TYPE_MAP.put('&', ExpressionType.QUERY_CONT);
    }
    private static final CharMatcher COMMA = CharMatcher.is(',');
    private static final CharMatcher END_EXPRESSION = CharMatcher.is('}');


    @Override
    public URITemplateExpression parse(CharBuffer buffer) {
        buffer.get();
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("early end of expression");
        }
        ExpressionType type = ExpressionType.SIMPLE;
        char c = buffer.charAt(0);
        if (EXPRESSION_TYPE_MAP.containsKey(c)) {
            char s = buffer.get();
            type = EXPRESSION_TYPE_MAP.get(s);
        }
        List<VariableSpec> varspecs = new ArrayList<>();
        while (true) {
            varspecs.add(VariableSpecParser.parse(buffer));
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("early end of expression");
            }
            c = buffer.get();
            if (COMMA.matches(c)) {
                continue;
            }
            if (END_EXPRESSION.matches(c)) {
                break;
            }
            throw new IllegalArgumentException("unexpected token");
        }
        return new TemplateExpression(type, varspecs);
    }
}
