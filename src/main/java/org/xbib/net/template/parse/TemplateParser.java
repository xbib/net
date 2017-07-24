package org.xbib.net.template.parse;

import org.xbib.net.template.expression.URITemplateExpression;

import java.nio.CharBuffer;

/**
 *
 */
public interface TemplateParser {
    URITemplateExpression parse(CharBuffer buffer);
}
