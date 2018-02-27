package org.xbib.net.template.render;

import org.xbib.net.PercentEncoder;
import org.xbib.net.PercentEncoders;
import org.xbib.net.template.expression.ExpressionType;
import org.xbib.net.template.vars.specs.VariableSpec;
import org.xbib.net.template.vars.values.VariableValue;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The algorithm used for rendering is centered around this class, and is
 * adapted from the algorithm suggested in the RFC's appendix.
 *
 * Eventually, rendering can be viewed as joining a list of rendered strings
 * with the expression type separator; if the resulting list is empty, the end
 * result is the empty string; otherwise, it is the expression's prefix string
 * (if any) followed by the joined list of rendered strings.
 *
 * This class renders one variable value according to the expression type and
 * value type. The rendering method returns a list, which can be empty.
 */
public abstract class ValueRenderer {
    /**
     * Whether variable values are named during expansion.
     */
    protected final boolean named;

    /**
     * Substitution string for an empty value/list member/map value.
     */
    protected final String ifEmpty;

    /**
     * The percent encoder.
     */
    private final PercentEncoder percentEncoder;

    protected ValueRenderer(ExpressionType type) {
        named = type.isNamed();
        ifEmpty = type.getIfEmpty();
        switch (type) {
            case RESERVED:
            case FRAGMENT:
                percentEncoder = PercentEncoders.getQueryEncoder(StandardCharsets.UTF_8);
                break;
            default:
                percentEncoder = PercentEncoders.getUnreservedEncoder(StandardCharsets.UTF_8);
                break;
        }
    }

    /**
     * Render a value given a varspec and value.
     *
     * @param varspec the varspec
     * @param value   the matching variable value
     * @return a list of rendered strings
     */
    public abstract List<String> render(VariableSpec varspec, VariableValue value);

    /**
     * Render a string value, doing character percent-encoding where needed.
     *
     * The character set on which to perform percent encoding is dependent
     * on the expression type.
     *
     * @param s the string to encode
     * @return an encoded string
     */
    protected String pctEncode(String s) {
        try {
            return percentEncoder.encode(s);
        } catch (CharacterCodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
