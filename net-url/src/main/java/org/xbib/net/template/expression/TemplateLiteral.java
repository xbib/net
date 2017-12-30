package org.xbib.net.template.expression;

import org.xbib.net.template.vars.Variables;

/**
 */
public
class TemplateLiteral implements URITemplateExpression {

    private final String literal;

    public TemplateLiteral(String literal) {
        this.literal = literal;
    }

    @Override
    public String expand(Variables vars) {
        return literal;
    }
}
