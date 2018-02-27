package org.xbib.net.template.parse;

import org.xbib.net.matcher.CharMatcher;
import org.xbib.net.template.expression.URITemplateExpression;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * URI template parser.
 */
public class URITemplateParser {

    private static final CharMatcher BEGIN_EXPRESSION = CharMatcher.is('{');

    private URITemplateParser() {
    }

    public static List<URITemplateExpression> parse(String input) {
        return parse(CharBuffer.wrap(input).asReadOnlyBuffer());
    }

    public static List<URITemplateExpression> parse(CharBuffer buffer) {
        List<URITemplateExpression> ret = new ArrayList<>();
        TemplateParser templateParser;
        URITemplateExpression expression;
        while (buffer.hasRemaining()) {
            templateParser = selectParser(buffer);
            expression = templateParser.parse(buffer);
            ret.add(expression);
        }
        return ret;
    }

    private static TemplateParser selectParser(CharBuffer buffer) {
        char c = buffer.charAt(0);
        TemplateParser parser;
        if (CharMatcher.LITERALS.matches(c)) {
            parser = new LiteralParser();
        } else if (BEGIN_EXPRESSION.matches(c)) {
            parser = new ExpressionParser();
        } else {
            throw new IllegalArgumentException("no parser");
        }
        return parser;
    }
}
