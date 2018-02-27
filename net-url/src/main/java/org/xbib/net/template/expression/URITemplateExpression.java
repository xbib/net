package org.xbib.net.template.expression;

import org.xbib.net.template.vars.Variables;

/**
 * Template expression interface.
 */
public interface URITemplateExpression {

    String expand(Variables vars);
}
