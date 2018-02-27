package org.xbib.net.template.parse;

import org.xbib.net.matcher.CharMatcher;
import org.xbib.net.template.vars.specs.ExplodedVariable;
import org.xbib.net.template.vars.specs.PrefixVariable;
import org.xbib.net.template.vars.specs.SimpleVariable;
import org.xbib.net.template.vars.specs.VariableSpec;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Variable spec parser.
 */
public class VariableSpecParser {

    private static final CharMatcher DIGIT = CharMatcher.inRange('0', '9')
            .precomputed();

    private static final CharMatcher VARCHAR = DIGIT
            .or(CharMatcher.inRange('a', 'z'))
            .or(CharMatcher.inRange('A', 'Z'))
            .or(CharMatcher.is('_'))
            .or(CharMatcher.PERCENT)
            .precomputed();

    private static final CharMatcher DOT = CharMatcher.is('.');

    private static final CharMatcher COLON = CharMatcher.is(':');

    private static final CharMatcher STAR = CharMatcher.is('*');

    private VariableSpecParser() {
    }

    public static VariableSpec parse(CharBuffer buffer) {
        String name = parseFullName(buffer);
        if (!buffer.hasRemaining()) {
            return new SimpleVariable(name);
        }
        char c = buffer.charAt(0);
        if (STAR.matches(c)) {
            buffer.get();
            return new ExplodedVariable(name);
        }
        if (COLON.matches(c)) {
            buffer.get();
            return new PrefixVariable(name, getPrefixLength(buffer));
        }
        return new SimpleVariable(name);
    }

    private static String parseFullName(CharBuffer buffer) {
        List<String> components = new ArrayList<>();
        while (true) {
            components.add(readName(buffer));
            if (!buffer.hasRemaining()) {
                break;
            }
            if (!DOT.matches(buffer.charAt(0))) {
                break;
            }
            buffer.get();
        }
        return String.join(".", components);
    }

    private static String readName(CharBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        char c;
        while (buffer.hasRemaining()) {
            c = buffer.charAt(0);
            if (!VARCHAR.matches(c)) {
                break;
            }
            sb.append(buffer.get());
            if (CharMatcher.PERCENT.matches(c)) {
                parsePercentEncoded(buffer, sb);
            }
        }
        String ret = sb.toString();
        if (ret.isEmpty()) {
            throw new IllegalArgumentException("empty var name");
        }
        return ret;
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

    private static int getPrefixLength(CharBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        char c;
        while (buffer.hasRemaining()) {
            c = buffer.charAt(0);
            if (!DIGIT.matches(c)) {
                break;
            }
            sb.append(buffer.get());
        }
        String s = sb.toString();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("empty prefix");
        }
        int ret;
        try {
            ret = Integer.parseInt(s);
            if (ret > 10000) {
                throw new NumberFormatException();
            }
            return ret;
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException("prefix invalid / too large");
        }
    }
}
