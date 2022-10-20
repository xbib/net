package org.xbib.net;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import org.xbib.datastructures.common.Pair;
import org.xbib.net.scheme.Scheme;
import org.xbib.net.scheme.SchemeRegistry;

/**
 * A URL parser class.
 */
public class URLParser {

    private static final String EMPTY = "";

    private final URLBuilder builder;

    URLParser(Charset charset, CodingErrorAction codingErrorAction) {
        builder = new URLBuilder();
        builder.charset(charset, codingErrorAction);
    }

    public URL parse(String input)
            throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
        return parse(input, true);
    }

    public URL parse(String input, boolean resolve)
            throws URLSyntaxException, MalformedInputException, UnmappableCharacterException {
        if (URL.isNullOrEmpty(input)) {
            return URL.NULL_URL;
        }
        if (input.indexOf('\n') >= 0) {
            return URL.NULL_URL;
        }
        if (input.indexOf('\t') >= 0) {
            return URL.NULL_URL;
        }
        String remaining = parseScheme(builder, input);
        if (remaining != null) {
            remaining = remaining.replace('\\', URL.SEPARATOR_CHAR);
            builder.schemeSpecificPart(remaining);
            if (remaining.startsWith(URL.DOUBLE_SLASH)) {
                Scheme scheme = SchemeRegistry.getInstance().getScheme(builder.scheme);
                if (builder.scheme == null || scheme.getDefaultPort() == -1) {
                    builder.host(EMPTY);
                } else {
                    remaining = remaining.substring(2);
                    int i = remaining.indexOf(URL.SEPARATOR_CHAR);
                    int j = remaining.indexOf(URL.QUESTION_CHAR);
                    int pos = i >= 0 && j >= 0 ? Math.min(i, j) : i >= 0 ? i : j >= 0 ? j : -1;
                    String host = (pos >= 0 ? remaining.substring(0, pos) : remaining);
                    parseHostAndPort(builder, parseUserInfo(builder, host), resolve);
                    if (builder.host == null) {
                        return URL.NULL_URL;
                    }
                    remaining = pos >= 0 ? remaining.substring(pos) : EMPTY;
                }
            }
            if (!URL.isNullOrEmpty(remaining)) {
                try {
                    parsePathWithQueryAndFragment(builder, remaining);
                } catch (CharacterCodingException e) {
                    throw new URLSyntaxException(e);
                }
            }
        }
        return builder.build();
    }

    String parseScheme(URLBuilder builder, String input) {
        Pair<String, String> p = URL.indexOf(URL.COLON_CHAR, input);
        if (p.getValue() == null) {
            return input;
        }
        if (!URL.isNullOrEmpty(p.getKey())) {
            builder.scheme(p.getKey());
        }
        return p.getValue();
    }

    String parseUserInfo(URLBuilder builder, String input)
            throws MalformedInputException, UnmappableCharacterException {
        String remaining = input;
        int i = input.lastIndexOf(URL.AT_CHAR);
        if (i > 0) {
            remaining = input.substring(i + 1);
            String userInfo = input.substring(0, i);
            builder.userInfo(builder.percentDecoder.decode(userInfo));
        }
        return remaining;
    }

    void parseHostAndPort(URLBuilder builder, String rawHost, boolean resolve)
            throws URLSyntaxException {
        String host = rawHost;
        if (host.indexOf(URL.LEFT_BRACKET_CHAR) == 0) {
            int i = host.lastIndexOf(URL.RIGHT_BRACKET_CHAR);
            if (i >= 0) {
                builder.port(parsePort(host.substring(i + 1)));
                host = host.substring(1, i);
            }
        } else {
            int i = host.indexOf(URL.COLON_CHAR);
            if (i >= 0) {
                builder.port(parsePort(host.substring(i)));
                host = host.substring(0, i);
            }
        }
        if (resolve) {
            builder.resolveFromHost(host);
        } else {
            builder.host(host);
        }
    }

    Integer parsePort(String portStr) throws URLSyntaxException {
        if (portStr == null || portStr.isEmpty()) {
            return null;
        }
        int i = portStr.indexOf(URL.COLON_CHAR);
        if (i >= 0) {
            portStr = portStr.substring(i + 1);
            if (portStr.isEmpty()) {
                return -1;
            }
        }
        try {
            int port = Integer.parseInt(portStr);
            if (port > 0 && port < 65536) {
                return port;
            } else {
                throw new URLSyntaxException("invalid port");
            }
        } catch (NumberFormatException e) {
            throw new URLSyntaxException("no numeric port: " + portStr);
        }
    }

    void parsePathWithQueryAndFragment(URLBuilder builder, String inputStr)
            throws MalformedInputException, UnmappableCharacterException {
        String input = inputStr;
        if (input == null) {
            return;
        }
        int i = input.lastIndexOf(URL.NUMBER_SIGN_CHAR);
        if (i >= 0) {
            builder.fragment(builder.percentDecoder.decode(input.substring(i + 1)));
            input = input.substring(0, i);
        }
        i = input.indexOf(URL.QUESTION_CHAR);
        if (i >= 0) {
            parseQuery(builder, input.substring(i + 1));
            input = input.substring(0, i);
        }
        if (input.length() > 0 && input.charAt(0) == URL.SEPARATOR_CHAR) {
            builder.pathSegment(EMPTY);
        }
        String s = input;
        while (s != null) {
            Pair<String, String> pair = URL.indexOf(URL.SEPARATOR_CHAR, s);
            String elem = pair.getKey();
            if (!elem.isEmpty()) {
                if (elem.charAt(0) == URL.SEMICOLON_CHAR) {
                    builder.pathSegment(EMPTY);
                    String t = elem.substring(1);
                    while (t != null) {
                        Pair<String, String> pathWithMatrixElem = URL.indexOf(URL.SEMICOLON_CHAR, t);
                        String matrixElem = pathWithMatrixElem.getKey();
                        Pair<String, String> p = URL.indexOf(URL.EQUAL_CHAR, matrixElem);
                        builder.matrixParam(builder.percentDecoder.decode(p.getKey()),
                                builder.percentDecoder.decode(p.getValue()));
                        t = pathWithMatrixElem.getValue();
                    }
                } else {
                    String t = elem;
                    i = 0;
                    while (t != null) {
                        Pair<String, String> pathWithMatrixElem = URL.indexOf(URL.SEMICOLON_CHAR, t);
                        String segment = pathWithMatrixElem.getKey();
                        if (i == 0) {
                            builder.pathSegment(builder.percentDecoder.decode(segment));
                        } else {
                            Pair<String, String> p = URL.indexOf(URL.EQUAL_CHAR, segment);
                            builder.matrixParam(builder.percentDecoder.decode(p.getKey()),
                                    builder.percentDecoder.decode(p.getValue()));
                        }
                        t = pathWithMatrixElem.getValue();
                        i++;
                    }
                }
            }
            s = pair.getValue();
        }
        if (input.endsWith("/")) {
            builder.pathSegment(EMPTY);
        }
    }

    void parseQuery(URLBuilder builder, String query)
            throws MalformedInputException, UnmappableCharacterException {
        if (query == null) {
            return;
        }
        String s = query;
        while (s != null) {
            Pair<String, String> p = URL.indexOf(URL.AMPERSAND_CHAR, s);
            Pair<String, String> param = URL.indexOf(URL.EQUAL_CHAR, p.getKey());
            if (!URL.isNullOrEmpty(param.getKey())) {
                builder.queryParam(builder.percentDecoder.decode(param.getKey()),
                        builder.percentDecoder.decode(param.getValue()));
            }
            s = p.getValue();
        }
        if (builder.queryParams.isEmpty()) {
            builder.query(builder.percentDecoder.decode(query));
        } else {
            builder.query(query);
        }
    }
}
