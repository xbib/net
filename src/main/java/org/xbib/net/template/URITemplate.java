package org.xbib.net.template;

import org.xbib.net.URL;
import org.xbib.net.template.expression.URITemplateExpression;
import org.xbib.net.template.parse.URITemplateParser;
import org.xbib.net.template.vars.Variables;

import java.util.List;

/**
 *
 */
public class URITemplate {

    private final List<URITemplateExpression> expressions;

    public URITemplate(String input) {
        this.expressions = URITemplateParser.parse(input);
    }

    public List<URITemplateExpression> expressions() {
        return expressions;
    }

    /**
     * Expand this template to a string given a list of variables.
     *
     * @param vars the variable map (names as keys, contents as values)
     * @return expanded string
     */
    public String toString(Variables vars) {
        StringBuilder sb = new StringBuilder();
        for (URITemplateExpression expression : expressions) {
            sb.append(expression.expand(vars));
        }
        return sb.toString();
    }

    /**
     * Expand this template to a URL given a set of variables.
     *
     * @param vars the variables
     * @return a URL
     */
    public URL toURL(Variables vars) {
        return URL.from(toString(vars));
    }
}
