package org.xbib.net.template.parse;

import org.xbib.net.util.CharMatcher;
import org.xbib.net.template.expression.TemplateLiteral;
import org.xbib.net.template.expression.URITemplateExpression;

import java.nio.CharBuffer;

public class LiteralParser implements TemplateParser {

    public LiteralParser() {
    }

    @Override
    public URITemplateExpression parse(CharBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        char c;
        while (buffer.hasRemaining()) {
            c = buffer.charAt(0);
            if (!CharMatcher.LITERALS.matches(c)) {
                break;
            }
            sb.append(buffer.get());
            if (CharMatcher.PERCENT.matches(c)) {
                parsePercentEncoded(buffer, sb);
            }
        }
        return new TemplateLiteral(sb.toString());
    }

    private static void parsePercentEncoded(CharBuffer buffer, StringBuilder sb) {
        if (buffer.remaining() < 2) {
            throw new IllegalArgumentException("short read");
        }
        char first = buffer.get();
        if (!CharMatcher.HEXDIGIT.matches(first)) {
            throw new IllegalArgumentException("illegal percent encoding");
        }
        char second = buffer.get();
        if (!CharMatcher.HEXDIGIT.matches(second)) {
            throw new IllegalArgumentException("illegal percent encoding");
        }
        sb.append(first).append(second);
    }
}
